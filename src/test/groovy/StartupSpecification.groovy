import helpers.SkTrackerSpecification
import ratpack.test.http.TestHttpClient

class StartupSpecification extends SkTrackerSpecification {

  @Delegate
  TestHttpClient client

  def setup() {
    client = testHttpClient(aut)
  }

  def "Server is starting"() {
    when:
    get("")

    then:
    response.statusCode == 200
    response.body.text.contains('This is the Serviceportal Tracking Server.')
  }
}
