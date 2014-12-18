package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.digits.sdk.android.AuthCallback;
import com.digits.sdk.android.DigitsAuthButton;
import com.digits.sdk.android.DigitsException;
import com.digits.sdk.android.DigitsSession;
import com.eventshigh.nearme.app.R;
import com.google.android.gms.analytics.HitBuilders;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;

import java.util.Date;

import io.fabric.sdk.android.Fabric;

public class LoginActivity extends BaseActivity {
    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    private static final String TWITTER_KEY = "1g1NOFbZKxr4jktowhU0W5w6P";
    private static final String TWITTER_SECRET = "fiPgdcRUfBWmAiJIXmpD5T1BNpuGj19PkN9WWlTpyXrdZ7Io7U";

    private static final String PREFS_FILE_NAME = "user_creds";
    private static final String PREF_USER_ID = "user_id";
    private static final String PREF_PHONE_NO = "phone_no";
    private static final String PREF_NUM_LOGIN_ATTEMPTS = "num_login_attemts";
    private static final String PREF_ASK_LOGIN = "ask_login";

    private static final int MAX_FAILED_ATTEMPT = 3;

    private SharedPreferences settings;
    private boolean isGoingToMaps = false;
    private long activityStartTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if we should ask for login or not. If not, go to MapsActivity.
        settings = getSharedPreferences(PREFS_FILE_NAME, 0);
        boolean askForLogIn = settings.getBoolean(PREF_ASK_LOGIN, true);
        if (!askForLogIn) {
            launchMaps();
        }

        // Initialize Digits.
        activityStartTime = new Date().getTime();
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));

        // Setup the content of login screen.
        setContentView(R.layout.activity_login);
        DigitsAuthButton digitsButton = (DigitsAuthButton) findViewById(R.id.auth_button);

        // Setup the Digits button.
        digitsButton.setAuthTheme(R.style.AppTheme);
        digitsButton.setCallback(new AuthCallback() {
            @Override
            public void success(DigitsSession session, String phoneNumber) {
                reportActionToAnalytics("loginSuccess");

                SharedPreferences.Editor editor = settings.edit();
                editor.putLong(PREF_USER_ID, session.getId());
                editor.putString(PREF_PHONE_NO, phoneNumber);
                editor.putBoolean(PREF_ASK_LOGIN, false);
                editor.apply();

                launchMaps();
            }

            @Override
            public void failure(DigitsException exception) {
                reportActionToAnalytics("loginFailed");

                Toast.makeText(LoginActivity.this, R.string.failed_login, Toast.LENGTH_SHORT).show();

                SharedPreferences.Editor editor = settings.edit();
                int numFailedLogin = settings.getInt(PREF_NUM_LOGIN_ATTEMPTS, 0) + 1;
                editor.putInt(PREF_NUM_LOGIN_ATTEMPTS, numFailedLogin);
                if (numFailedLogin >= MAX_FAILED_ATTEMPT) {
                    editor.putBoolean(PREF_ASK_LOGIN, false);
                }
                editor.apply();
            }
        });
    }

    @Override
    protected void onStop() {
        if (!isGoingToMaps) {
            reportActionToAnalytics("loginBounced");
        }

        super.onStop();
    }

    public void skip(View view) {
        reportActionToAnalytics("loginSkipped");

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_ASK_LOGIN, false);
        editor.apply();

        launchMaps();
    }

    private void launchMaps() {
        // Report the time taken by login action.
        isGoingToMaps = true;
        if (activityStartTime > 0) {
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(getClass().getSimpleName())
                    .setAction("loginActionTimeMillis")
                    .setLabel("")
                    .setValue(new Date().getTime() - activityStartTime)
                    .build());
        }

        startActivity(new Intent(this, MapsActivity.class));
    }
}
