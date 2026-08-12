package com.account.util;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;


public final class Fingerprints {
private Fingerprints() {}


public static String createAccount(String customerId, String type, String subType, String currency, String nickname, String displayName) {
String s = n(customerId) + "|" + n(type) + "|" + n(subType) + "|" + n(currency) + "|" + n(nickname) + "|" + n(displayName);
return sha256(s);
}


public static String hold(String accountId, String amount, String currency, String type, String reason) {
String s = n(accountId) + "|" + n(amount) + "|" + n(currency) + "|" + n(type) + "|" + n(reason);
return sha256(s);
}


public static String posting(String accountId, String side, String amount, String currency, String reason) {
String s = n(accountId) + "|" + n(side) + "|" + n(amount) + "|" + n(currency) + "|" + n(reason);
return sha256(s);
}


private static String n(String s) { return s == null ? "" : s.trim().toLowerCase(); }


private static String sha256(String s) {
try {
MessageDigest md = MessageDigest.getInstance("SHA-256");
byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
StringBuilder sb = new StringBuilder();
for (byte x : b) sb.append(String.format("%02x", x));
return sb.toString();
} catch (Exception e) { throw new RuntimeException(e); }
}
}