package com.eventshigh.nearme.app.security;

import android.net.Uri;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Helper class which can be used to sign the URI.
 */
public class Signer {
    private static byte[] API_KEY = new byte[] {-7,91,-9,18,105,-84,102,67,-33,38,-38,-37,35,108,88,44};
    private static final String CIPHER_ALGORITHM = "AES";

    public static Uri sign(Uri uri) throws GeneralSecurityException, UnsupportedEncodingException {
        // Add timestamp.
        uri = uri.buildUpon()
                .appendQueryParameter("timestamp", Long.toString(System.currentTimeMillis()))
                .build();

        // Get the path with timestamp and sign it.
        String path = uri.getPath() + "?" + uri.getQuery();

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(API_KEY, CIPHER_ALGORITHM));

        String signature = Base64.encodeToString(
                cipher.doFinal(path.getBytes("UTF-8")),
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);

        // Add signature to the uri.
        return uri.buildUpon().appendQueryParameter("sign", signature).build();
    }
}
