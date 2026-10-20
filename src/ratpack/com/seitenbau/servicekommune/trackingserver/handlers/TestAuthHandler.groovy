package com.seitenbau.servicekommune.trackingserver.handlers

import ratpack.groovy.handling.GroovyContext

import static ratpack.jackson.Jackson.json

class TestAuthHandler extends AbstractTrackingServerHandler {
  @Override
  protected void handle(GroovyContext ctx) {
    String processId = ctx.allPathTokens.processId

    // Check authorization
    if (!requireAuthorizationForProcess(processId, ctx)) {
      return
    }

    ctx.response.status(200)
    ctx.render(json("success"))
  }
}
