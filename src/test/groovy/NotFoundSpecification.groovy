import helpers.SkTrackerSpecification
import ratpack.test.http.TestHttpClient

class NotFoundSpecification extends SkTrackerSpecification {
  @Delegate
  TestHttpClient client

  def setup() {
    client = testHttpClient(aut)
  }

  def "Correct error appears on unavailable URLs"() {
    when:
    get("/this-pages-does-not-exist")

    then:
    response.statusCode == 404
    response.body.text == "{\"errorType\":\"Client error\",\"errorMessage\":\"Page not found.\"}"
  }
}
