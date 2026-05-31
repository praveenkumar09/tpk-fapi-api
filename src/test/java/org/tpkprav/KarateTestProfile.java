package org.tpkprav;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class KarateTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        // Port 0 lets the OS pick a free port — avoids conflicts with the main suite
        // and with stale processes from crashed prior runs
        return Map.of("quarkus.http.test-port", "8083");
    }
}