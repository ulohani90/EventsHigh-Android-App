package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.user.Account;

import java.util.Date;

public class LoginActivity extends BaseActivity {
    private boolean isGoingToEvents = false;
    private long activityStartTime = 0;
    private Account account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Check if we should ask for login or not. If not, go to MapsActivity.
        account = new Account(this);
        if (!account.shouldAskForLogin()) {
            launchEventsActivity();
        }

        // Initialize Digits.
        activityStartTime = new Date().getTime();

        /**
        // Setup the content of login screen.
        DigitsAuthButton digitsButton = (DigitsAuthButton) findViewById(R.id.auth_button);

        // Setup the Digits button.
        digitsButton.setAuthTheme(R.style.AppTheme);
        digitsButton.setCallback(new AuthCallback() {
            @Override
            public void success(DigitsSession session, String phoneNumber) {
                reportActionToAnalytics("loginSuccess");
                account.recordSuccess(session, phoneNumber);
                launchEventsActivity();
            }

            @Override
            public void failure(DigitsException exception) {
                reportActionToAnalytics("loginFailed");
                Toast.makeText(LoginActivity.this, R.string.failed_login, Toast.LENGTH_SHORT).show();
                if (account.recordFailure()) {
                    launchEventsActivity();
                }
            }
        });
         **/
    }

    @Override
    protected void onStop() {
        if (!isGoingToEvents) {
            reportActionToAnalytics("loginBounced");
        }

        super.onStop();
    }

    public void skip(View view) {
        reportActionToAnalytics("loginSkipped");
        if (!BuildConfig.DEBUG) {
            account.recordSkipLogin();
        }

        launchEventsActivity();
    }

    private void launchEventsActivity() {
        // Report the time taken by login action.
        isGoingToEvents = true;
        if (activityStartTime > 0) {
            reportActionToAnalytics("loginActionTimeMillis", "",
                    new Date().getTime() - activityStartTime);
        }

        Class target = LaunchActivity.isMapsViewDefault(this)
                ? MapsActivity.class : EventGridActivity.class;
        startActivity(new Intent(this, target));
    }
}
