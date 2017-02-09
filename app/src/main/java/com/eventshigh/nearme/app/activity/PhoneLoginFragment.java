package com.eventshigh.nearme.app.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.Signer;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static com.eventshigh.nearme.app.activity.FbLoginFragment.LOGOUT_BROADCAST_ACTION;

/**
 * Created by umesh on 06/02/17.
 */

public class PhoneLoginFragment extends Fragment {
    private enum VerificationStatus {
        VERIFIED,
        CODE_SENT,
        RETRY
    }

    BaseActivity activity;

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
    private View parentView;
    private ScrollView parentScrollView;

    boolean hideSkip, isLogout, isCloseActivity;


    public static PhoneLoginFragment newInstance(boolean hideSkip, boolean isLogout, boolean isCloseActivity) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("hide_skip", hideSkip);
        bundle.putBoolean("is_logout", isLogout);
        bundle.putBoolean("is_close_activity", isCloseActivity);
        PhoneLoginFragment fragment = new PhoneLoginFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (BaseActivity) getActivity();
        hideSkip = getArguments().getBoolean("hide_skip", false);
        isLogout = getArguments().getBoolean("is_logout", false);
        isCloseActivity = getArguments().getBoolean("is_close_activity", false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        parentView = inflater.inflate(R.layout.activity_phone_login, container, false);

        return parentView;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        parentScrollView = (ScrollView) view.findViewById(R.id.parent_scrollview);
        phoneNoParent = view.findViewById(R.id.phone_no_parent);
        codeParent = view.findViewById(R.id.code_parent);
        verifiedParent = view.findViewById(R.id.verified_parent);

        nameView = (TextInputLayout) view.findViewById(R.id.name);
        phoneNoView = (TextInputLayout) view.findViewById(R.id.phone_no);
        codeView = (TextInputLayout) view.findViewById(R.id.code);
        progressBar = view.findViewById(R.id.top_progress_bar);
        view.findViewById(R.id.send_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCode(v);
            }
        });

        view.findViewById(R.id.change_number).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeNumber(v);
            }
        });
        view.findViewById(R.id.verify_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyCode(v);
            }
        });
        view.findViewById(R.id.change_number_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeNumber(v);
            }
        });


        nameView.setErrorEnabled(true);
        phoneNoView.setErrorEnabled(true);
        codeView.setErrorEnabled(true);

        nameEditText = nameView.getEditText();
        phoneNoEditText = phoneNoView.getEditText();
        codeEditText = codeView.getEditText();

        account = new Account(activity);
        updateView();
        /*parentScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = parentScrollView.getRootView().getHeight() - parentScrollView.getHeight();
                if (heightDiff > dpToPx(getActivity(), 200)) { // if more than 200 dp, it's probably a keyboard...
                  //  parentScrollView.fullScroll(View.FOCUS_DOWN);
                }
            }
        });*/

    }

    public static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }


    public TextInputLayout getCodeView() {
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
        if (phoneNo.length() < 10) {
            phoneNoEditText.requestFocus();
            phoneNoView.setError("Entered phone number is not correct");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        Uri requestUrl = UpdateAccountInfoService.getBaseUri(activity, "registerMobileNo")
                .appendQueryParameter("name", name)
                .appendQueryParameter("mobile_no", phoneNo)
                .build();
        try {
            VolleyHelper.addToRequestQueue(activity,
                    new JsonObjectRequest(Request.Method.GET, Signer.sign(requestUrl).toString(), null,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject s, boolean isIntermediate) {
                                    progressBar.setVisibility(View.GONE);
                                    VerificationStatus status = parseStatus(s.optString("status"));
                                    if (status == VerificationStatus.RETRY) {
                                        activity.reportActionToAnalytics("sendCodeRetry");
                                        showRetryMessage();
                                        return;
                                    }

                                    account.recordPhoneNumber(name, phoneNo);
                                    if (status == VerificationStatus.VERIFIED) {

                                        activity.reportActionToAnalytics("sendCodeVerified");
                                        account.recordVerifiedPhoneNumber();
                                        startNextTask();
                                    } else if (status == VerificationStatus.CODE_SENT) {
                                        activity.reportActionToAnalytics("sendCodeSuccess");
                                        startNextTask();
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                    progressBar.setVisibility(View.GONE);
                                    activity.reportActionToAnalytics("sendCodeRetry");
                                    VolleyHelper.log(activity, volleyError);
                                    showRetryMessage();
                                }
                            }
                    )
            );
        } catch (IOException | GeneralSecurityException e) {
            progressBar.setVisibility(View.GONE);
            activity.reportActionToAnalytics("sendCodeRetry");
            Crashlytics.getInstance().core.logException(e);
            showRetryMessage();
        }
    }

    public void startNextTask() {
        clearBackStackActivities();
        if (isLogout) {
            startLaunchActivity();
        }
        if (isCloseActivity) {
            activity.finish();
        }
    }

    public void startLaunchActivity() {
        Intent intent = new Intent(activity, LaunchActivity.class);
        startActivity(intent);
    }

    public void clearBackStackActivities() {
        Intent intent = new Intent();
        intent.setAction(LOGOUT_BROADCAST_ACTION);
        activity.sendBroadcast(intent);
    }

    public void startInterestActivity() {
        /*Intent intent = new Intent(this,SelectInterestsActivity.class);
        intent.putExtra(SelectInterestsActivity.ONBOARDING_FLOW,true);
        startActivity(intent);*/
        activity.finish();
    }

    public void verifyCode(View view) {
        progressBar.setVisibility(View.VISIBLE);
        Uri requestUrl = UpdateAccountInfoService.getBaseUri(activity, "verifyMobileNo")
                .appendQueryParameter("mobile_no", phoneNoEditText.getText().toString())
                .appendQueryParameter("verification_code", codeEditText.getText().toString())
                .build();
        try {
            VolleyHelper.addToRequestQueue(activity,
                    new JsonObjectRequest(Request.Method.GET, Signer.sign(requestUrl).toString(), null,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject s, boolean isIntermediate) {
                                    progressBar.setVisibility(View.GONE);
                                    VerificationStatus status = parseStatus(s.optString("status"));
                                    if (status != VerificationStatus.VERIFIED) {
                                        activity.reportActionToAnalytics("verifyCodeRetry");
                                        showRetryMessage();
                                        return;
                                    }

                                    activity.reportActionToAnalytics("verifyCodeSuccess");
                                    account.recordVerifiedPhoneNumber();
                                    setVerifiedMobileNoView();
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                    progressBar.setVisibility(View.GONE);
                                    activity.reportActionToAnalytics("verifyCodeRetry");
                                    VolleyHelper.log(activity, volleyError);
                                    showRetryMessage();
                                }
                            }
                    )
            );
        } catch (IOException | GeneralSecurityException e) {
            progressBar.setVisibility(View.GONE);
            activity.reportActionToAnalytics("verifyCodeRetry");
            Crashlytics.getInstance().core.logException(e);
            showRetryMessage();
        }
    }

    public void changeNumber(View view) {
        activity.reportActionToAnalytics("changeNumber");
        account.removeUserInfo();
        setRequestMobileNoView();
    }

    private void updateView() {
        Account.UserInfo userInfo = account.getUserInfo();
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
        TextView codeLabelView = (TextView) parentView.findViewById(viewId);
        codeLabelView.setText(codeLabelString);
        Linkify.addLinks(codeLabelView, Linkify.PHONE_NUMBERS);
    }

    private void showRetryMessage() {
        activity.showMessage(R.string.retry);
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
