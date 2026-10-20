package com.seitenbau.servicekommune.trackingserver.handlers

import com.seitenbau.servicekommune.trackingserver.ServerConfig
import com.seitenbau.servicekommune.trackingserver.exceptions.HttpClientError
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ratpack.groovy.handling.GroovyContext

class RequireAdminHandler extends AbstractTrackingServerHandler {
  Logger logger = LoggerFactory.getLogger(this.class)

  @Override
  protected void handle(GroovyContext context) {
    def (String username, String password) = getUsernameAndPasswordFromHeaders(context)
    verifyUser(context, username, password)

    // Check if user is admin
    Sql sql = ServerConfig.getNewSqlConnection()
    try {
      String getPermissionsStatement = "SELECT `isAdmin` FROM users WHERE username = ?"
      List<GroovyRowResult> rows = sql.rows(getPermissionsStatement, [username])
      assert rows.size() == 1
      if (rows[0]."isAdmin" == false) {
        logger.warn("Authorization failed. User '$username' tried to access URL protected by RequireAdminHandler.")
        throw new HttpClientError("Authorization failed. User '$username' is not an admin.", 403)
      } else {
        // User is admin, so we delegate to the next handler.
        context.next()
      }
    } finally {
      sql.close()
    }

  }
}
