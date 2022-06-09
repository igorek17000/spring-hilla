package com.example.application.bybit.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;


public class BybitEncryption {

    public static String genQueryString (
            TreeMap<String, String> params,
            String secret
    ) throws NoSuchAlgorithmException, InvalidKeyException {

        var keySet = params.keySet();
        var iter = keySet.iterator();
        var sb = new StringBuilder();

        while (iter.hasNext()) {
            String key = iter.next();
            sb.append(key);
            sb.append("=");
            sb.append(params.get(key));
            sb.append("&");
        }

        sb.deleteCharAt(sb.length() - 1);

        var sha256_HMAC = Mac.getInstance("HmacSHA256");
        var secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);

        return sb + "&sign=" + bytesToHex(sha256_HMAC.doFinal(sb.toString().getBytes()));
    }

    public static String bytesToHex( byte[] hash ) {
        var hexString = new StringBuilder();
        for ( byte b : hash ) {
            var hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
