package com.example.application.bybit.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;


public class BybitEncryption {

    public static String genQueryString (
            TreeMap<String, String> params,
            String secret) throws NoSuchAlgorithmException, InvalidKeyException {

        Set<String> keySet = params.keySet();
        Iterator<String> iter = keySet.iterator();
        StringBuilder sb = new StringBuilder();

        while (iter.hasNext()) {
            String key = iter.next();
            sb.append(key);
            sb.append("=");
            sb.append(params.get(key));
            sb.append("&");
        }

        sb.deleteCharAt(sb.length() - 1);

        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);

        return sb + "&sign=" + bytesToHex(sha256_HMAC.doFinal(sb.toString().getBytes()));
    }

    public static String bytesToHex( byte[] hash ) {
        StringBuilder hexString = new StringBuilder();
        for ( byte b : hash ) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
