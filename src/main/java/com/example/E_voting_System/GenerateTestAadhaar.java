package com.example.E_voting_System;

import com.example.E_voting_System.util.AadhaarValidator;
import java.util.Scanner;

public class GenerateTestAadhaar {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        StringBuilder prefix = new StringBuilder();
        final int LIMIT = 11;

        System.out.println("Enter digits (type in chunks, e.g. '123' then '456'). "
                + "Stops automatically at " + LIMIT + " digits.\n");

        while (prefix.length() < LIMIT) {
            System.out.print("Current: \"" + prefix + "\" (" + prefix.length() + "/" + LIMIT + ") — enter more: ");
            String chunk = scanner.nextLine().trim();

            if (!chunk.matches("\\d*")) {
                System.out.println("⚠️  Digits only — ignoring that input.\n");
                continue;
            }

            for (char c : chunk.toCharArray()) {
                if (prefix.length() >= LIMIT) {
                    System.out.println("⚠️  Limit reached — ignoring extra character '" + c + "'.");
                    break;
                }
                prefix.append(c);
            }

            System.out.println("Now: \"" + prefix + "\" (" + prefix.length() + "/" + LIMIT + ")\n");
        }

        String finalPrefix = prefix.toString();
        System.out.println("✅ Reached " + LIMIT + " digits: " + finalPrefix + "\n");

        try {
            String valid = AadhaarValidator.generateValidTestAadhaar(finalPrefix);
            System.out.println("Valid test Aadhaar: " + valid);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        scanner.close();
    }
}