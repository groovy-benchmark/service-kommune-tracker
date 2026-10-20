import com.seitenbau.servicekommune.trackingserver.ServerConfig
import helpers.SkTrackerSpecification
import ratpack.func.Action
import ratpack.http.client.RequestSpec
import ratpack.test.http.TestHttpClient

class GetSumOfGivenEventSpecification extends SkTrackerSpecification {

  @Delegate
  TestHttpClient client

  def setup() {
    client = testHttpClient(aut, new Action<RequestSpec>() {
      @Override
      void execute(RequestSpec requestSpec) throws Exception {
        requestSpec.basicAuth(ServerConfig.TESTUSER_NAME, ServerConfig.TESTUSER_PASSWORD)
      }
    })
  }


  def "Get the sum of a given Event"() {
    given:
    String processId = "testprozess"
    String eventId = "testevent"

    when:
    get("api/v1.0/processes/$processId/events/$eventId/sum")
    int sum = Integer.parseInt(response.body.text)

    then:
    response.statusCode == 200
    sum > 0 // the test database should always contain entries for "testprozess" and "testevent"
  }

  def "Adding events increases the sum of events"() {
    given:
    String processId = "testprozess"
    String eventId = "testevent"
    int processInstanceId = 123456

    when:
    // Get current state
    get("api/v1.0/processes/$processId/events/$eventId/sum")
    int oldSum = Integer.parseInt(response.body.text)

    // Track new event
    params({ params ->
      params.put("processInstanceId", processInstanceId)
    })
    post("/api/v1.0/processes/$processId/events/$eventId")

    // Get new state
    get("api/v1.0/processes/$processId/events/$eventId/sum")
    int newSum = Integer.parseInt(response.body.text)

    then:
    newSum - oldSum == 1
  }

  def "Get the sum of a given Event with timeFrom"() {
    given:
    String processId = "testprozess"
    String eventId = "testevent"
    String timeFrom = "2145916800" // 2038-01-01 00:00:00+00:00 i.e. a day far in the future

    when:
    params({ params ->
      params.put("timeFrom", timeFrom)
    })
    get("api/v1.0/processes/$processId/events/$eventId/sum")

    then:
    int sum = Integer.parseInt(response.body.text)
    sum == 0
  }

  def "Get the sum with an invalid timestamp"() {
    given:
    String processId = "testprozess"
    String eventId = "testevent"
    String timeFrom = "a String that should fail to parse"

    when:
    params({ params ->
      params.put("timeFrom", timeFrom)
    })
    get("api/v1.0/processes/$processId/events/$eventId/sum")

    then:
    response.statusCode == 400
    response.body.text.contains("Parameter 'timeFrom' must be a valid integer")
  }

  def "Get the sum with a ridiculously large timestamp, larger than the timestamp supports it"() {
    given:
    String processId = "testprozess"
    String eventId = "testevent"
    String timeUntil = "999999999999999999999999999"

    when:
    params({ params ->
      params.put("timeUntil", timeUntil)
    })
    get("api/v1.0/processes/$processId/events/$eventId/sum")

    then:
    response.statusCode == 400
    String expected = "Parameter 'timeUntil' must be a valid integer smaller than $Integer.MAX_VALUE".toString()
    response.body.text.contains(expected)
  }

}
