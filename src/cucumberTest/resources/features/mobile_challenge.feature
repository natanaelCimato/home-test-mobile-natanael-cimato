Feature: Art Gallery mobile challenge

  Scenario: User authentication happy path
    Given I am on the login screen
    When I login with valid credentials
    Then I should see the art gallery catalog

  Scenario: Login input validation for missing credentials
    Given I am on the login screen
    When I submit the login form without credentials
    Then the login alert should say "Please complete both fields"

  Scenario Outline: Login input validation for incomplete credentials
    Given I am on the login screen
    When I submit login with email "<email>" and password "<password>"
    Then the login alert should say "Please complete both fields"

    Examples:
      | email             | password |
      | johndoe@email.com |          |
      |                   | 123      |

  Scenario: Login input validation for invalid credentials
    Given I am on the login screen
    When I login with invalid credentials
    Then the login alert should say "Invalid user or password"

  Scenario: Catalog exploration for a deep item
    Given I am authenticated in the catalog
    When I open the catalog item "Twilight Glow"
    Then I should see item details for "Twilight Glow"

  Scenario: User registration flow
    Given I am on the login screen
    When I complete the registration form
    Then I should reach the registration success screen
