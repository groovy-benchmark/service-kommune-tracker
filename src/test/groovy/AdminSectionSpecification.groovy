import com.seitenbau.servicekommune.trackingserver.ServerConfig
import groovy.json.JsonSlurper
import helpers.SkTrackerSpecification
import ratpack.func.Action
import ratpack.http.client.RequestSpec
import ratpack.test.http.TestHttpClient

class AdminSectionSpecification extends SkTrackerSpecification {

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

  def "Get all users as non-admin"() {
    when:
    get("api/v1.0/admin/users")
    def result = new JsonSlurper().parseText(response.body.text)

    then:
    response.statusCode == 403

    result."errorType" == "Client error"
    result."errorMessage".matches("Authorization failed\\. User .* is not an admin.")
  }

}
