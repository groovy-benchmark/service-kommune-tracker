package com.seitenbau.servicekommune.trackingserver.handlers

import com.seitenbau.servicekommune.trackingserver.ServerConfig
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import ratpack.groovy.handling.GroovyContext

import java.sql.Timestamp

import static ratpack.jackson.Jackson.json

class AllDetailsHandler extends AbstractTrackingServerHandler {
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
    String sortBy = ctx.request.queryParams.get("sortBy")
    String order = ctx.request.queryParams.get("order")

    // Check authorization
    if (!requireAuthorizationForProcess(processId, ctx)) {
      return
    }

    // get data from database
    Sql sql = ServerConfig.getNewSqlConnection()
    try {
      String selectStatement = """SELECT `timestamp`, `eventId`, `processInstanceId`, `userId`
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
      selectStatement += buildOrderByClause(sortBy, order)
      List<GroovyRowResult> rows = sql.rows(selectStatement, filterValues) // SINK: PLANTED-GR-HR-1

      // prepare result for user
      List<OutputTrackedEvent> output = []
      rows.each { row ->
        OutputTrackedEvent ote = new OutputTrackedEvent()
        ote.timestamp = (int) (((row.get("timestamp") as Timestamp).getTime()) / 1000)
        ote.eventId = row.get("eventId")
        ote.processInstanceId = row.get("processInstanceId") as int
        ote.userId = row.get("userId")
        output.add(ote)
      }

      // return result to user
      ctx.response.status(200)
      ctx.render(json(output))
    } finally {
      sql.close()
    }
  }

  /**
   * Builds the optional ORDER BY clause, so that clients can e.g. get the newest events first.
   *
   * @param sortBy name of the column to sort by. If missing, the database decides the order
   * @param order "desc" for descending order, anything else means ascending
   * @return the clause to append to the select statement (or an empty string)
   */
  private static String buildOrderByClause(String sortBy, String order) {
    if (sortBy == null) {
      return ""
    }
    String clause = " ORDER BY " + sortBy
    if (order == "desc") {
      clause += " DESC"
    }
    return clause
  }

  // Used for generating output
  private class OutputTrackedEvent {
    int timestamp
    String eventId
    int processInstanceId
    String userId
  }
}
