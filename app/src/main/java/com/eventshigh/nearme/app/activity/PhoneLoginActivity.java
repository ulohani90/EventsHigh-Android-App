package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.volley.Request.Method;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.Signer;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Account.UserInfo;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * This screen allows user to register and verify his phone number.
 */
public class PhoneLoginActivity extends BaseActivity {

    PhoneLoginFragment fragment;

    boolean showSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.empty_layout);
        FrameLayout container = (FrameLayout) findViewById(R.id.container);

        showSkip = getIntent().getBooleanExtra("hide_skip", false);
        boolean isLogout = getIntent().getBooleanExtra("is_logout", false);

        fragment = PhoneLoginFragment.newInstance(showSkip, isLogout, true);

        getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).commit();
    }

    @Override
    public View getViewForSnackbar() {
        if (fragment != null && fragment.getCodeView() != null) {
            return fragment.getCodeView();
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.register_phone_menu, menu);
        MenuItem skipBtn = menu.findItem(R.id.action_skip_register_phone);
        skipBtn.setVisible(showSkip);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_skip_register_phone) {

            if (fragment != null)
                fragment.startNextTask();
        }
        return super.onOptionsItemSelected(item);
    }
}
