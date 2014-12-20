package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.digits.sdk.android.AuthCallback;
import com.digits.sdk.android.DigitsAuthButton;
import com.digits.sdk.android.DigitsException;
import com.digits.sdk.android.DigitsSession;
import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.TwitterUtils;
import com.google.android.gms.analytics.HitBuilders;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;

import java.util.Date;

import io.fabric.sdk.android.Fabric;

public class LoginActivity extends BaseActivity {
    private boolean isGoingToMaps = false;
    private long activityStartTime = 0;
    private Account account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if we should ask for login or not. If not, go to MapsActivity.
        account = new Account(this);
        if (!account.shouldAskForLogin()) {
            launchMaps();
        }

        // Initialize Digits.
        activityStartTime = new Date().getTime();
        TwitterAuthConfig authConfig = TwitterUtils.getAuthConfig();
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
                account.recordSuccess(session, phoneNumber);
                launchMaps();
            }

            @Override
            public void failure(DigitsException exception) {
                reportActionToAnalytics("loginFailed");
                Toast.makeText(LoginActivity.this, R.string.failed_login, Toast.LENGTH_SHORT).show();
                if (account.recordFailure()) {
                    launchMaps();
                }
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
        if (!BuildConfig.DEBUG) {
            account.recordSkip();
        }

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
