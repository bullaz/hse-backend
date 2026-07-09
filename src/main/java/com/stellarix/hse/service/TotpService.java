package com.stellarix.hse.service;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class TotpService {

    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP   = 30;
    private static final int WINDOW      = 2; // accept ±2 steps (±60s) to handle clock skew

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    public String generateSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return base32Encode(bytes);
    }

    public String getQrCodeUri(String secret, String email) {
        String issuer = "Stellarix%20HSE";
        String account = email.replace("@", "%40");
        return "otpauth://totp/" + issuer + ":" + account
                + "?secret=" + secret
                + "&issuer=" + issuer
                + "&algorithm=SHA1&digits=6&period=30";
    }

    public boolean verifyCode(String secret, String code) {
        return verifyCodeStep(secret, code, null) >= 0;
    }

    /**
     * Same check as {@link #verifyCode}, but also rejects a code matching a time-step
     * that's already been accepted before — without this, a valid code can be replayed
     * as many times as requested within its ~60s window.
     * @return the matched time-step (to persist as the new "last used"), or -1 if invalid.
     */
    public long verifyCodeStep(String secret, String code, Long lastUsedStep) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) return -1;
        long timeIndex = System.currentTimeMillis() / 1000 / TIME_STEP;
        for (int i = -WINDOW; i <= WINDOW; i++) {
            long step = timeIndex + i;
            if ((lastUsedStep == null || step > lastUsedStep) && generateCode(secret, step).equals(code)) {
                return step;
            }
        }
        return -1;
    }

    private String generateCode(String secret, long counter) {
        try {
            byte[] key      = base32Decode(secret);
            byte[] msg      = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac         = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash     = mac.doFinal(msg);
            int offset      = hash[hash.length - 1] & 0x0F;
            int binary      = ((hash[offset]     & 0x7F) << 24)
                            | ((hash[offset + 1] & 0xFF) << 16)
                            | ((hash[offset + 2] & 0xFF) <<  8)
                            |  (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception e) {
            throw new RuntimeException("TOTP generation failed", e);
        }
    }

    private String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                sb.append(BASE32_CHARS.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }
        if (bitsLeft > 0) sb.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        return sb.toString();
    }

    private byte[] base32Decode(String base32) {
        String cleaned = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        byte[] result  = new byte[cleaned.length() * 5 / 8];
        int buffer = 0, bitsLeft = 0, idx = 0;
        for (char c : cleaned.toCharArray()) {
            buffer = (buffer << 5) | BASE32_CHARS.indexOf(c);
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                result[idx++] = (byte) ((buffer >> bitsLeft) & 0xFF);
            }
        }
        return result;
    }
}
