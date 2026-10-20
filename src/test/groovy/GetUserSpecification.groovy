import com.seitenbau.servicekommune.trackingserver.ServerConfig
import groovy.json.JsonSlurper
import helpers.SkTrackerSpecification
import ratpack.func.Action
import ratpack.http.client.RequestSpec
import ratpack.test.http.TestHttpClient

class GetUserSpecification extends SkTrackerSpecification {

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

  def "Get details about test user"() {
    when:
    get("api/v1.0/admin/users/${ServerConfig.TESTUSER_NAME}")
    def result = new JsonSlurper().parseText(response.body.text)

    then:
    response.statusCode == 200

    long now = System.currentTimeMillis()
    long tenSecondsAgo = System.currentTimeMillis() - (10 * 1000)

    result != null
    result.isAdmin == false
    result.creationDate <= now
    result.creationDate > tenSecondsAgo
    result.creationDateRelative == "moments ago"
    (result.permissions as List).contains(ServerConfig.TESTUSER_AUTHORIZED_PROCESS_ID)
  }

  def "Get details about not existing user"() {
    given:
    String invalidUsername = "I.do.not.exist"

    when:
    get("api/v1.0/admin/users/$invalidUsername")
    def result = new JsonSlurper().parseText(response.body.text)

    then:
    response.statusCode == 404

    result.errorMessage == "User with username '$invalidUsername' does not exist"
  }

}
