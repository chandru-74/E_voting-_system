package com.example.E_voting_System.util;

/**
 * Validates Aadhaar numbers using the Verhoeff checksum algorithm —
 * the same checksum UIDAI uses when generating real Aadhaar numbers.
 *
 * This does NOT verify the Aadhaar was actually issued to a real person
 * (only UIDAI's database can do that). It only rejects numbers that are
 * mathematically impossible to be valid — catching typos and made-up
 * numbers like "123456789012".
 */
public class AadhaarValidator {

    private AadhaarValidator() {} // utility — no instantiation

    // Verhoeff multiplication table
    private static final int[][] MULTIPLICATION_TABLE = {
            {0,1,2,3,4,5,6,7,8,9},
            {1,2,3,4,0,6,7,8,9,5},
            {2,3,4,0,1,7,8,9,5,6},
            {3,4,0,1,2,8,9,5,6,7},
            {4,0,1,2,3,9,5,6,7,8},
            {5,9,8,7,6,0,4,3,2,1},
            {6,5,9,8,7,1,0,4,3,2},
            {7,6,5,9,8,2,1,0,4,3},
            {8,7,6,5,9,3,2,1,0,4},
            {9,8,7,6,5,4,3,2,1,0}
    };

    // Verhoeff permutation table
    private static final int[][] PERMUTATION_TABLE = {
            {0,1,2,3,4,5,6,7,8,9},
            {1,5,7,6,2,8,3,0,9,4},
            {5,8,0,3,7,9,6,1,4,2},
            {8,9,1,6,0,4,3,5,2,7},
            {9,4,5,3,1,2,6,8,7,0},
            {4,2,8,6,5,7,3,9,0,1},
            {2,7,9,3,8,0,6,4,1,5},
            {7,0,4,6,9,1,3,2,5,8}
    };

    /**
     * Checks whether a 12-digit Aadhaar string satisfies the Verhoeff
     * checksum. Returns false for anything that isn't exactly 12 digits.
     */
    public static boolean isValidChecksum(String aadhaar) {
        if (aadhaar == null || !aadhaar.matches("\\d{12}")) {
            return false;
        }

        // Verhoeff works right-to-left over all 12 digits, including
        // the last one — the checksum digit is baked into the number
        // itself, not appended separately, for this algorithm's use here.
        int checksum = 0;
        String reversed = new StringBuilder(aadhaar).reverse().toString();

        for (int i = 0; i < reversed.length(); i++) {
            int digit = Character.getNumericValue(reversed.charAt(i));
            checksum = MULTIPLICATION_TABLE[checksum][PERMUTATION_TABLE[i % 8][digit]];
        }

        return checksum == 0;
    }

    /**
     * Extra structural rule real Aadhaar numbers follow:
     * the first digit is never 0 or 1.
     */
    public static boolean hasValidFirstDigit(String aadhaar) {
        if (aadhaar == null || aadhaar.isEmpty()) return false;
        char first = aadhaar.charAt(0);
        return first != '0' && first != '1';
    }

    /**
     * Combined check — both rules must pass.
     */
    public static boolean isPlausibleAadhaar(String aadhaar) {
        return hasValidFirstDigit(aadhaar) && isValidChecksum(aadhaar);
    }

    /**
     * DEV-ONLY helper: given the first 11 digits, computes a 12th digit
     * that makes the whole number pass the Verhoeff checksum + first-digit rule.
     * Not part of real Aadhaar generation — just for generating test data locally.
     */
    public static String generateValidTestAadhaar(String first11Digits) {
        if (first11Digits == null || !first11Digits.matches("\\d{11}")) {
            throw new IllegalArgumentException("Need exactly 11 digits, got: " + first11Digits);
        }
        char first = first11Digits.charAt(0);
        if (first == '0' || first == '1') {
            throw new IllegalArgumentException("First digit can't be 0 or 1");
        }
        for (int lastDigit = 0; lastDigit <= 9; lastDigit++) {
            String candidate = first11Digits + lastDigit;
            if (isValidChecksum(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No valid last digit found — this shouldn't happen");
    }
}