import com.seitenbau.servicekommune.trackingserver.ServerConfig
import helpers.SkTrackerSpecification
import ratpack.func.Action
import ratpack.http.client.RequestSpec
import ratpack.test.http.TestHttpClient

class AddUserSpecification extends SkTrackerSpecification {

  @Delegate
  TestHttpClient client

  def setup() {
    client = testHttpClient(aut, new Action<RequestSpec>() {
      @Override
      void execute(RequestSpec requestSpec) throws Exception {
        requestSpec.basicAuth(ServerConfig.TESTADMIN_NAME, ServerConfig.TESTADMIN_PASSWORD)
      }
    })
  }

  def "Adding a new user"() {
    given:
    String username = "newuser"
    String password = "newuserpassword"
    String isAdmin = "false"

    when:
    params({ params ->
      params.put("username", username)
      params.put("passwordCleartext", password)
      params.put("isAdmin", isAdmin)
    })
    post("/api/v1.0/admin/users")

    then:
    response.statusCode == 201
    response.body.text == "User created."
  }

  def "Adding a new admin"() {
    given:
    String username = "newadmin"
    String password = "newuserpassword"
    String isAdmin = "true"

    when:
    params({ params ->
      params.put("username", username)
      params.put("passwordCleartext", password)
      params.put("isAdmin", isAdmin)
    })
    post("/api/v1.0/admin/users")

    then:
    response.statusCode == 201
    response.body.text == "User created."
  }

  def "Adding an existing user"() {
    given:
    String username = ServerConfig.TESTUSER_NAME
    String password = "newuserpassword"
    String isAdmin = "false"

    when:
    params({ params ->
      params.put("username", username)
      params.put("passwordCleartext", password)
      params.put("isAdmin", isAdmin)
    })
    post("/api/v1.0/admin/users")

    then:
    response.statusCode == 409
    response.body.text.contains("Username '$username' already exists.")
  }

  def "Adding an user with a too-long username"() {
    given:
    String username = "a-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very" +
            "very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-" +
            "very-very-very-very-very-very-very-very-very-very-very-very-very-long-string"
    String password = "newuserpassword"
    String isAdmin = "false"

    when:
    params({ params ->
      params.put("username", username)
      params.put("passwordCleartext", password)
      params.put("isAdmin", isAdmin)
    })
    post("/api/v1.0/admin/users")

    then:
    response.statusCode == 400
    response.body.text.contains("Username '$username' is longer than the allowed 191 characters.")
  }

  def "Adding an user without a password"() {
    given:
    String username = "newuser"
    String isAdmin = "false"

    when:
    params({ params ->
      params.put("username", username)
      params.put("isAdmin", isAdmin)
    })
    post("/api/v1.0/admin/users")

    then:
    response.statusCode == 400
    response.body.text.contains("Password must not be empty.")
  }

  def "Adding an user with an empty password"() {
    given:
    String username = "newuser"
    String password = ""
    String isAdmin = "false"

    when:
    params({ params ->
      params.put("username", username)
      params.put("passwordCleartext", password)
      params.put("isAdmin", isAdmin)
    })
    post("/api/v1.0/admin/users")

    then:
    response.statusCode == 400
    response.body.text.contains("Password must not be empty.")
  }

}
