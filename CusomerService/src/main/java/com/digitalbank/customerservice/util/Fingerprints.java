// com.digitalbank.customerservice.util.Fingerprints
package com.digitalbank.customerservice.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Fingerprints {
  private Fingerprints() {}
  public static String customerCreate(String firstName, String lastName,
                                      String email, String phone, String address) {
    String s = (firstName == null ? "" : firstName.trim().toLowerCase()) + "|" +
               (lastName  == null ? "" : lastName.trim().toLowerCase())  + "|" +
               (email     == null ? "" : email.trim().toLowerCase())     + "|" +
               (phone     == null ? "" : phone.trim())                   + "|" +
               (address   == null ? "" : address.trim().toLowerCase());
    return sha256(s);
  }
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
