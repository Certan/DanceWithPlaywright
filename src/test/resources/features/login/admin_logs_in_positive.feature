@ui @login

Feature: Validate admin login functionality - Happy Path

  Background:
    Given the user is on the ADMIN_LOGIN page

  Scenario: Admin logs in with valid credentials
    When the user successfully logs in as ADMIN
    Then the user gets redirected to the DASHBOARD page