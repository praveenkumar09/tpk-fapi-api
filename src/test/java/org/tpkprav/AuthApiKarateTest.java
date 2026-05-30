package org.tpkprav;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Runs the Karate API test suite against the live Quarkus server.
 *
 * Uses KarateTestProfile (port 8083) so this test starts its own
 * Quarkus instance and never conflicts with the main suite on port 8081.
 */
@QuarkusTest
@TestProfile(KarateTestProfile.class)
class AuthApiKarateTest {

    @Test
    void runAuthFeatures() {
        // Propagate the actual test port so karate-config.js builds the right base URL
        System.setProperty("quarkus.http.test-port",
                String.valueOf(io.restassured.RestAssured.port));

        // Use file paths + explicit configDir to avoid classgraph/Quarkus classloader issues
        Results results = Runner
                .path("src/test/resources/karate/auth.feature")
                .configDir("src/test/resources")
                .outputCucumberJson(true)
                .parallel(1);
        Assertions.assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}