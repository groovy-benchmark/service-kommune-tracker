import com.seitenbau.servicekommune.trackingserver.ServerConfig
import helpers.SkTrackerSpecification
import ratpack.func.Action
import ratpack.http.client.RequestSpec
import ratpack.test.http.TestHttpClient

class AuthorizationSpecification extends SkTrackerSpecification{

  def "Access without authentication fails"(){
    given:
    String processName = "doesNotMatter"
    TestHttpClient unauthorizedClient = TestHttpClient.testHttpClient(aut)

    when:
    unauthorizedClient.get("/api/v1.0/testAuth/$processName")

    then:
    unauthorizedClient.response.statusCode == 401
    unauthorizedClient.response.body.text.contains("Required header 'Authorization' is missing")
  }

  def "Access with an unknown users fails"(){
    given:
    String processName = "doesNotMatter"
    String nonExistingUsername = "iDoNotExist"
    String password = "doesNotMatter"
    TestHttpClient client = TestHttpClient.testHttpClient(aut, new Action<RequestSpec>() {
      @Override
      void execute(RequestSpec requestSpec) throws Exception {
        requestSpec.basicAuth(nonExistingUsername, password)
      }
    })

    when:
    client.get("/api/v1.0/testAuth/$processName")

    then:
    client.response.statusCode == 401
    client.response.body.text.contains("User not found")
  }

  def "Access with bad password fails"(){
    given:
    String processName = "doesNotMatter"
    String password = "doesNotMatchPasswordInDb"
    TestHttpClient client = TestHttpClient.testHttpClient(aut, new Action<RequestSpec>() {
      @Override
      void execute(RequestSpec requestSpec) throws Exception {
        requestSpec.basicAuth(ServerConfig.TESTUSER_NAME, password)
      }
    })

    when:
    client.get("/api/v1.0/testAuth/$processName")

    then:
    client.response.statusCode == 401
    client.response.body.text.contains("Wrong password")
  }

  def "Access for an unauthorized process fails"(){
    given:
    String processName = "aProcessIdBaseWeAreNotAllowedtoAccess"
    TestHttpClient client = TestHttpClient.testHttpClient(aut, new Action<RequestSpec>() {
      @Override
      void execute(RequestSpec requestSpec) throws Exception {
        requestSpec.basicAuth(ServerConfig.TESTUSER_NAME, ServerConfig.TESTUSER_PASSWORD)
      }
    })

    when:
    client.get("/api/v1.0/testAuth/$processName")

    then:
    client.response.statusCode == 403
    client.response.body.text.contains("is not allowed to access process")
  }

  def "Access for an authorized user succeeds"(){
    given:
    TestHttpClient client = TestHttpClient.testHttpClient(aut, new Action<RequestSpec>() {
      @Override
      void execute(RequestSpec requestSpec) throws Exception {
        requestSpec.basicAuth(ServerConfig.TESTUSER_NAME, ServerConfig.TESTUSER_PASSWORD)
      }
    })

    when:
    client.get("/api/v1.0/testAuth/${ServerConfig.TESTUSER_AUTHORIZED_PROCESS_ID}")

    then:
    client.response.statusCode == 200
    client.response.body.text.contains("success")
  }


}
