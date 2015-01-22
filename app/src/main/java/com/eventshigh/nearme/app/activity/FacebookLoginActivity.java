package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

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

/**
 * Facebook login activity which asks users to login to facebook. If user is already logged in,
 * we directly go to next screen passed in parameter.
 */
public class FacebookLoginActivity extends BaseActivity {
    private LoginButton authButton;
    private UiLifecycleHelper uiHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_facebook_login);
        findViewById(R.id.skip).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Account account = new Account(getApplicationContext());
                account.recordSkipLogin();
                up();
            }
        });

        authButton = (LoginButton) findViewById(R.id.auth_button);
        authButton.setReadPermissions(Arrays.asList("email", "public_profile"));
        authButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Session session = Session.getActiveSession();
                if (session != null && !session.isOpened() && !session.isClosed()) {
                    session.openForRead(new Session.OpenRequest(FacebookLoginActivity.this)
                            .setPermissions(Arrays.asList("email", "public_profile"))
                            .setCallback(callback));
                } else {
                    Session.openActiveSession(FacebookLoginActivity.this, true, callback);
                }
            }
        });

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
        NavUtils.navigateUpTo(this, new Intent(this, ShareAppActivity.class));
    }

    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            authButton.setSession(session);

            /*
            if (state == SessionState.CLOSED_LOGIN_FAILED) {
                Account account = new Account(getApplicationContext());
                account.recordLoginFailure();
                Toast.makeText(FacebookLoginActivity.this, R.string.failed_login, Toast.LENGTH_SHORT).show();
                return;
            }
            */

            if (state.isOpened()) {
                Request.newMeRequest(session, new GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        Account account = new Account(getApplicationContext());
                        account.recordFacebookEmail(user.getProperty("email").toString());
                        up();
                    }
                }).executeAsync();
            }
        }
    };
}
