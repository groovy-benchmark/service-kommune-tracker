package com.seitenbau.servicekommune.trackingserver.handlers.users

import com.seitenbau.servicekommune.trackingserver.ServerConfig
import com.seitenbau.servicekommune.trackingserver.exceptions.HttpClientError
import com.seitenbau.servicekommune.trackingserver.handlers.AbstractTrackingServerHandler
import groovy.sql.Sql
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ratpack.groovy.handling.GroovyContext

import java.sql.SQLIntegrityConstraintViolationException

class AddUserHandler extends AbstractTrackingServerHandler {

  Logger logger = LoggerFactory.getLogger(this.class)

  @Override
  protected void handle(GroovyContext context) {
    String username = context.request.queryParams.get("username")
    String passwordCleartext = context.request.queryParams.get("passwordCleartext")
    String isAdmin = context.request.queryParams.get("isAdmin") ?: false

    if (username == null || username.size() == 0) {
      throw new HttpClientError("Username must not be empty.", 400)
    }

    if (passwordCleartext == null || passwordCleartext.size() == 0) {
      throw new HttpClientError("Password must not be empty.", 400)
    }

    if (isAdmin == "true" || isAdmin == "false") {
      // everything ok
    } else {
      throw new HttpClientError("Unexpected value '$isAdmin' for 'isAdmin' parameter.", 400)
    }

    try {
      createUser(username, passwordCleartext, isAdmin.toBoolean())
      context.response.status(201)
      context.render("User created.")
      logger.info("New user '$username' created.")
    } catch (UsernameTooLongException ignored) {
      throw new HttpClientError("Username '$username' is longer than the allowed 191 characters.", 400)
    } catch (PasswordEmptyException ignored) {
      throw new HttpClientError("Password must not be empty.", 400)
    } catch (UsernameAlreadyExistsException ignored) {
      throw new HttpClientError("Username '$username' already exists.", 409)
    }
  }

  static void createUser(String username, String passwordCleartext, boolean isAdmin) {
    assert username != null
    assert username.size() != 0
    assert passwordCleartext != null
    assert passwordCleartext.size() != 0

    if (username.length() > 191) {
      throw new UsernameTooLongException()
    }

    if (passwordCleartext.length() == 0) {
      throw new PasswordEmptyException()
    }

    String passwordBcrypted = BCrypt.hashpw(passwordCleartext, BCrypt.gensalt())

    // Store result in database
    Sql sql = ServerConfig.getNewSqlConnection()
    try {
      sql.executeInsert(
              "INSERT INTO users (`username`, `bcryptPassword`, `isAdmin`) VALUES (?, ?, ?)",
              [username, passwordBcrypted, isAdmin])
      sql.commit()
    } catch (SQLIntegrityConstraintViolationException ignored) {
      throw new UsernameAlreadyExistsException()
    } finally {
      sql.close()
    }
  }

  static class UsernameTooLongException extends Exception {}

  static class PasswordEmptyException extends Exception {}

  static class UsernameAlreadyExistsException extends Exception {}
}
