package com.seitenbau.servicekommune.trackingserver.handlers

import com.seitenbau.servicekommune.trackingserver.ServerConfig
import com.seitenbau.servicekommune.trackingserver.exceptions.HttpClientError
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.mindrot.jbcrypt.BCrypt
import ratpack.groovy.handling.GroovyHandler
import ratpack.handling.Context

import static ratpack.jackson.Jackson.json

abstract class AbstractTrackingServerHandler extends GroovyHandler {

  /**
   * Checks if the user authenticated in the request headers is allowed to access the process.
   *
   * Writes a error status and message into the response if something failed. Do NOT forget you will need to terminate
   * early (i.e. "return") if this method returns false.
   *
   * @param processId the processId to check permissions against
   * @param ctx The HTTP Context with request and response
   * @return true if authentication and authorization are okay!
   */
  static boolean requireAuthorizationForProcess(String processId, Context ctx) {
    def (String username, String password) = getUsernameAndPasswordFromHeaders(ctx)
    verifyUser(ctx, username, password)

    // Check permissions
    Sql sql = ServerConfig.getNewSqlConnection()
    try {
      String getPermissionsStatement = "SELECT 1 FROM permissions WHERE `username` = ? AND `processId` = ?"
      int resultSize = sql.rows(getPermissionsStatement, [username, processId]).size()
      if (resultSize == 0) {
        // No results --> No fitting permission!
        ctx.response.status(403)
        ctx.render(json(["errorMsg": "Authorization failed. User '$username' is not allowed to access process '$processId'.".toString()]))
        return false
      }
      return true
    } finally {
      sql.close()
    }
  }

  static Tuple<String> getUsernameAndPasswordFromHeaders(Context ctx) {
    // Check if header was supplied
    String header = ctx.request.headers.get("Authorization")
    if (header == null) {
      throw new HttpClientError("Required header 'Authorization' is missing", 401)
    }

    // Get username and password from header
    try {
      String encodedPart = header.split(" ")[1]
      String[] decoded = new String(encodedPart.decodeBase64()).split(":")
      String username = decoded[0]
      String password = decoded[1] // watch out! cleartext password

      return new Tuple(username, password)
    } catch (Exception ignored) {
      throw new HttpClientError("Authorization header not in correct format. (Basic only)", 400)
    }
  }

  /**
   * Verifies the provided username and password are valid. Throws 401 exceptions if not.
   *
   * @param ctx
   * @param username
   * @param password
   */
  static void verifyUser(Context ctx, String username, String password) {
    // Check if the password matches the user
    Sql sql = ServerConfig.getNewSqlConnection()
    try {
      String getPasswordStatement = "SELECT `bcryptPassword` FROM users WHERE username = ?"
      GroovyRowResult result = sql.firstRow(getPasswordStatement, [username])
      if (result == null) {
        throw new HttpClientError("Authentication failed. User not found.", 401)
      }
      String storedPassword = new String(result.get("bcryptPassword") as byte[])
      if (BCrypt.checkpw(password, storedPassword)) {
        // PW okay!
      } else {
        throw new HttpClientError("Authentication failed. Wrong password.", 401)
      }
    } finally {
      sql.close()
    }
  }

  /**
   * Extracts a optional query parameter from the context.
   * Sets a appropriate message in the context if the parameter is invalid (i.e. not an integer)
   *
   * @param ctx context object to extract from and write errors to
   * @param paramName parameter name to extract
   * @return the extracted integer, or null if parameter is missing
   * @throws NumberFormatException when parameter is invalid. I.e. you should return from your handler
   *                               when this is thrown as appropriate error messages have already been set
   */
  static Integer extractIntegerFromQueryParams(Context ctx, String paramName) throws NumberFormatException {
    Integer param
    try {
      param = ctx.request.queryParams.get(paramName) as Integer
      return param
    } catch (NumberFormatException e) {
      ctx.response.status(400)
      ctx.render(json(["errorMsg": "Parameter '$paramName' must be a valid integer smaller than ${Integer.MAX_VALUE}".toString()]))
      throw e
    }
  }
}
