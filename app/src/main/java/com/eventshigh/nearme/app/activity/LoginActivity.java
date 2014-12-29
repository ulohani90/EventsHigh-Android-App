package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.user.Account;

import java.util.Date;

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

        /**
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
         **/
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
            account.recordSkipLogin();
        }

        launchMaps();
    }

    private void launchMaps() {
        // Report the time taken by login action.
        isGoingToMaps = true;
        if (activityStartTime > 0) {
            reportActionToAnalytics("loginActionTimeMillis", "",
                    new Date().getTime() - activityStartTime);
        }

        startActivity(new Intent(this, MapsActivity.class));
    }
}
