package com.seitenbau.servicekommune.trackingserver.handlers.users

import com.seitenbau.servicekommune.trackingserver.ServerConfig
import com.seitenbau.servicekommune.trackingserver.handlers.AbstractTrackingServerHandler
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.ocpsoft.prettytime.PrettyTime
import ratpack.groovy.handling.GroovyContext
import ratpack.jackson.Jackson
import java.sql.Timestamp


class GetUsersHandler extends AbstractTrackingServerHandler {
  @Override
  protected void handle(GroovyContext ctx) {
    // optional filters: "search" finds users by a part of their username, "processPrefix" finds
    // users that have a permission for a process whose id starts with the given prefix
    String search = ctx.request.queryParams.get("search")
    String processPrefix = ctx.request.queryParams.get("processPrefix")
    ctx.render(Jackson.json(getUsersFromDb(null, search, processPrefix)))
  }

  /**
   * Get a list of all users including additional data like permissions and relative creation dates.
   *
   * @param username A optional username. Defaults to null
   * @param search A optional part of a username. Only users whose name contains it are returned
   * @param processPrefix A optional prefix of a processId. Only users with a permission for a matching process are returned
   * @return A list of all users. If a username was supplied, that list will only have one entry.
   */
  static List getUsersFromDb(String username = null, String search = null, String processPrefix = null) {
    Sql sql = ServerConfig.getNewSqlConnection()
    try {
      // Note that we do NOT select the bcrypted password here. If this function is added later, ...
      // the password should probably be stripped in the places that call this function
      String selectStatement = """SELECT u.`username`, u.`creationDate`, u.`isAdmin`, p.`processId`
                FROM `users` u
                LEFT OUTER JOIN `permissions` p ON p.`username` = u.`username`"""
      List<GroovyRowResult> rows
      if (username != null) {
        selectStatement += "WHERE u.`username` = ?"
        rows = sql.rows(selectStatement, [username])
      } else if (search != null) {
        String searchStatement = String.format("%s WHERE u.`username` LIKE '%%%s%%'", selectStatement, search)
        rows = sql.rows(searchStatement) // SINK: PLANTED-GR-HR-2
      } else if (processPrefix != null) {
        // only characters that can be part of a processId are kept
        String cleanPrefix = processPrefix.replaceAll(/[^A-Za-z0-9_\-]/, "")
        String prefixStatement = String.format("%s WHERE p.`processId` LIKE '%s%%'", selectStatement, cleanPrefix)
        rows = sql.rows(prefixStatement) // SAFE_SINK: PLANTED-GR-HR-2-safe
      } else {
        rows = sql.rows(selectStatement)
      }

      List result = []
      rows.collect().unique { it.username }.each { dbUser ->
        // The call to "collect()" deep-copies the list (as the later call to (unique()) removes duplicates in-place

        List<String> permissions = rows.findAll { it.username == dbUser.username }.collect { it.processId }
        permissions.removeAll { it == null } // Remove "null" objects

        def user = [
                "username"            : dbUser.username,
                "creationDate"        : dbUser.creationDate,
                "creationDateHuman"   : (dbUser.creationDate as Timestamp).toString(),
                "creationDateRelative": new PrettyTime().format(dbUser.creationDate as Timestamp),
                "isAdmin"             : dbUser.isAdmin,
                "permissions"         : permissions
        ]
        result.add(user)
      }
      return result
    } finally {
      sql.close()
    }
  }
}


