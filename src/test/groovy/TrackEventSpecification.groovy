import helpers.SkTrackerSpecification
import ratpack.test.http.TestHttpClient

class TrackEventSpecification extends SkTrackerSpecification {

  @Delegate
  TestHttpClient client

  def setup() {
    client = testHttpClient(aut)
  }

  def "Adding a tracking event"() {
    given:
    String processId = "testkommune-testprozess"
    String eventId = "testevent"
    int processInstanceId = 123456

    when:
    params({ params ->
      params.put("processInstanceId", processInstanceId)
    })
    post("/api/v1.0/processes/$processId/events/$eventId")

    then:
    response.statusCode == 201
    response.body.text.empty
  }

  def "Adding a tracking event with a user ID"() {
    given:
    String processId = "testkommune-testprozess"
    String eventId = "testevent"
    int processInstanceId = 123456
    String userId = "userId:123456"

    when:
    params({ params ->
      params.put("processInstanceId", processInstanceId)
      params.put("userId", userId)
    })
    post("/api/v1.0/processes/$processId/events/$eventId")

    then:
    response.statusCode == 201
    response.body.text.empty
  }

  def "Missing process instance ID"() {
    given:
    String processId = "testkommune-testprozess"
    String eventId = "testevent"

    when:
    post("/api/v1.0/processes/$processId/events/$eventId")

    then:
    response.statusCode == 400
    response.body.text.contains("'processInstanceId' is required")
  }

  def "Process instance ID is not a integer"() {
    given:
    String processId = "testkommune-testprozess"
    String eventId = "testevent"
    String processInstanceId = "Some string"

    when:
    params({ params ->
      params.put("processInstanceId", processInstanceId)
    })
    post("/api/v1.0/processes/$processId/events/$eventId")

    then:
    response.statusCode == 400
    response.body.text.contains("'processInstanceId' must be an integer")
  }

  def "event ID is too long"() {
    given:
    String processId = "testkommune-testprozess"
    String eventId = "a-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very" +
            "very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-very-" +
            "very-very-very-very-very-very-very-very-very-very-very-very-very-long-string"
    int processInstanceId = 123456

    when:
    params({ params ->
      params.put("processInstanceId", processInstanceId)
    })
    post("/api/v1.0/processes/$processId/events/$eventId")

    then:
    response.statusCode == 400
    response.body.text.contains("'eventId' must be shorter than 190 characters\"")
  }


}
