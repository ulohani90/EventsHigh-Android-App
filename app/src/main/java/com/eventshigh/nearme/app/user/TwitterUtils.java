package com.eventshigh.nearme.app.user;

import android.util.Base64;

import com.twitter.sdk.android.core.TwitterAuthConfig;

import java.io.UnsupportedEncodingException;

public class TwitterUtils {
    private static final byte[] ENCODED_KEY = new byte[] {77,87,99,120,84,107,57,71,89,108,112,76,101,72,73,48,97,109,116,48,98,51,100,111,86,84,66,88,78,88,99,50,85,65,61,61};
    private static final byte[] ENCODED_SECRET = new byte[] {90,109,108,81,90,50,82,106,85,108,86,109,81,108,100,116,81,87,108,75,83,86,104,116,99,69,81,49,86,68,70,67,84,110,66,49,82,50,111,120,79,86,66,114,84,106,108,88,86,50,120,85,99,72,108,89,99,109,82,97,78,48,108,118,78,49,85,61};

    public static TwitterAuthConfig getAuthConfig() {
        try {
            String key = new String(Base64.decode(ENCODED_KEY, Base64.DEFAULT), "UTF-8");
            String secret = new String(Base64.decode(ENCODED_SECRET, Base64.DEFAULT), "UTF-8");
            return new TwitterAuthConfig(key, secret);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
