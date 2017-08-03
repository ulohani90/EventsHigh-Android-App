package com.eventshigh.nearme.app.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import com.eventshigh.nearme.app.R;

/**
 * Created by umesh on 19/07/16.
 */
public class FBLoginActivity extends BaseActivity {

    public static final int RC_SIGN_IN = 89;

    FbLoginFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.empty_layout);

        FrameLayout container = (FrameLayout) findViewById(R.id.container);


        boolean hideSkip = getIntent().getBooleanExtra("hide_skip", false);
        boolean isLogout = getIntent().getBooleanExtra("is_logout", false);

        boolean showSpecialText = getIntent().getBooleanExtra("show_special_text", false);

        // GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this);

        fragment = FbLoginFragment.newInstance(hideSkip, showSpecialText, isLogout, true);

        getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).commit();


    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FbLoginFragment.LOGOUT_BROADCAST_ACTION);
        registerReceiver(receiver, intentFilter);
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("onReceive", "Logout in progress");

            finish();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    /* @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            fragment.handleSignInResult(result);
        }
    }*/

}
