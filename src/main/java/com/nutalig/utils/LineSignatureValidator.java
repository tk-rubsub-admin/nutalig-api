package com.nutalig.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class LineSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public static boolean validate(String channelSecret, String requestBody, String signature) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                    channelSecret.getBytes(),
                    HMAC_SHA256
            );

            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(keySpec);

            byte[] hash = mac.doFinal(requestBody.getBytes());
            String computedSignature = Base64.getEncoder().encodeToString(hash);

            return computedSignature.equals(signature);

        } catch (Exception e) {
            throw new RuntimeException("Failed to validate LINE signature", e);
        }
    }
}