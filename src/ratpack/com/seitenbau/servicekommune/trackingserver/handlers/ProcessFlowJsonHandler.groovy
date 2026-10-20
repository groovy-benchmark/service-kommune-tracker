package com.seitenbau.servicekommune.trackingserver.handlers

import com.seitenbau.servicekommune.trackingserver.ServerConfig
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ratpack.groovy.handling.GroovyContext

import static ratpack.jackson.Jackson.json

class ProcessFlowJsonHandler extends AbstractTrackingServerHandler {
  private final String NO_FURTHER_EVENT = "SPECIAL_IDENTIFIER_FOR_NO_FURTHER_EVENTS_IN_THIS_FLOW"

  Logger logger = LoggerFactory.getLogger(this.class)

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
    // optional comma-separated list of events the flow diagram is restricted to, e.g. "login,checkout"
    String events = ctx.request.queryParams.get("events")

    // Check authorization
    if (!requireAuthorizationForProcess(processId, ctx)) {
      return
    }

    JsonSankeyData jsonSankeyData = generateJsonSankeyData(processId, timeFrom, timeUntil, events)

    // return result to user
    ctx.response.status(200)
    ctx.render(json(jsonSankeyData))
  }

  /**
   * Generates a structure that can be fed into a d3-sankey-diagram
   * (see https://ricklupton.github.io/d3-sankey-diagram/)
   *
   * @param processId The processId for which to generate the diagram
   * @param timeFrom A UNIX timestamp marking the point in time when events are included in the result (inclusive).
   *     If missing, all events are included (as long as they are not filtered by another parameter)
   * @param timeUntil A UNIX timestamp marking the point in time when events are no longer included in the result.
   *    If missing, all events are included (as long as they are not filtered by another parameter)
   *
   * @param events A comma-separated list of eventIds. If missing, all events are included
   *
   * @return
   */
  private JsonSankeyData generateJsonSankeyData(String processId, Integer timeFrom, Integer timeUntil, String events) {
    // benchmark this call as the execution time might grow for large processes
    long startTime = System.currentTimeMillis()

    // get data from database
    Sql sql = ServerConfig.getNewSqlConnection()
    try {
      String selectStatement = """SELECT `timestamp`, `eventId`, `processInstanceId`
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
        List<String> eventNames = events.split(",").collect { it.trim() }
        selectStatement += " AND `eventId` IN (" + eventNames.collect { "?" }.join(", ") + ") "
        filterValues.addAll(eventNames)
      }
      selectStatement += "ORDER BY `processInstanceId` ASC, `timestamp` ASC"
      List<GroovyRowResult> rows = sql.rows(selectStatement, filterValues) // SAFE_SINK: PLANTED-GR-HR-3-safe

      // extract flows and events
      Map<String, List<String>> flows = [:]
      Set<String> uniqueEvents = []
      rows.each { row ->
        String processInstanceId = row.get("processInstanceId") as String
        String eventId = row.get("eventId") as String

        assert eventId != NO_FURTHER_EVENT: "A super unlikely case appeared where a event was called '$NO_FURTHER_EVENT', or the developer of this function made a false assumption."

        // extract flow
        List<String> flow = flows.get(processInstanceId)
        if (flow == null) {
          // Initialize flow, if hasn't been initialized yet
          flow = []
          flows.put(processInstanceId, flow)
        }
        flow.add(eventId)

        // extract event. This is a set, therefore no duplicate entries
        uniqueEvents.add(eventId)
      }

      // count number of following events, for each event
      Map<String, Map<String, Integer>> eventsAndFollowingEvents = [:]
      uniqueEvents.each { eventName ->
        eventsAndFollowingEvents.put(eventName, [:])
      }
      flows.values().each { flow ->
        flow.eachWithIndex { currentEventId, index ->
          String followingEvent
          if (index == flow.size() - 1) {
            // this is the last element
            followingEvent = NO_FURTHER_EVENT
          } else {
            followingEvent = flow.get(index + 1)
          }

          int currentCounter = eventsAndFollowingEvents.get(currentEventId).get(followingEvent) ?: 0
          eventsAndFollowingEvents.get(currentEventId).put(followingEvent, currentCounter + 1)
        }
      }

      // Generate JSON Sankey data
      JsonSankeyData jsonSankeyData = new JsonSankeyData()
      uniqueEvents.each { eventId ->
        jsonSankeyData.nodes.add(new Node(eventId))
      }
      eventsAndFollowingEvents.each { precedingEventId, followingEvents ->
        followingEvents.each { followingEventId, count ->
          if (followingEventId == NO_FURTHER_EVENT) {
            return // skip ending events
          }

          // Generate random color, seeded by the links name.
          Random rng = new Random((precedingEventId + followingEventId).hashCode().toLong())
          // We want values in a certain range.
          int base = 150
          int range = 50
          String color = "rgb(${base + rng.nextInt(range)}, ${base + rng.nextInt(range)}, ${base + rng.nextInt(range)})"

          Link link = new Link(precedingEventId, followingEventId, count, color)
          jsonSankeyData.links.add(link)
        }
      }

      long endTime = System.currentTimeMillis()
      logger.info("Process flow analysis for process '$processId' took ${endTime - startTime} ms.")

      return jsonSankeyData

    } finally {
      sql.close()
    }
  }

  class JsonSankeyData {
    List<Node> nodes = []
    List<Link> links = []
  }

  class Link {
    String source
    String target
    int value
    String color

    Link(String source, String target, int value, String color) {
      this.source = source
      this.target = target
      this.value = value
      this.color = color
    }
  }

  class Node {
    String id

    Node(String id) {
      this.id = id
    }
  }

}
