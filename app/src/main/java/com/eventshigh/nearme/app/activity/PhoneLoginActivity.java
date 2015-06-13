package com.eventshigh.nearme.app.activity;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.util.Linkify;
import android.util.Pair;
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
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.AccountStateReporter;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * This screen allows user to register and verify his phone number.
 */
public class PhoneLoginActivity extends BaseActivity {
    public static final String EXTRA_IN_ONBOARDING_FLOW = "inOnboardingFlow";

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
    private EditText phoneNoView;
    private EditText codeView;

    private boolean inOnboardingFlow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        phoneNoParent = findViewById(R.id.phone_no_parent);
        codeParent = findViewById(R.id.code_parent);
        verifiedParent = findViewById(R.id.verified_parent);

        phoneNoView = (EditText) findViewById(R.id.phone_no);
        codeView = (EditText) findViewById(R.id.code);
        progressBar = findViewById(R.id.top_progress_bar);

        account = new Account(this);
        inOnboardingFlow = getIntent().getBooleanExtra(EXTRA_IN_ONBOARDING_FLOW, false);
        updateView();
    }

    @Override
    public View getViewForSnackbar() {
        return codeView;
    }

    public void sendCode(View view) {
        final String phoneNo = Utils.simplifyPhoneNo(phoneNoView.getText().toString());
        if (phoneNo.length() < 10 || phoneNo.length() > 12) {
            phoneNoView.requestFocus();
            showMessage("Entered phone number is not correct");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        Uri requestUrl = AccountStateReporter.getBaseUri(this, "registerMobileNo")
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

                                    account.recordPhoneNumber(phoneNo);
                                    if (status == VerificationStatus.VERIFIED) {
                                        reportActionToAnalytics("sendCodeVerified");
                                        account.recordVerifiedPhoneNumber();
                                        setVerifiedMobileNoView();
                                    } else if (status == VerificationStatus.CODE_SENT) {
                                        reportActionToAnalytics("sendCodeSuccess");
                                        setRequestCodeView();
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

    public void verifyCode(View view) {
        progressBar.setVisibility(View.VISIBLE);
        Uri requestUrl = AccountStateReporter.getBaseUri(this, "verifyMobileNo")
                .appendQueryParameter("mobile_no", phoneNoView.getText().toString())
                .appendQueryParameter("verification_code", codeView.getText().toString())
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
        account.removePhoneNumber();
        setRequestMobileNoView();
    }

    private void updateView() {
        Pair<String, Boolean> accountPhoneStatus = account.getPhoneNumber();
        if (accountPhoneStatus.first == null) {
            setRequestMobileNoView();
        } else {
            phoneNoView.setText(accountPhoneStatus.first);
            if (accountPhoneStatus.second) {
                setVerifiedMobileNoView();
            } else {
                setRequestCodeView();
            }
        }
    }

    private void setPhoneNumberInStringResource(int viewId, int stringResourceId,
                                                String phoneNumber) {
        String codeLabelString = String.format(
                getResources().getString(stringResourceId), phoneNumber);
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
        // In case of OnBoarding, we show skip button. We also change the message for phone
        // registration based on the referrer used to install the app.
        findViewById(R.id.skip).setVisibility(inOnboardingFlow ? View.VISIBLE : View.GONE);

        phoneNoParent.setVisibility(View.VISIBLE);
        codeParent.setVisibility(View.GONE);
        verifiedParent.setVisibility(View.GONE);
    }

    // Set the UI elements when we need to ask for verification code.
    private void setRequestCodeView() {
        if (inOnboardingFlow) {
            skip(null);
            return;
        }
        setPhoneNumberInStringResource(R.id.code_label, R.string.ui_code,
                phoneNoView.getText().toString());
        phoneNoParent.setVisibility(View.GONE);
        codeParent.setVisibility(View.VISIBLE);
        verifiedParent.setVisibility(View.GONE);
    }

    // Set the UI elements when user mobile no is verified.
    private void setVerifiedMobileNoView() {
        if (inOnboardingFlow) {
            skip(null);
            return;
        }
        setPhoneNumberInStringResource(R.id.verified, R.string.ui_code_verified,
                phoneNoView.getText().toString());
        phoneNoParent.setVisibility(View.GONE);
        codeParent.setVisibility(View.GONE);
        verifiedParent.setVisibility(View.VISIBLE);
    }

    public void skip(View view) {
        if (view != null) {
            reportActionToAnalytics("skipPhoneLogin");
        }
        finish();
    }
}
