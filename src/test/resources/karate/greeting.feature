Feature: Greeting API - GET /hello

  Background:
    * url baseUrl

  Scenario: GET /hello returns plain text greeting
    Given path '/hello'
    When method GET
    Then status 200
    And match response == 'Hello from Quarkus REST'