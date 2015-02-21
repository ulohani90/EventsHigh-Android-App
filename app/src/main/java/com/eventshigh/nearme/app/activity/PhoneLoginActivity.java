package com.eventshigh.nearme.app.activity;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.util.Linkify;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request.Method;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.AccountStateReporter;

import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * This screen allows user to register and verify his phone number.
 */
public class PhoneLoginActivity extends BaseActivity {
    private static enum VerificationStatus {
        VERIFIED,
        CODE_SENT,
        RETRY
    }

    private View phoneNoParent;
    private View codeParent;
    private View verifiedParent;

    private Account account;
    private EditText phoneNoView;
    private EditText codeView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        phoneNoParent = findViewById(R.id.phone_no_parent);
        codeParent = findViewById(R.id.code_parent);
        verifiedParent = findViewById(R.id.verified_parent);

        phoneNoView = (EditText) findViewById(R.id.phone_no);
        codeView = (EditText) findViewById(R.id.code);

        account = new Account(this);
        updateView();
    }

    private void updateView() {
        Pair<String, Boolean> accountPhoneStatus = account.getPhoneNumber();
        if (accountPhoneStatus.first == null) {
            setRequestMobileNoView();
        } else {
            phoneNoView.setText(accountPhoneStatus.first);
            if (account.isRetryingPhoneVerification()) {
                setRequestMobileNoView();
            } else if (accountPhoneStatus.second) {
                setVerifiedMobileNoView();
            } else {
                setRequestCodeView();
            }
        }
    }

    private void setPhoneNumerInStringResource(int viewId, int stringResourceId,
                                               String phoneNumber) {
        String codeLabelString = String.format(
                getResources().getString(stringResourceId), phoneNumber);
        TextView codeLabelView = (TextView) findViewById(viewId);
        codeLabelView.setText(codeLabelString);
        Linkify.addLinks(codeLabelView, Linkify.PHONE_NUMBERS);
    }

    public void sendCode(View view) {
        final String phoneNo = phoneNoView.getText().toString();
        Uri requestUrl = AccountStateReporter.getBaseUri(this, "registerMobileNo")
                .appendQueryParameter("mobile_no", phoneNo)
                .build();
        try {
            VolleyHelper.addToRequestQueue(this,
                    new JsonObjectRequest(Method.GET, Signer.sign(requestUrl).toString(), null,
                            new Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject s, boolean isIntermediate) {
                                    VerificationStatus status = parseStatus(s.optString("status"));
                                    if (status == VerificationStatus.RETRY) {
                                        showRetryMessage();
                                        return;
                                    }

                                    account.recordPhoneNumber(phoneNo);
                                    if (status == VerificationStatus.VERIFIED) {
                                        account.recordVerifiedPhoneNumber();
                                        setVerifiedMobileNoView();
                                    } else if (status == VerificationStatus.CODE_SENT) {
                                        setRequestCodeView();
                                    }
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                    showRetryMessage();
                                }
                            }
                    )
            );
        } catch (IOException | GeneralSecurityException e) {
            showRetryMessage();
        }
    }

    public void verifyCode(View view) {
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
                                    VerificationStatus status = parseStatus(s.optString("status"));
                                    if (status != VerificationStatus.VERIFIED) {
                                        showRetryMessage();
                                        return;
                                    }

                                    account.recordVerifiedPhoneNumber();
                                    setVerifiedMobileNoView();
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                    showRetryMessage();
                                }
                            }
                    )
            );
        } catch (IOException | GeneralSecurityException e) {
            showRetryMessage();
        }
    }

    public void retryPhoneVerification(View view) {
        account.retryPhoneVerification();
        updateView();
    }

    private void showRetryMessage() {
        Toast.makeText(this, R.string.retry, Toast.LENGTH_SHORT).show();
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


    // Set the UI elements when we need to ask for mobile no.
    private void setRequestMobileNoView() {
        phoneNoParent.setVisibility(View.VISIBLE);
        codeParent.setVisibility(View.GONE);
        verifiedParent.setVisibility(View.GONE);
    }

    // Set the UI elements when we need to ask for verification code.
    private void setRequestCodeView() {
        setPhoneNumerInStringResource(R.id.code_label, R.string.ui_code,
                phoneNoView.getText().toString());
        phoneNoParent.setVisibility(View.GONE);
        codeParent.setVisibility(View.VISIBLE);
        verifiedParent.setVisibility(View.GONE);
    }

    // Set the UI elements when user mobile no is verified.
    private void setVerifiedMobileNoView() {
        setPhoneNumerInStringResource(R.id.verified, R.string.ui_code_verified,
                phoneNoView.getText().toString());
        phoneNoParent.setVisibility(View.GONE);
        codeParent.setVisibility(View.GONE);
        verifiedParent.setVisibility(View.VISIBLE);
    }
}
