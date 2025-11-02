package com.example.social.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class TokenUtils {
    private static final SecureRandom RNG = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    public static byte[] randomBytes(int length) {
        byte[] result = new byte[length];
        RNG.nextBytes(result);
        return result;
    }

    public static String sha256Hex(byte[] raw){
        try{
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(messageDigest.digest(raw));
        }
        catch (Exception e){
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static String sha256Hex(byte[] raw, int off, int len){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(raw, off, len);
            return HEX.formatHex(md.digest());
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

}
