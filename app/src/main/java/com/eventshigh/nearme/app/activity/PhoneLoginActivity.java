package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.util.Linkify;
import android.view.View;
import android.widget.EditText;
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
    private enum VerificationStatus {
        VERIFIED,
        CODE_SENT,
        RETRY
    }

    private View phoneNoParent;
    private View codeParent;
    private View verifiedParent;
    private View progressBar;

    private Account account;
    private TextInputLayout nameView;
    private TextInputLayout phoneNoView;
    private TextInputLayout codeView;
    private EditText nameEditText;
    private EditText phoneNoEditText;
    private EditText codeEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        phoneNoParent = findViewById(R.id.phone_no_parent);
        codeParent = findViewById(R.id.code_parent);
        verifiedParent = findViewById(R.id.verified_parent);

        nameView = (TextInputLayout) findViewById(R.id.name);
        phoneNoView = (TextInputLayout) findViewById(R.id.phone_no);
        codeView = (TextInputLayout) findViewById(R.id.code);
        progressBar = findViewById(R.id.top_progress_bar);

        nameView.setErrorEnabled(true);
        phoneNoView.setErrorEnabled(true);
        codeView.setErrorEnabled(true);

        nameEditText = nameView.getEditText();
        phoneNoEditText = phoneNoView.getEditText();
        codeEditText = codeView.getEditText();

        account = new Account(this);
        updateView();
    }

    @Override
    public View getViewForSnackbar() {
        return codeView;
    }

    public void sendCode(View view) {
        final String name = Utils.checkIfUnknown(nameEditText.getText().toString());
        if (name == null || name.length() < 3) {
            nameEditText.requestFocus();
            nameView.setError("Entered name is not correct");
            return;
        }

        final String phoneNo = Utils.simplifyPhoneNo(phoneNoEditText.getText().toString());
        phoneNoEditText.setText(phoneNo);
        if (phoneNo.length() < 10 || phoneNo.length() > 12) {
            phoneNoEditText.requestFocus();
            phoneNoView.setError("Entered phone number is not correct");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        Uri requestUrl = UpdateAccountInfoService.getBaseUri(this, "registerMobileNo")
                .appendQueryParameter("name", name)
                .appendQueryParameter("mobile_no", phoneNo)
                .build();
        try {
            VolleyHelper.addToRequestQueue(this,
                    new JsonObjectRequest(Method.GET, Signer.sign(requestUrl).toString(), null,
                            new Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject s, boolean isIntermediate) {
                                    progressBar.setVisibility(View.GONE);
                                    VerificationStatus status = parseStatus(s.optString("status"));
                                    if (status == VerificationStatus.RETRY) {
                                        reportActionToAnalytics("sendCodeRetry");
                                        showRetryMessage();
                                        return;
                                    }

                                    account.recordPhoneNumber(name, phoneNo);
                                    if (status == VerificationStatus.VERIFIED) {

                                        reportActionToAnalytics("sendCodeVerified");
                                        account.recordVerifiedPhoneNumber();
                                        startInterestActivity();
                                        //finish();
                                    } else if (status == VerificationStatus.CODE_SENT) {
                                        reportActionToAnalytics("sendCodeSuccess");
                                        startInterestActivity();
                                        //finish();
                                    }
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                    progressBar.setVisibility(View.GONE);
                                    reportActionToAnalytics("sendCodeRetry");
                                    VolleyHelper.log(PhoneLoginActivity.this, volleyError);
                                    showRetryMessage();
                                }
                            }
                    )
            );
        } catch (IOException | GeneralSecurityException e) {
            progressBar.setVisibility(View.GONE);
            reportActionToAnalytics("sendCodeRetry");
            Crashlytics.getInstance().core.logException(e);
            showRetryMessage();
        }
    }

    public void startInterestActivity(){
        Intent intent = new Intent(this,SelectInterestsActivity.class);
        intent.putExtra(SelectInterestsActivity.ONBOARDING_FLOW,true);
        startActivity(intent);
        finish();
    }

    public void verifyCode(View view) {
        progressBar.setVisibility(View.VISIBLE);
        Uri requestUrl = UpdateAccountInfoService.getBaseUri(this, "verifyMobileNo")
                .appendQueryParameter("mobile_no", phoneNoEditText.getText().toString())
                .appendQueryParameter("verification_code", codeEditText.getText().toString())
                .build();
        try {
            VolleyHelper.addToRequestQueue(this,
                    new JsonObjectRequest(Method.GET, Signer.sign(requestUrl).toString(), null,
                            new Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject s, boolean isIntermediate) {
                                    progressBar.setVisibility(View.GONE);
                                    VerificationStatus status = parseStatus(s.optString("status"));
                                    if (status != VerificationStatus.VERIFIED) {
                                        reportActionToAnalytics("verifyCodeRetry");
                                        showRetryMessage();
                                        return;
                                    }

                                    reportActionToAnalytics("verifyCodeSuccess");
                                    account.recordVerifiedPhoneNumber();
                                    setVerifiedMobileNoView();
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                    progressBar.setVisibility(View.GONE);
                                    reportActionToAnalytics("verifyCodeRetry");
                                    VolleyHelper.log(PhoneLoginActivity.this, volleyError);
                                    showRetryMessage();
                                }
                            }
                    )
            );
        } catch (IOException | GeneralSecurityException e) {
            progressBar.setVisibility(View.GONE);
            reportActionToAnalytics("verifyCodeRetry");
            Crashlytics.getInstance().core.logException(e);
            showRetryMessage();
        }
    }

    public void changeNumber(View view) {
        reportActionToAnalytics("changeNumber");
        account.removeUserInfo();
        setRequestMobileNoView();
    }

    private void updateView() {
        UserInfo userInfo = account.getUserInfo();
        nameEditText.setText(userInfo.name);
        phoneNoEditText.setText(userInfo.phoneNo);
        if (userInfo.phoneNo == null || userInfo.name == null) {
            setRequestMobileNoView();
        } else if (userInfo.isVerified) {
            setVerifiedMobileNoView();
        } else {
            setRequestCodeView();
        }
    }

    private void setPhoneNumberInStringResource(int viewId, int stringResourceId) {
        String codeLabelString = String.format(
            getResources().getString(stringResourceId), nameEditText.getText(), phoneNoEditText.getText());
        TextView codeLabelView = (TextView) findViewById(viewId);
        codeLabelView.setText(codeLabelString);
        Linkify.addLinks(codeLabelView, Linkify.PHONE_NUMBERS);
    }

    private void showRetryMessage() {
        showMessage(R.string.retry);
    }

    private VerificationStatus parseStatus(@Nullable String statusString) {
        try {
            if (statusString != null) {
                return VerificationStatus.valueOf(statusString.toUpperCase());
            }
        } catch (Exception e) {
            // do nothing.
            Crashlytics.getInstance().core.logException(e);
        }
        return VerificationStatus.RETRY;
    }

    // Set the UI elements when we need to ask for mobile no.
    private void setRequestMobileNoView() {
        phoneNoParent.setVisibility(View.VISIBLE);
        codeParent.setVisibility(View.GONE);
        verifiedParent.setVisibility(View.GONE);
    }

    // Set the UI elements when we need to ask for verification code.
    private void setRequestCodeView() {
        setPhoneNumberInStringResource(R.id.code_label, R.string.ui_code);
        phoneNoParent.setVisibility(View.GONE);
        codeParent.setVisibility(View.VISIBLE);
        verifiedParent.setVisibility(View.GONE);
    }

    // Set the UI elements when user mobile no is verified.
    private void setVerifiedMobileNoView() {
        setPhoneNumberInStringResource(R.id.verified, R.string.ui_code_verified);
        phoneNoParent.setVisibility(View.GONE);
        codeParent.setVisibility(View.GONE);
        verifiedParent.setVisibility(View.VISIBLE);
    }
}
