package com.seitenbau.servicekommune.trackingserver.handlers

import com.seitenbau.servicekommune.trackingserver.ServerConfig
import com.seitenbau.servicekommune.trackingserver.exceptions.HttpClientError
import groovy.sql.Sql
import ratpack.groovy.handling.GroovyContext

import static ratpack.jackson.Jackson.json

class TrackEventHandler extends AbstractTrackingServerHandler {
  @Override
  protected void handle(GroovyContext ctx) {

    // get path parameters
    String processId = ctx.allPathTokens.processId
    String eventId = ctx.allPathTokens.eventId

    // get POST parameters
    String processInstanceId = ctx.request.queryParams.get("processInstanceId")
    String userId = ctx.request.queryParams.get("userId")

    // verify parameters
    if (processInstanceId == null) {
      throw new HttpClientError("Parameter 'processInstanceId' is required", 400)
    }
    if (!(processInstanceId.isInteger())) {
      throw new HttpClientError("Parameter 'processInstanceId' must be an integer", 400)
    }
    if (processId.length() > 190) {
      throw new HttpClientError("Parameter 'processId' must be shorter than 190 characters", 400)
    }
    if (eventId.length() > 190) {
      throw new HttpClientError("Parameter 'eventId' must be shorter than 190 characters", 400)
    }
    if (userId != null) {
      if (userId.length() > 190) {
        throw new HttpClientError("Parameter 'userId' must be shorter than 190 characters", 400)
      }
    }

    // Store result in database
    Sql sql = ServerConfig.getNewSqlConnection()
    try {
      sql.execute("INSERT INTO trackedEvents (`processId`, `eventId`, `processInstanceId`, `userId`) VALUES (?, ?, ?, ?)",
              [processId, eventId, processInstanceId, userId])
      sql.commit()

      ctx.response.status(201)
      ctx.response.send() // No further content
    } finally {
      sql.close()
    }
  }
}
