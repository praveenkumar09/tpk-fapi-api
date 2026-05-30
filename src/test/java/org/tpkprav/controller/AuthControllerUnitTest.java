package org.tpkprav.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthControllerUnitTest {

    @Test
    void maskNric_masksAllButLastFour() {
        assertEquals("*****567D", AuthController.maskNric("S1234567D"));
    }

    @Test
    void maskNric_shortNric_returnsStars() {
        assertEquals("****", AuthController.maskNric("AB"));
    }

    @Test
    void maskNric_nullNric_returnsStars() {
        assertEquals("****", AuthController.maskNric(null));
    }

    @Test
    void maskNric_exactlyFourChars_noMask() {
        assertEquals("1234", AuthController.maskNric("1234"));
    }
}