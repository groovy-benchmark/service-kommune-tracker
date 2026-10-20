import com.seitenbau.servicekommune.trackingserver.ServerConfig
import com.seitenbau.servicekommune.trackingserver.handlers.*
import com.seitenbau.servicekommune.trackingserver.handlers.users.AddUserHandler
import com.seitenbau.servicekommune.trackingserver.handlers.users.EditUserHandler
import com.seitenbau.servicekommune.trackingserver.handlers.users.GetUserHandler
import com.seitenbau.servicekommune.trackingserver.handlers.users.GetUsersHandler
import org.flywaydb.core.Flyway
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ratpack.error.ClientErrorHandler
import ratpack.handling.Context
import ratpack.http.MutableHeaders

import java.lang.reflect.Field
import java.nio.file.Files

import static ratpack.groovy.Groovy.ratpack

Logger logger = LoggerFactory.getLogger(this.class)
logger.info("Initiating Ratpack main class...")

// Check required database config variables
List<String> requiredDbConfigValues = ["DB_URL", "DB_USERNAME", "DB_PASSWORD", "DB_DRIVER"]
requiredDbConfigValues.each {
  Field field = ServerConfig.declaredFields.find({ field -> field.name == it })
  if (field.get(null) == null) {
    // variable is not configured. Maybe we have config data in environment variables?

    String valueFromEnvVariable = System.getenv(it)
    if (valueFromEnvVariable == null) {
      throw new RuntimeException("Required environment variable '$it' is not set.")
    } else {
      field.set(null, valueFromEnvVariable)
    }
  }
}

// Setup database via Flyway
flyway = Flyway.configure().dataSource(ServerConfig.DB_URL, ServerConfig.DB_USERNAME, ServerConfig.DB_PASSWORD).load()

// Clean old values
if (ServerConfig.SET_UP_TEST_DATA) {
  flyway.clean()
}

// Setup database tables
flyway.migrate()

// Create default entries in database
if (ServerConfig.SET_UP_TEST_DATA) {
  ServerConfig.setupTestData()
}

ratpack {
  bindings {
    bind(ExceptionHandler) // custom exception handler
    bindInstance(ClientErrorHandler, new CustomClientErrorHandler())
  }

  handlers {
    all {
      // Set CORS headers for all requests
      MutableHeaders headers = response.headers
      headers.set("Access-Control-Allow-Origin", "*")
      headers.set("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE")
      headers.set("Access-Control-Allow-Headers", "Authorization")
      next()
    }

    get() { Context ctx ->
      ctx.response.contentType("text/html")
      render("This is the Serviceportal Tracking Server.<br><a href=\"api/v1.0\">API Documentation</a>")
    } // Start page

    prefix("api/v1.0") {
      get() { Context ctx ->
        ctx.response.contentType("text/html")
        ctx.render(new String(Files.readAllBytes(ctx.file("resources/api-documentation.html"))))
      }

      prefix("openapi.yaml") {
        get() { Context ctx ->
          ctx.response.contentType("text/yaml")
          ctx.render(new String(Files.readAllBytes(ctx.file("resources/openapi.yaml"))))
        }
      }

      prefix("processes") {
        prefix("") {
          all(new RequireAdminHandler())
          get(new AllProcessesHandler())
        }

        prefix(":processId") {
          get(new AllDetailsHandler()) // Getting all details of tracked events

          prefix("events/:eventId") {
            post(new TrackEventHandler()) // Adding a new tracked event

            prefix("sum") {
              get(new SumForProcessAndEventHandler())
              // Getting the sum of tracked events for a given eventId
            }
          }

          prefix("sums") {
            get(new SumsForProcessHandler())
            // Getting the sums of all tracked events for a given processId
          }

          prefix("flow") {
            prefix("json") {
              get(new ProcessFlowJsonHandler())
            }
            prefix("html") {
              get(new ProcessFlowHtmlHandler())
            }
          }
        }
      }

      prefix("testAuth/:processId") {
        get("", new TestAuthHandler())
        // Only works for correctly authenticated users. Useful for testing
      }

      prefix("admin") {
        all(new RequireAdminHandler())

        prefix("users") {
          path {
            byMethod() {
              get(new GetUsersHandler())
              post(new AddUserHandler())
            }
          }

          prefix(":username") {
            path {
              byMethod {
                get(new GetUserHandler())
                patch(new EditUserHandler())
              }
            }
          }
        }
      }

      prefix("static") {
        get("d3-sankey.js") { ctx ->
          ctx.response.contentType("text/javascript")
          ctx.render(new String(Files.readAllBytes(ctx.file("resources/d3-sankey-diagram.js"))))
        }
        get("sankey_example.png") { ctx ->
          ctx.response.contentType("image/png")
          ctx.response.send(Files.readAllBytes(ctx.file("resources/sankey_example.png")))
        }
      }
    }

    get("favicon.ico") { Context ctx ->
      ctx.response.contentType("image/x-icon")
      ctx.response.send(Files.readAllBytes(ctx.file("resources/favicon.ico")))
    } // favicon
  }
}

logger.info("Serviceportal Tracking Server Main Class configured. ✅")
