package com.seitenbau.servicekommune.trackingserver.handlers.users

import com.seitenbau.servicekommune.trackingserver.exceptions.HttpClientError
import com.seitenbau.servicekommune.trackingserver.handlers.AbstractTrackingServerHandler
import ratpack.groovy.handling.GroovyContext
import ratpack.jackson.Jackson

class GetUserHandler extends AbstractTrackingServerHandler {
  @Override
  protected void handle(GroovyContext ctx) {
    String username = ctx.allPathTokens."username"
    if (!username) throw new HttpClientError("Username must not be empty", 400)

    List users = GetUsersHandler.getUsersFromDb(username)

    if (users.isEmpty()) throw new HttpClientError("User with username '$username' does not exist", 404)

    assert users.size() == 1: "Programming error. There should never be more than 1 user with a username '$username'"

    ctx.render(Jackson.json(users.first()))
  }

}
