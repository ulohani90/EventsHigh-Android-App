package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.user.Account;
import com.facebook.Request;
import com.facebook.Request.GraphUserCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;

import java.util.Arrays;
import java.util.List;

/**
 * Facebook login activity which asks users to login to facebook. If user is already logged in,
 * we directly go to next screen passed in parameter.
 */
public class FacebookLoginActivity extends BaseActivity {
    private static final List<String> PERMISSIONS = Arrays.asList("email", "public_profile");
    public static final String PARAM_ONBOARDING = "onboarding";

    private TextView connectMessageView;
    private LoginButton authButton;
    private UiLifecycleHelper uiHelper;
    private boolean isOnBoarding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onNewIntent(getIntent());

        setContentView(R.layout.activity_facebook_login);
        findViewById(R.id.skip).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                reportActionToAnalytics("skipLogin");
                Account account = new Account(getApplicationContext());
                account.recordSkipLogin();
                up();
            }
        });

        connectMessageView = ((TextView)findViewById(R.id.fb_connect_message));
        authButton = (LoginButton) findViewById(R.id.auth_button);
        authButton.setReadPermissions(PERMISSIONS);

        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            up();
            return true;
        }

        return super.onOptionsItemSelected(item);
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

        // For scenarios where the main activity is launched and user
        // session is not null, the session state change notification
        // may not be triggered. Trigger it if it's open/closed.
        Session session = Session.getActiveSession();
        if (session != null) {
            authButton.setSession(session);
            if (session.isOpened()) {
                callback.call(session, session.getState(), null);
            }
        }

        uiHelper.onResume();
    }

    @Override
    public void onNewIntent(Intent intent) {
        isOnBoarding = intent.hasExtra(PARAM_ONBOARDING);
        setTheme(isOnBoarding ? android.R.style.Theme_NoTitleBar : R.style.AppTheme);
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

    private void up() {
        if (isOnBoarding) {
            startActivity(new Intent(FacebookLoginActivity.this, LaunchActivity.class));
        } else {
            NavUtils.navigateUpFromSameTask(this);
        }
        finish();
    }

    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            authButton.setSession(session);

            if (state.isOpened()) {
                reportActionToAnalytics("loginSuccessFull");
                Request.newMeRequest(session, new GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        reportActionToAnalytics("gotFBEmail");
                        Account account = new Account(getApplicationContext());
                        String email = user.getProperty("email").toString();
                        account.recordFacebookEmail(email);
                        connectMessageView.setText("Logged in as " + email);
                        if (isOnBoarding) {
                            up();
                        }
                    }
                }).executeAsync();
            } else {
                connectMessageView.setText(R.string.ui_connect_facebook);
            }
        }
    };
}
