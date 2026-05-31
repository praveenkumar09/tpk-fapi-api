package org.tpkprav.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaskingUtilsTest {

    @Test
    void maskNric_masksAllButLastFour() {
        assertEquals("*****567D", MaskingUtils.maskNric("S1234567D"));
    }

    @Test
    void maskNric_shortNric_returnsStars() {
        assertEquals("****", MaskingUtils.maskNric("AB"));
    }

    @Test
    void maskNric_nullNric_returnsStars() {
        assertEquals("****", MaskingUtils.maskNric(null));
    }

    @Test
    void maskNric_exactlyFourChars_noMask() {
        assertEquals("1234", MaskingUtils.maskNric("1234"));
    }
}