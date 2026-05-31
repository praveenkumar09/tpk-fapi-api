package org.tpkprav;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(KarateTestProfile.class)
class AuthApiKarateTest {

    @Test
    void runKarateFeatures() {
        System.setProperty("quarkus.http.test-port",
                String.valueOf(io.restassured.RestAssured.port));

        Results results = Runner
                .path("classpath:karate")
                .outputCucumberJson(true)
                .parallel(1);

        Assertions.assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}