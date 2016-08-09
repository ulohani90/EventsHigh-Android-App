package com.eventshigh.nearme.app.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.network.AddFacebookUserInfoRequest;
import com.eventshigh.nearme.app.user.Account;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Created by umesh on 19/07/16.
 */
public class FBLoginActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.empty_layout);

        FrameLayout container = (FrameLayout) findViewById(R.id.container);


        boolean hideSkip = getIntent().getBooleanExtra("hide_skip", false);


        boolean showSpecialText = getIntent().getBooleanExtra("show_special_text", false);

        FbLoginFragment fragment = FbLoginFragment.newInstance(hideSkip, showSpecialText, true);

        getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).commit();

    }

}
