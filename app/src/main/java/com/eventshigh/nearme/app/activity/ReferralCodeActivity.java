package com.eventshigh.nearme.app.activity;


import android.content.Intent;
import android.net.Uri;

import android.os.Bundle;

import android.net.Uri;

import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.Signer;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;


/**
 * Created by umesh on 10/05/16.
 */

public class ReferralCodeActivity extends BaseActivity {

    private enum VerificationStatus {
        SUCCESS,
        FAILURE,
        RETRY
    }

    TextInputLayout referralCode;
    private EditText referralCodeEditText;
    ProgressBar topProgressBar;
    Account account;
    private LinearLayout verifyPhnLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_referral_code);
        topProgressBar = (ProgressBar) findViewById(R.id.top_progress_bar);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Enter Referral Code");
        toolbar.setTitle("Enter Referral Code");
        account = new Account(this);
        referralCode = (TextInputLayout) findViewById(R.id.referral_code);
        referralCodeEditText = referralCode.getEditText();
        findViewById(R.id.send_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCodeToServer();
            }
        });

        verifyPhnLayout = (LinearLayout) findViewById(R.id.verify_phn_layout);

        (findViewById(R.id.verify_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyClicked();
            }
        });

        //checkisPhoneVerified();
    }

    /* public boolean checkisPhoneVerified() {
         if (!account.getUserInfo().isVerified) {
             PhoneVerificationDialog.show(this, R.string.ui_verify_phone, R.string.ui_phone_verify_pa);
             return false;
         }
         return true;
 <<<<<<< HEAD

     }

     }*/
    public void verifyClicked() {
        startActivity(new Intent(this, PhoneLoginActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (account != null && !(account.getUserInfo().isVerified)) {
            verifyPhnLayout.setClickable(true);
            verifyPhnLayout.setVisibility(View.VISIBLE);
        } else {
            verifyPhnLayout.setVisibility(View.GONE);
        }
    }

    public void sendCodeToServer() {

        if (checkIsValid()) {
            topProgressBar.setVisibility(View.VISIBLE);
            reportActionToAnalytics("submitReferCodenClick");

            Uri requestUrl = UpdateAccountInfoService.getBaseUri(this, "reportRefCode")
                    .appendQueryParameter("ref_code", referralCodeEditText.getText().toString())
                    .build();

            try {
                VolleyHelper.addToRequestQueue(this,
                        new JsonObjectRequest(Request.Method.GET, Signer.sign(requestUrl).toString(), null,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject s, boolean isIntermediate) {
                                        topProgressBar.setVisibility(View.GONE);
                                        VerificationStatus status = parseStatus(s.optString("status"));
                                        if (status == VerificationStatus.FAILURE) {
                                            showMessage(s.optString("message"));
                                            return;
                                        }
                                        showMessage("You have successfully submitted the referrer code");
                                        finish();
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError volleyError) {
                                        topProgressBar.setVisibility(View.GONE);
                                        VolleyHelper.log(ReferralCodeActivity.this, volleyError);
                                        showMessage(volleyError.getMessage());
                                    }
                                }
                        ));
            } catch (UnsupportedEncodingException | GeneralSecurityException e) {
                topProgressBar.setVisibility(View.GONE);

                showRetryMessage();
            }


        }


    }

    private VerificationStatus parseStatus(@Nullable String statusString) {
        try {
            if (statusString != null) {
                return VerificationStatus.valueOf(statusString.toUpperCase());
            }
        } catch (Exception e) {
            // do nothing.

        }
        return VerificationStatus.RETRY;
    }

    private void showRetryMessage() {
        showMessage(R.string.retry);
    }

    public boolean checkIsValid() {
        if (referralCodeEditText.getText().length() > 0) {
            referralCode.setErrorEnabled(false);
            return true;
        } else {
            referralCode.setErrorEnabled(true);
            referralCode.setError("Valid referral code required");
            return false;
        }
    }

}
