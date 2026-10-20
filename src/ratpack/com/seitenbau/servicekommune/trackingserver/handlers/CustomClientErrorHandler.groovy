package com.seitenbau.servicekommune.trackingserver.handlers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ratpack.error.ClientErrorHandler
import ratpack.handling.Context

import static ratpack.jackson.Jackson.json

class CustomClientErrorHandler implements ClientErrorHandler {
  Logger logger = LoggerFactory.getLogger(this.class)

  @Override
  void error(Context ctx, int statusCode) throws Exception {
    switch (statusCode) {
      case 404:
        logger.warn("404 occurred on '${ctx.request.uri}'")
        ctx.response.status(404)
        ctx.render(json([
                "errorType"   : "Client error",
                "errorMessage": "Page not found."
        ]))
        break
      default:
        logger.error("Unhandled ClientError occurred.", ctx.request)
        throw new RuntimeException("Unhandled ClientError")
    }
  }
}


