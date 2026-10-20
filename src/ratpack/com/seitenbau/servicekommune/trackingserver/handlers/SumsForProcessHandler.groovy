package com.seitenbau.servicekommune.trackingserver.handlers

import com.seitenbau.servicekommune.trackingserver.ServerConfig
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import ratpack.groovy.handling.GroovyContext
import static ratpack.jackson.Jackson.json

class SumsForProcessHandler extends AbstractTrackingServerHandler {
  @Override
  protected void handle(GroovyContext ctx) {
    // get path parameters
    String processId = ctx.allPathTokens.processId

    // get GET parameters
    Integer timeFrom
    Integer timeUntil
    try {
      timeFrom = extractIntegerFromQueryParams(ctx, "timeFrom")
      timeUntil = extractIntegerFromQueryParams(ctx, "timeUntil")
    } catch (NumberFormatException ignored) {
      return
    }
    // optional comma-separated list of events to restrict the sums to, e.g. "login,checkout"
    String events = ctx.request.queryParams.get("events")

    // Check authorization
    if (!requireAuthorizationForProcess(processId, ctx)) {
      return
    }

    // get data from database
    Sql sql = ServerConfig.getNewSqlConnection()
    try {
      String selectStatement = """SELECT `eventId`, COUNT(`eventId`) as count
                FROM trackedEvents
                WHERE `processId` = ?"""
      List filterValues = [processId]
      if (timeFrom != null) {
        selectStatement += " AND `timestamp` >= FROM_UNIXTIME(?)"
        filterValues.add(timeFrom.toString())
      }
      if (timeUntil != null) {
        selectStatement += " AND `timestamp` <= FROM_UNIXTIME(?)"
        filterValues.add(timeUntil.toString())
      }
      if (events) {
        StringBuilder eventList = new StringBuilder()
        events.split(",").each { String eventName ->
          if (eventList.length() > 0) {
            eventList.append(", ")
          }
          eventList.append("'").append(eventName.trim()).append("'")
        }
        selectStatement += " AND `eventId` IN (" + eventList.toString() + ") "
      }
      selectStatement += "GROUP BY `eventId`"
      List<GroovyRowResult> rows = sql.rows(selectStatement, filterValues) // SINK: PLANTED-GR-HR-3

      // prepare result for user
      Map<String, Integer> output = [:]
      rows.each { row ->
        output.put(row.get("eventId").toString(), Integer.parseInt(row.get("count").toString()))
      }

      // return result to user
      ctx.response.status(200)
      ctx.render(json(output))
    } finally {
      sql.close()
    }
  }
}
