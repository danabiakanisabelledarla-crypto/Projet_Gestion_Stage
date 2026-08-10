package com.gestionstages.gestion_stages.services;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Service
public class TotpService {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final long TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSecret() {
        byte[] bytes = new byte[20];
        secureRandom.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public boolean verify(String secret, String submittedCode) {
        if (secret == null || submittedCode == null || !submittedCode.matches("\\d{6}")) {
            return false;
        }
        long currentWindow = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;
        int expected = Integer.parseInt(submittedCode);
        for (long offset = -1; offset <= 1; offset++) {
            if (generateCode(secret, currentWindow + offset) == expected) {
                return true;
            }
        }
        return false;
    }

    public String provisioningUri(String secret, String email, String issuer) {
        String encodedIssuer = urlEncode(issuer);
        String label = encodedIssuer + ":" + urlEncode(email);
        return "otpauth://totp/" + label + "?secret=" + secret
                + "&issuer=" + encodedIssuer + "&algorithm=SHA1&digits=6&period=30";
    }

    int generateCode(String secret, long counter) {
        try {
            byte[] key = decodeBase32(secret);
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            return binary % 1_000_000;
        } catch (Exception exception) {
            throw new IllegalStateException("Generation TOTP impossible", exception);
        }
    }

    private String encodeBase32(byte[] input) {
        StringBuilder output = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte value : input) {
            buffer = (buffer << 8) | (value & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                output.append(ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 31));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            output.append(ALPHABET.charAt((buffer << (5 - bitsLeft)) & 31));
        }
        return output.toString();
    }

    private byte[] decodeBase32(String input) {
        String normalized = input.replace("=", "").replace(" ", "").toUpperCase();
        byte[] result = new byte[normalized.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char character : normalized.toCharArray()) {
            int value = ALPHABET.indexOf(character);
            if (value < 0) {
                throw new IllegalArgumentException("Secret Base32 invalide");
            }
            buffer = (buffer << 5) | value;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        return result;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
