package com.seitenbau.servicekommune.trackingserver.handlers

import com.seitenbau.servicekommune.trackingserver.exceptions.HttpClientError
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ratpack.error.ServerErrorHandler
import ratpack.handling.Context

import static ratpack.jackson.Jackson.json

class ExceptionHandler implements ServerErrorHandler {
  Logger logger = LoggerFactory.getLogger(this.class)

  @Override
  void error(Context context, Throwable throwable) throws Exception {
    switch (throwable.class) {
      case HttpClientError:
        // Using a custom "HttpClientError" Exception is not suggested Ratpack way to handle user
        // errors (that would be context.clientError()), but it allows us to attach a custom message
        // directly in the handler. So we do just that.
        HttpClientError badRequestException = (HttpClientError) throwable
        context.response.status(badRequestException.responseStatusCode)
        if (badRequestException.responseStatusCode == 401) {
          context.response.headers.add("WWW-Authenticate", "Basic realm=\"Authentication required.\", charset=\"UTF-8\"")
        }
        context.render(json([
                "errorType"   : "Client error",
                "errorMessage": badRequestException.message
        ]))
        break
      default:
        // Unexpected error.
        logger.error("Unexpected error occurred.", throwable)
        context.response.status(500)
        context.render(json([
                "errorType"   : "Server error",
                "errorClass"  : throwable.class,
                "errorMessage": throwable.message
        ]))
        break
    }
  }
}
