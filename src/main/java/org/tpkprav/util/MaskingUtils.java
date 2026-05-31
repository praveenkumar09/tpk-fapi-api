package org.tpkprav.util;

public final class MaskingUtils {

    private MaskingUtils() {}

    public static String maskNric(String nric) {
        if (nric == null || nric.length() < 4) {
            return "****";
        }
        return "*".repeat(nric.length() - 4) + nric.substring(nric.length() - 4);
    }
}