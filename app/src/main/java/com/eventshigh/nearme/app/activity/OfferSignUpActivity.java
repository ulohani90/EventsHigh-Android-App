package com.eventshigh.nearme.app.activity;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.data.stream.OfferObject;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.Signer;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import pl.snowdog.material.ui.ToolbarColorizeHelper;

/**
 * Created by umesh on 17/04/16.
 */
public class OfferSignUpActivity extends BaseActivity {


    OfferObject obj;
    ImageView offerBg;
    TextInputLayout mobileNum;
    TextInputLayout fullName;
    TextInputLayout emailAdd;
    EditText mobileEditText;
    EditText fullNameEditText;
    EditText emailAddEditText;
    TextView termsText;
    View progressBar;

    Account account;

    ProgressDialog progressDialog;

    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offer_signup);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        progressBar = findViewById(R.id.top_progress_bar);
        if (getIntent() != null) {
            obj = getIntent().getParcelableExtra("offer");

        }

        account = new Account(this);
        if (obj != null) {
            setUpData();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_event_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        } else if (item.getItemId() == R.id.action_share) {
            shareCoupon(obj);
        }
        return super.onOptionsItemSelected(item);
    }

    public void setUpData() {
        offerBg = (ImageView) findViewById(R.id.offer_bg);
        Glide.with(this).load(obj.imgUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                .into(offerBg);
        ((TextView) findViewById(R.id.title)).setText(obj.name);
        ((TextView) findViewById(R.id.subtitle)).setText(obj.desc);
        mobileNum = (TextInputLayout) findViewById(R.id.mobile_no);
        fullName = (TextInputLayout) findViewById(R.id.fullname);
        emailAdd = (TextInputLayout) findViewById(R.id.email_id);
        mobileEditText = mobileNum.getEditText();
        fullNameEditText = fullName.getEditText();
        emailAddEditText = emailAdd.getEditText();
        if (account.getUserInfo().isSignedIn) {
            emailAddEditText.setText(account.getUserInfo().email);
        }
        if (account.getUserInfo().isVerified) {
            mobileEditText.setText(account.getUserInfo().phoneNo);
            fullNameEditText.setText(account.getUserInfo().name);
        }
        termsText = (TextView) findViewById(R.id.terms_text);

        ((TextView) findViewById(R.id.signup_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUpClicked();
            }
        });
        String[] terms = obj.termsConditions.split("\\.");
        if (terms.length > 0) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < terms.length; i++) {
                builder.append("\u2022 ");
                builder.append(terms[i]);
                builder.append("\n");

            }
            termsText.setText(builder.toString());
        }
    }


    public void signUpClicked() {
        if (checkIfDetailsCorrect()) {
            progressDialog = ProgressDialog.show(this, null, "Signing up. Please wait..");
            reportActionToAnalytics("signupButtonClicked");

            Uri requestUrl = UpdateAccountInfoService.getBaseUri(this, "offer_signup")
                    .appendQueryParameter("mobile_no", mobileEditText.getText().toString())
                    .appendQueryParameter("offer_id", obj.id + "")
                    .appendQueryParameter("name", fullNameEditText.getText().toString())
                    .appendQueryParameter("email", emailAddEditText.getText().toString())
                    .build();


            try {
                VolleyHelper.addToRequestQueue(this,
                        new JsonObjectRequest(Request.Method.GET, Signer.sign(requestUrl).toString(), null,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject s, boolean isIntermediate) {
                                        if (progressDialog != null) {
                                            progressDialog.dismiss();
                                        }
                                        updatePreferencesForOffer();
                                        progressBar.setVisibility(View.GONE);
                                        showMessage("You have successfully signed up for the offer");
                                        finish();
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError volleyError) {
                                        if (progressDialog != null) {
                                            progressDialog.dismiss();
                                        }
                                        progressBar.setVisibility(View.GONE);
                                        VolleyHelper.log(OfferSignUpActivity.this, volleyError);
                                        showRetryMessage();
                                    }
                                }
                        )
                );
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    }

    private void showRetryMessage() {
        showMessage(R.string.retry);
    }

    public boolean checkIfDetailsCorrect() {
        if (fullNameEditText.getText() != null && fullNameEditText.getText().toString().length() > 0) {
            fullName.setErrorEnabled(false);
            if (mobileEditText.getText() != null && mobileEditText.getText().toString().length() == 10) {
                mobileNum.setErrorEnabled(false);

                if (emailAddEditText.getText() != null && Utils.isValidEmail(emailAddEditText.getText())) {
                    emailAdd.setErrorEnabled(false);
                    return true;
                } else {
                    emailAdd.setErrorEnabled(true);
                    emailAdd.setError("Valid Email Address required");
                    return false;
                }

            } else {
                mobileNum.setErrorEnabled(true);
                mobileNum.setError("Valid Mobile number required");
                return false;
            }
        } else {
            fullName.setErrorEnabled(true);
            fullName.setError("Full Name required");
            return false;
        }
    }

    private void setLightToolbarIcons() {
        toolbar.post(new Runnable() {
            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                ToolbarColorizeHelper.colorizeToolbar(toolbar,
                        getResources().getColor(android.R.color.white), OfferSignUpActivity.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (toolbar != null)
            setLightToolbarIcons();
    }

    @Override
    public void onBackPressed() {
        this.finish();
        // overridePendingTransition(R.anim.stay,R.anim.animate_up_bottom);
    }

    public void updatePreferencesForOffer() {

        Preferences preferences = Preferences.getInstance(this);

        StringBuilder builder = new StringBuilder();
        builder.append(preferences.getPrefOfferActedId());
        if (builder.length() > 0) {
            builder.append(",");
        }
        builder.append(obj.id + "");
        preferences.setPrefOfferActedId(builder.toString());

    }
}
