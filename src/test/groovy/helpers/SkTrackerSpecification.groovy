package helpers

import com.seitenbau.servicekommune.trackingserver.ServerConfig
import groovy.sql.Sql
import org.mindrot.jbcrypt.BCrypt
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.test.ServerBackedApplicationUnderTest
import spock.lang.Specification

class SkTrackerSpecification extends Specification {

  ServerBackedApplicationUnderTest aut

  def setup() {
    ServerConfig.DB_URL = "jdbc:h2:mem:skTracker;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DATABASE_TO_LOWER=FALSE;;DB_CLOSE_DELAY=-1"
    ServerConfig.DB_USERNAME = "sa"
    ServerConfig.DB_PASSWORD = ""
    ServerConfig.DB_DRIVER = "org.h2.Driver"
    ServerConfig.SET_UP_TEST_DATA = true

    aut = new GroovyRatpackMainApplicationUnderTest()
  }
}
