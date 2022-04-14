package org.burgeon.legolas.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * @author Sam Lu
 * @date 2022/4/4
 */
public class EncryptUtil {

    public static String sha1(String username, String secret) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA");
        byte[] input = (username + secret).getBytes();
        md.update(input);
        byte[] result = md.digest();
        return Base64.getEncoder().encodeToString(result);
    }

}
