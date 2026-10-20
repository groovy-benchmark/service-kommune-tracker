package com.seitenbau.servicekommune.trackingserver.handlers

import com.seitenbau.servicekommune.trackingserver.ServerConfig
import com.seitenbau.servicekommune.trackingserver.exceptions.HttpClientError
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.ocpsoft.prettytime.PrettyTime
import ratpack.groovy.handling.GroovyContext

import java.sql.Timestamp

import static ratpack.jackson.Jackson.json

class AllProcessesHandler extends AbstractTrackingServerHandler {
  @Override
  protected void handle(GroovyContext ctx) {
    List<ReturnDto> result = []
    String sortBy = ctx.request.queryParams.get("sortBy")
    if (sortBy != null && !(sortBy in ["processId", "numberOfTrackedEvents", "firstTrackedEvent", "lastTrackedEvent"])) {
      throw new HttpClientError("Parameter 'sortBy' has invalid value '$sortBy'", 400)
    }

    // get data from database
    Sql sql = ServerConfig.getNewSqlConnection()
    try {
      String selectStatement = """\
        SELECT e.`processId`, COUNT(*) as numberOfTrackedEvents, MAX(e.`timestamp`) as lastTrackedEvent, MIN(e.`timestamp`) as firstTrackedEvent 
          FROM `trackedEvents` as e 
          GROUP BY e.processId"""
      if (sortBy != null) {
        selectStatement += " ORDER BY " + sortBy
      }
      List<GroovyRowResult> rowsOfEvents = sql.rows(selectStatement) // SAFE_SINK: PLANTED-GR-HR-1-safe
      rowsOfEvents.each { dbEvent ->
        ReturnDto currentDto = new ReturnDto()

        currentDto.processId = dbEvent."processId"
        currentDto.numberOfTrackedEvents = dbEvent."numberOfTrackedEvents"
        currentDto.firstTrackedEventRelative = new PrettyTime().format(dbEvent."firstTrackedEvent" as Timestamp)
        currentDto.lastTrackedEventRelative = new PrettyTime().format(dbEvent."lastTrackedEvent" as Timestamp)

        // Doing sql queries in a loop is probably not the most efficient way, but neither is
        // optimizing it prematurely and spending developer time.
        String selectPermissionsStatement = """\
         SELECT `username` 
           FROM `permissions` p
           WHERE `processId` = ?;"""
        List<GroovyRowResult> rowsOfPermissions = sql.rows(selectPermissionsStatement, [currentDto.processId])
        currentDto.usersWithAccess = rowsOfPermissions.collect { it."username" }

        result.add(currentDto)
      }

      // Sort by name, unless the client asked for a different order
      if (sortBy == null) {
        result = result.sort { it.processId }
      }

      // Render results
      ctx.render(json(result))
      ctx.response.status(200)
    } finally {
      sql.close()
    }

  }

  class ReturnDto {
    String processId
    List<String> usersWithAccess
    int numberOfTrackedEvents
    String firstTrackedEventRelative
    String lastTrackedEventRelative
  }
}
