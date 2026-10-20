import com.seitenbau.servicekommune.trackingserver.ServerConfig
import groovy.json.JsonSlurper
import helpers.SkTrackerSpecification
import ratpack.func.Action
import ratpack.http.client.RequestSpec
import ratpack.test.http.TestHttpClient

class GetAllDetailsSpecification extends SkTrackerSpecification {
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

  def "Getting all details for a process"() {
    given:
    final int timestampBufferForTest = 5 // seconds
    String processId = "testprozess"

    when:
    get("api/v1.0/processes/$processId")
    List<Map> answer = new JsonSlurper().parseText(response.body.text) as List<Map>

    then:
    // the event was just created
    answer.get(0).get("timestamp") <= (new Date().getTime() / 1000) + timestampBufferForTest
    answer.get(0).get("timestamp") >= (new Date().getTime() / 1000) - timestampBufferForTest

    answer.get(0).get("eventId") == "testevent"

    answer.get(0).get("processInstanceId") == 123

    answer.get(0).get("userId") == null // Not specified
  }
}
