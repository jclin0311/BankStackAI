package com.digitalbank.customerservice.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates the one-time password an Auth0 account is created with.
 *
 * <p>Nobody is meant to know this value: the customer receives a password-reset link and
 * chooses their own. It exists only because Auth0 requires a password when creating a
 * database user. Generating it per user means a leaked or guessed value compromises one
 * account rather than every account provisioned by KYC.</p>
 */
public final class TemporaryPasswords {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%^&*-_=+";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;

    private static final int LENGTH = 24;

    private TemporaryPasswords() {
    }

    /** @return a password satisfying Auth0's "fair" strength policy or stricter. */
    public static String generate() {
        List<Character> chars = new ArrayList<>(LENGTH);

        // guarantee one of each class, so generation never trips the strength policy
        chars.add(pick(UPPER));
        chars.add(pick(LOWER));
        chars.add(pick(DIGITS));
        chars.add(pick(SYMBOLS));

        while (chars.size() < LENGTH) {
            chars.add(pick(ALL));
        }
        Collections.shuffle(chars, RANDOM);

        StringBuilder sb = new StringBuilder(LENGTH);
        chars.forEach(sb::append);
        return sb.toString();
    }

    private static char pick(String alphabet) {
        return alphabet.charAt(RANDOM.nextInt(alphabet.length()));
    }
}
