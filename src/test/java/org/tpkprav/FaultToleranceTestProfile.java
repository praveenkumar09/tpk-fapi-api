package org.tpkprav;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class FaultToleranceTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                // Raise circuit-breaker and retry thresholds on the db-connector client so
                // no state bleeds between test methods; fault behaviour is tested via mocks.
                "org.tpkprav.client.DbConnectorClient/store/CircuitBreaker/requestVolumeThreshold", "500",
                "org.tpkprav.client.DbConnectorClient/store/CircuitBreaker/failureRatio", "0.9",
                "org.tpkprav.client.DbConnectorClient/store/CircuitBreaker/delay", "500",
                "org.tpkprav.client.DbConnectorClient/store/CircuitBreaker/successThreshold", "1",
                "org.tpkprav.client.DbConnectorClient/store/Retry/maxRetries", "0",
                "org.tpkprav.client.DbConnectorClient/store/Retry/delay", "10",
                // Keep the AuthController circuit-breaker thresholds high too
                "org.tpkprav.controller.AuthController/token/CircuitBreaker/requestVolumeThreshold", "500",
                "org.tpkprav.controller.AuthController/token/CircuitBreaker/failureRatio", "0.9",
                "org.tpkprav.controller.AuthController/token/Retry/maxRetries", "1",
                "org.tpkprav.controller.AuthController/token/Retry/delay", "10"
        );
    }
}