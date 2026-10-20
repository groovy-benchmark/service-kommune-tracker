import com.seitenbau.servicekommune.trackingserver.ServerConfig
import groovy.json.JsonSlurper
import helpers.SkTrackerSpecification
import ratpack.func.Action
import ratpack.http.client.RequestSpec
import ratpack.test.http.TestHttpClient

class EditUsersSpecification extends SkTrackerSpecification {

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

  def "Update the password of a user"() {
    given:
    String newPassword = "a new password for local tests"

    when:
    params({ params ->
      params.put("newPassword", newPassword)
    })
    patch("api/v1.0/admin/users/${ServerConfig.TESTUSER_NAME}")
    def result = new JsonSlurper().parseText(response.body.text)

    then:
    response.statusCode == 200

    result != null
    result.status == "Success"
    result.changes instanceof List<String>
    (result.changes as List<String>).contains("Password was updated.")
  }

  def "Grant admin rights to user"() {
    when:
    params({ params ->
      params.put("isAdmin", true)
    })
    patch("api/v1.0/admin/users/${ServerConfig.TESTUSER_NAME}")
    def result = new JsonSlurper().parseText(response.body.text)

    then:
    response.statusCode == 200

    result != null
    result.status == "Success"
    result.changes instanceof List<String>
    (result.changes as List<String>) == ["Admin status was set to true."]
  }

  def "Remove admin rights to user"() {
    when:
    params({ params ->
      params.put("isAdmin", false)
    })
    patch("api/v1.0/admin/users/${ServerConfig.TESTADMIN_NAME}")
    def result = new JsonSlurper().parseText(response.body.text)

    then:
    response.statusCode == 200

    result != null
    result.status == "Success"
    result.changes instanceof List<String>
    (result.changes as List<String>) == ["Admin status was set to false."]
  }

  def "Add permissions to view a process to a user"() {
    given:
    String nameOfANewProcess = "teststadt-testprozessNummerZwei"

    when:
    params({ params ->
      params.put("addPermission", nameOfANewProcess)
    })
    patch("api/v1.0/admin/users/${ServerConfig.TESTUSER_NAME}")
    def result = new JsonSlurper().parseText(response.body.text)

    then:
    response.statusCode == 200

    result != null
    result.status == "Success"
    result.changes instanceof List<String>
    (result.changes as List<String>) == ["Added permission for process '$nameOfANewProcess'.".toString()]
  }

  def "Add permissions to view a process to a user that already has access to this process"() {
    given:
    String nameOfAnExistingProcess = ServerConfig.TESTUSER_AUTHORIZED_PROCESS_ID

    when:
    params({ params ->
      params.put("addPermission", nameOfAnExistingProcess)
    })
    patch("api/v1.0/admin/users/${ServerConfig.TESTUSER_NAME}")
    def result = new JsonSlurper().parseText(response.body.text)

    then:
    response.statusCode == 200

    result != null
    result.status == "Success"
    result.changes instanceof List<String>
    (result.changes as List<String>) == ["Permission for process '$nameOfAnExistingProcess' was already given.".toString()]
  }

  def "Remove permissions to view a process from a user"() {
    given:
    String nameOfAPermissionThisUserHasAccessTo = ServerConfig.TESTUSER_AUTHORIZED_PROCESS_ID

    when:
    params({ params ->
      params.put("removePermission", nameOfAPermissionThisUserHasAccessTo)
    })
    patch("api/v1.0/admin/users/${ServerConfig.TESTUSER_NAME}")
    def result = new JsonSlurper().parseText(response.body.text)

    then:
    response.statusCode == 200

    result != null
    result.status == "Success"
    result.changes instanceof List<String>
    (result.changes as List<String>) == ["Permission for process '$nameOfAPermissionThisUserHasAccessTo' was removed.".toString()]
  }

  def "Remove permissions to view a process from a user that does not have this permission"() {
    given:
    String nameOfAnNonExistingProcess = "thisProcessDoesNotExist"

    when:
    params({ params ->
      params.put("removePermission", nameOfAnNonExistingProcess)
    })
    patch("api/v1.0/admin/users/${ServerConfig.TESTUSER_NAME}")
    def result = new JsonSlurper().parseText(response.body.text)

    then:
    response.statusCode == 200

    result != null
    result.status == "Success"
    result.changes instanceof List<String>
    (result.changes as List<String>) == ["Permission for process '$nameOfAnNonExistingProcess' was not given in the first place and therefore doesn't need to be removed.".toString()]
  }

}
