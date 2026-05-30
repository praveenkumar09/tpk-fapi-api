package org.tpkprav;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class KarateTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        // Run on a dedicated port so the Karate test never conflicts
        // with the main test suite's port 8081
        return Map.of("quarkus.http.test-port", "8083");
    }
}