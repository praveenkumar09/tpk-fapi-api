package org.tpkprav.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class SgNricValidator implements ConstraintValidator<SgNric, String> {

    private static final Pattern FORMAT = Pattern.compile("^[STFGMstfgm]\\d{7}[A-Za-z]$");
    private static final int[] WEIGHTS = {2, 7, 6, 5, 4, 3, 2};
    private static final String ST_CHECK = "JZIHGFEDCBA";
    private static final String FG_CHECK = "XWUTRQPNMLK";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || !FORMAT.matcher(value).matches()) {
            return false;
        }
        return checksumMatches(value.toUpperCase());
    }

    private static boolean checksumMatches(String nric) {
        char prefix = nric.charAt(0);
        int sum = 0;
        for (int i = 0; i < 7; i++) {
            sum += (nric.charAt(i + 1) - '0') * WEIGHTS[i];
        }
        int offset = (prefix == 'T' || prefix == 'G') ? 4 : (prefix == 'M') ? 3 : 0;
        sum += offset;
        int remainder = sum % 11;
        String table = (prefix == 'S' || prefix == 'T') ? ST_CHECK : FG_CHECK;
        return nric.charAt(8) == table.charAt(remainder);
    }
}
