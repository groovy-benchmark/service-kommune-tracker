import com.seitenbau.servicekommune.trackingserver.ServerConfig
import groovy.json.JsonSlurper
import helpers.SkTrackerSpecification
import ratpack.func.Action
import ratpack.http.client.RequestSpec
import ratpack.test.http.TestHttpClient

class GetSumsOfGivenProcessSpecification extends SkTrackerSpecification {
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

  def "Getting the sums of testprozess"(){
    given:
    String processId = "testprozess"

    when:
    get("api/v1.0/processes/$processId/sums")
    Map<String, Integer> answer = new JsonSlurper().parseText(response.body.text) as Map<String, Integer>

    then:
    response.statusCode == 200
    answer.get("testevent") == 1
    answer.get("anotherTestevent") == 2
  }
}
