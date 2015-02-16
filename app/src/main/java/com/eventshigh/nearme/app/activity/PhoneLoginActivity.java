package com.eventshigh.nearme.app.activity;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
    };

    private Account account;
    private EditText phoneNoView;
    private EditText codeView;
    private TextView sendCodeButton;
    private TextView verifyCodeButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        phoneNoView = (EditText) findViewById(R.id.phone_no);
        codeView = (EditText) findViewById(R.id.code);
        sendCodeButton = (TextView) findViewById(R.id.send_code);
        verifyCodeButton = (TextView) findViewById(R.id.verify_code);

        account = new Account(this);
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

    public void sendCode(View view) {
        final String phoneNo = phoneNoView.getText().toString();
        Uri requestUrl =  AccountStateReporter.getBaseUri(this, "registerMobileNo")
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
                                        retry();
                                        return;
                                    }

                                    account.recordPhoneNumber(phoneNo);
                                    if (status == VerificationStatus.VERIFIED) {
                                        account.recordVerifiedPhoneNumber();
                                        setVerifiedMobileNoView();
                                    } else {
                                        setRequestCodeView();
                                    }
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                    retry();
                                }
                            }
                    )
            );
        } catch (IOException | GeneralSecurityException e) {
            retry();
        }
    }

    public void verifyCode(View view) {
        Uri requestUrl =  AccountStateReporter.getBaseUri(this, "verifyMobileNo")
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
                                        retry();
                                        return;
                                    }

                                    account.recordVerifiedPhoneNumber();
                                    setVerifiedMobileNoView();
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                    retry();
                                }
                            }
                    )
            );
        } catch (IOException | GeneralSecurityException e) {
            retry();
        }
    }

    private void retry() {
        Toast.makeText(this, "retry", Toast.LENGTH_SHORT).show();
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
        phoneNoView.setEnabled(true);
        codeView.setEnabled(false);
        sendCodeButton.setVisibility(View.VISIBLE);
        verifyCodeButton.setVisibility(View.GONE);
    }

    // Set the UI elements when we need to ask for verification code.
    private void setRequestCodeView() {
        phoneNoView.setEnabled(false);
        codeView.setEnabled(true);
        sendCodeButton.setVisibility(View.GONE);
        verifyCodeButton.setVisibility(View.VISIBLE);
    }

    // Set the UI elements when user mobile no is verified.
    private void setVerifiedMobileNoView() {
        phoneNoView.setEnabled(false);
        codeView.setEnabled(false);
        sendCodeButton.setVisibility(View.GONE);
        verifyCodeButton.setVisibility(View.GONE);
    }
}
