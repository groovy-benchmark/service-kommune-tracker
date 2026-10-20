package com.seitenbau.servicekommune.trackingserver.handlers

import com.seitenbau.servicekommune.trackingserver.ServerConfig
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import ratpack.groovy.handling.GroovyContext

import static ratpack.jackson.Jackson.json

class SumForProcessAndEventHandler extends AbstractTrackingServerHandler {
  @Override
  protected void handle(GroovyContext ctx) {
    // get path parameters
    String processId = ctx.allPathTokens.processId
    String eventId = ctx.allPathTokens.eventId

    // get GET parameters
    String match = ctx.request.queryParams.get("match") // "exact" (default) or "prefix"
    Integer timeFrom
    Integer timeUntil
    try {
      timeFrom = extractIntegerFromQueryParams(ctx, "timeFrom")
      timeUntil = extractIntegerFromQueryParams(ctx, "timeUntil")
    } catch (NumberFormatException ignored) {
      return
    }

    // Check authorization
    if (!requireAuthorizationForProcess(processId, ctx)) {
      return
    }

    // events are named hierarchically (e.g. "form.step1", "form.step2"), so the sum can also be built for a group
    EventCounter counter = (match == "prefix") ? new PrefixEventCounter(eventId) : new ExactEventCounter(eventId)

    // get count from database
    Sql sql = ServerConfig.getNewSqlConnection()
    try {
      String selectStatement = """SELECT COUNT(*) as amountTrackedEvent
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
      GroovyRowResult row = counter.count(sql, selectStatement, filterValues)

      // return result to user
      ctx.response.status(200)
      ctx.render(json(row.get("amountTrackedEvent")))
    } finally {
      sql.close()
    }
  }

  private static abstract class EventCounter {
    protected final String eventId

    EventCounter(String eventId) {
      this.eventId = eventId
    }

    abstract GroovyRowResult count(Sql sql, String statement, List filterValues)
  }

  private static class ExactEventCounter extends EventCounter {
    ExactEventCounter(String eventId) {
      super(eventId)
    }

    @Override
    GroovyRowResult count(Sql sql, String statement, List filterValues) {
      return sql.firstRow(statement + " AND `eventId` = ?", filterValues + [eventId]) // SAFE_SINK: PLANTED-GR-HR-4-safe
    }
  }

  private static class PrefixEventCounter extends EventCounter {
    PrefixEventCounter(String eventId) {
      super(eventId)
    }

    @Override
    GroovyRowResult count(Sql sql, String statement, List filterValues) {
      return sql.firstRow("$statement AND `eventId` LIKE '$eventId%'".toString(), filterValues) // SINK: PLANTED-GR-HR-4
    }
  }
}
