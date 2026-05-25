@ui @login

Feature: Validate admin login functionality - Negative Path

  Background:
    Given the user is on the ADMIN_LOGIN page

  @runUi
  Scenario Outline: Admin logs in with invalid credentials
    When the ADMIN logs in with username: <username> and password: <password>
    And the ADMIN submits the login form
    Then the ADMIN login attempt should fail with error message 'Invalid credentials'
    Examples:
      | username                                                         | password      | description             |
      | Admin                                                            | wrongpassword | invalid password        |
      | Admin                                                            | Admin123      | case-sensitive password |
      | WrongUser                                                        | admin123      | invalid username        |
      | VeryLongUsernameVeryLongUsernameVeryLongUsernameVeryLongUsername | wrongpassword | long username           |