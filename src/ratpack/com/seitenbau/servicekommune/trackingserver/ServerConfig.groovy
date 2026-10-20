package com.seitenbau.servicekommune.trackingserver

import com.seitenbau.servicekommune.trackingserver.handlers.users.AddUserHandler
import com.seitenbau.servicekommune.trackingserver.handlers.users.EditUserHandler
import groovy.sql.Sql

class ServerConfig {
  public static String DB_URL
  public static String DB_USERNAME
  public static String DB_PASSWORD
  public static String DB_DRIVER

  // Default entries for tests
  public static boolean SET_UP_TEST_DATA = false
  public static final String TESTUSER_NAME = "testuser"
  public static final String TESTUSER_PASSWORD = "A password only used for running tests"
  public static final String TESTUSER_AUTHORIZED_PROCESS_ID = "testprozess"
  public static final String TESTADMIN_NAME = "admin"
  public static final String TESTADMIN_PASSWORD = "A admin password for local tests"

  /**
   * Creates a new, configured sql connection. Do not forget to close the connection again with
   * sql.close() after you're done with it.
   *
   * @return a configured sql connection
   */
  static Sql getNewSqlConnection() {
    return Sql.newInstance(DB_URL, DB_USERNAME, DB_PASSWORD, DB_DRIVER)
  }

  static setupTestData() {
    Sql sql = getNewSqlConnection()
    try {
      // one already tracked event
      String insertOneTrackedEventStatement = "INSERT INTO trackedEvents (`processId`, `eventId`, `processInstanceId`) VALUES('$TESTUSER_AUTHORIZED_PROCESS_ID', 'testevent', 123);"
      sql.execute(insertOneTrackedEventStatement)

      // and two more for another event
      String insertOtherEventStatement = "INSERT INTO trackedEvents (`processId`, `eventId`, `processInstanceId`) VALUES('$TESTUSER_AUTHORIZED_PROCESS_ID', 'anotherTestevent', 123);"
      sql.execute(insertOtherEventStatement)
      sql.execute(insertOtherEventStatement)

      // test user with access to a specific process
      AddUserHandler.createUser(TESTUSER_NAME, TESTUSER_PASSWORD, false)
      EditUserHandler.addPermission(TESTUSER_NAME, TESTUSER_AUTHORIZED_PROCESS_ID)

      // test admin users
      AddUserHandler.createUser(TESTADMIN_NAME, TESTADMIN_PASSWORD, true)
    } finally {
      sql.close()
    }
  }
}

