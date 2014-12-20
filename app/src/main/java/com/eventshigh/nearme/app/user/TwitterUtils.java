package com.eventshigh.nearme.app.user;

import com.twitter.sdk.android.core.TwitterAuthConfig;

public class TwitterUtils {
    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    private static final String TWITTER_KEY = "1g1NOFbZKxr4jktowhU0W5w6P";
    private static final String TWITTER_SECRET = "fiPgdcRUfBWmAiJIXmpD5T1BNpuGj19PkN9WWlTpyXrdZ7Io7U";


    public static TwitterAuthConfig getAuthConfig() {
         return new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
    }
}
