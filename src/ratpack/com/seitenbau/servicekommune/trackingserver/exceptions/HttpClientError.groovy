package com.seitenbau.servicekommune.trackingserver.exceptions

/**
 * Should be thrown when a situation occurs that was caused by a client error. See
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Status#client_error_responses
 *
 * This exception is supposed to be caught by the system and the description should be shown to the
 * client.
 */
class HttpClientError extends Exception {
  String message
  int responseStatusCode

  /**
   * @param message The message to show to clients
   * @param responseStatusCode The HTTP status code to use (403 = Forbidden, 404 = Not found, ...)
   */
  HttpClientError(String message, int responseStatusCode) {
    super(message)
    this.message = message
    this.responseStatusCode = responseStatusCode
  }
}
