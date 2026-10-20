import com.seitenbau.servicekommune.trackingserver.ServerConfig
import groovy.json.JsonSlurper
import helpers.SkTrackerSpecification
import ratpack.func.Action
import ratpack.http.client.RequestSpec
import ratpack.test.http.TestHttpClient

class GetProcessFlowAsJsonSpecification extends SkTrackerSpecification {

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


  def "Get the process flow of a given process"() {
    given:
    String processId = "testprozess"

    when:
    get("api/v1.0/processes/$processId/flow/json")
    def result = new JsonSlurper().parseText(response.body.text)

    then:
    response.statusCode == 200

    result."nodes" instanceof List
    result."links" instanceof List

    // All nodes available?
    (result."nodes" as List).find { it."id" == "testevent" } != null
    (result."nodes" as List).find { it."id" == "anotherTestevent" } != null

    // Correct amount of links for the first transition
    (result."links" as List).find {
      it."source" == "testevent" &&
              it."target" == "anotherTestevent"
    }."value" == 1

    // Correct amount of links for the second transition (the one that loops back)
    (result."links" as List).find {
      it."source" == "anotherTestevent" &&
              it."target" == "anotherTestevent"
    }."value" == 1
  }


}
