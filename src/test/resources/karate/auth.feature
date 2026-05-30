Feature: Auth Token API - POST /v1/auth/token

  Background:
    * url baseUrl

  # ──────────────────────────────────────────────────
  # POSITIVE SCENARIOS
  # ──────────────────────────────────────────────────

  Scenario: Valid NRIC and UUID return a signed Bearer token
    Given path '/v1/auth/token'
    And request { nric: 'S1234567D', uuid: '550e8400-e29b-41d4-a716-446655440000' }
    When method POST
    Then status 200
    And match response.status == 'SUCCESS'
    And match response.data.accessToken == '#notnull'
    And match response.data.tokenType == 'Bearer'
    And match response.data.expiresIn == 900
    And match response.requestId == '#notnull'
    And match response.timestamp == '#notnull'

  Scenario: Supplied X-Request-Id is echoed in response header and body
    Given path '/v1/auth/token'
    And header X-Request-Id = 'my-trace-abc-123'
    And request { nric: 'S1234567D', uuid: '550e8400-e29b-41d4-a716-446655440000' }
    When method POST
    Then status 200
    And match responseHeaders['X-Request-Id'][0] == 'my-trace-abc-123'
    And match response.requestId == 'my-trace-abc-123'

  Scenario: Request without X-Request-Id generates a UUID request ID
    Given path '/v1/auth/token'
    And request { nric: 'T9876543Z', uuid: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890' }
    When method POST
    Then status 200
    And match response.requestId == '#uuid'

  Scenario: Short NRIC (exactly 4 chars) is accepted
    Given path '/v1/auth/token'
    And request { nric: 'A12B', uuid: '11111111-2222-3333-4444-555555555555' }
    When method POST
    Then status 200
    And match response.data.tokenType == 'Bearer'

  # ──────────────────────────────────────────────────
  # NEGATIVE SCENARIOS
  # ──────────────────────────────────────────────────

  Scenario: Missing nric field returns 400 validation error
    Given path '/v1/auth/token'
    And request { uuid: '550e8400-e29b-41d4-a716-446655440000' }
    When method POST
    Then status 400
    And match response.status == 'ERROR'
    And match response.error.code == 'VALIDATION_001'
    And match response.error.message == 'Validation failed'
    And match response.error.details == '#[_ >= 1]'

  Scenario: Missing uuid field returns 400 validation error
    Given path '/v1/auth/token'
    And request { nric: 'S1234567D' }
    When method POST
    Then status 400
    And match response.status == 'ERROR'
    And match response.error.code == 'VALIDATION_001'

  Scenario: Blank nric returns 400 with details referencing nric field
    Given path '/v1/auth/token'
    And request { nric: '', uuid: '550e8400-e29b-41d4-a716-446655440000' }
    When method POST
    Then status 400
    And match response.status == 'ERROR'
    And match response.error.code == 'VALIDATION_001'
    And match response.error.details[0].field == 'nric'

  Scenario: Blank uuid returns 400 with details referencing uuid field
    Given path '/v1/auth/token'
    And request { nric: 'S1234567D', uuid: '' }
    When method POST
    Then status 400
    And match response.status == 'ERROR'
    And match response.error.details[0].field == 'uuid'

  Scenario: Both fields blank returns 400 with two validation errors
    Given path '/v1/auth/token'
    And request { nric: '   ', uuid: '   ' }
    When method POST
    Then status 400
    And match response.status == 'ERROR'
    And match response.error.details == '#[2]'

  Scenario: Empty JSON body returns 400 (both fields null)
    Given path '/v1/auth/token'
    And request {}
    When method POST
    Then status 400
    And match response.status == 'ERROR'
    And match response.error.code == 'VALIDATION_001'

  Scenario: GET on token endpoint returns 405 Method Not Allowed
    Given path '/v1/auth/token'
    When method GET
    Then status 405

  Scenario: PUT on token endpoint returns 405 Method Not Allowed
    Given path '/v1/auth/token'
    And request {}
    When method PUT
    Then status 405

  Scenario: DELETE on token endpoint returns 405 Method Not Allowed
    Given path '/v1/auth/token'
    When method DELETE
    Then status 405

  Scenario: Non-existent path returns 404
    Given path '/v1/auth/does-not-exist'
    And request { nric: 'S1234567D', uuid: '550e8400-e29b-41d4-a716-446655440000' }
    When method POST
    Then status 404