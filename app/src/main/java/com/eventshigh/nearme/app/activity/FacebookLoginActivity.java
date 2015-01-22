package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.user.Account;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;

import java.util.Arrays;

/**
 * Facebook login activity which asks users to login to facebook. If user is already logged in,
 * we directly go to next screen passed in parameter.
 */
public class FacebookLoginActivity extends BaseActivity {
    private Account account;
    private UiLifecycleHelper uiHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        account = new Account(this);
        if (account.getFacebookEmail() != null) {
            next();
            return;
        }

        setContentView(R.layout.activity_facebook_login);
        findViewById(R.id.skip).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });

        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);

        LoginButton authButton = (LoginButton) findViewById(R.id.auth_button);
        authButton.setReadPermissions(Arrays.asList("public_profile"));
    }

    public void onStart() {
        super.onStart();

        // Show the Up button in the action bar.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    private void next() {
        shareApp();
    }

    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            if (state == SessionState.CLOSED_LOGIN_FAILED) {
                Toast.makeText(FacebookLoginActivity.this, R.string.failed_login, Toast.LENGTH_SHORT).show();
                return;
            }

            if (state.isOpened()) {
                // TODO: record the facebook email id.
                Log.w("TEXT", "got facebook login");
            }
        }
    };
}
