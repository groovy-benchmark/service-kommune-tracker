package com.seitenbau.servicekommune.trackingserver.handlers

import ratpack.groovy.handling.GroovyContext

import java.nio.file.Files

class ProcessFlowHtmlHandler extends AbstractTrackingServerHandler {

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

    // Check authorization
    if (!requireAuthorizationForProcess(processId, ctx)) {
      return
    }

    // Setup result html
    ctx.response.contentType("text/html")
    String html = new String(Files.readAllBytes(ctx.file("resources/process-flow-base-page.html")))

    // Render placeholder "PLACEHOLDER_PROCESS_ID"
    html = html.replaceAll("PLACEHOLDER_PROCESS_ID", processId)

    // Render placeholder "PLACEHOLDER_TIME_PARAMS"
    String timeParams = ""
    if (timeFrom != null && timeUntil == null) {
      timeParams += "?timeFrom=$timeFrom"
    }
    if (timeFrom == null && timeUntil != null) {
      timeParams += "?timeUntil=$timeUntil"
    }
    if (timeFrom != null && timeUntil != null) {
      timeParams += "?timeFrom=$timeFrom&timeUntil=$timeUntil"
    }
    html = html.replaceAll("PLACEHOLDER_TIME_PARAMS", timeParams)

    // Send result to user
    ctx.render(html)
  }
}
