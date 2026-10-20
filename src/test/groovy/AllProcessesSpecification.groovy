import com.seitenbau.servicekommune.trackingserver.ServerConfig
import groovy.json.JsonSlurper
import helpers.SkTrackerSpecification
import ratpack.func.Action
import ratpack.http.client.RequestSpec
import ratpack.test.http.TestHttpClient

class AllProcessesSpecification extends SkTrackerSpecification {
  @Delegate
  TestHttpClient client

  def setup() {
    client = testHttpClient(aut, new Action<RequestSpec>() {
      @Override
      void execute(RequestSpec requestSpec) throws Exception {
        requestSpec.basicAuth(ServerConfig.TESTADMIN_NAME, ServerConfig.TESTADMIN_PASSWORD)
      }
    })
  }

  def "Get all processes"() {
    when:
    get("api/v1.0/processes")
    List result = new JsonSlurper().parseText(response.body.text) as List

    then:
    response.statusCode == 200

    result.size() == 1
    result.first()."processId" == ServerConfig.TESTUSER_AUTHORIZED_PROCESS_ID
    result.first()."numberOfTrackedEvents" == 3
    result.first()."firstTrackedEventRelative" == "moments ago"
    result.first()."lastTrackedEventRelative" == "moments ago"
    result.first()."usersWithAccess" instanceof List
    result.first()."usersWithAccess".size() == 1
    result.first()."usersWithAccess".first() == ServerConfig.TESTUSER_NAME
  }

  def "Get all processes as non-admin"() {
    when:
    // Override client to use USER Access (and not admin)
    client = testHttpClient(aut, new Action<RequestSpec>() {
      @Override
      void execute(RequestSpec requestSpec) throws Exception {
        requestSpec.basicAuth(ServerConfig.TESTUSER_NAME, ServerConfig.TESTUSER_PASSWORD)
      }
    })
    get("api/v1.0/processes")
    def result = new JsonSlurper().parseText(response.body.text)

    then:
    response.statusCode == 403

    result."errorType" == "Client error"
    result."errorMessage".matches("Authorization failed\\. User .* is not an admin.")
  }
}
