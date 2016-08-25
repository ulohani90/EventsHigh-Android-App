package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.network.AddFacebookUserInfoRequest;
import com.eventshigh.nearme.app.ui.AppAlertDialog;
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
 * Created by umesh on 02/08/16.
 */
public class FbLoginFragment extends Fragment {
    CallbackManager callbackManager;
    LoginResult loginResult;

    ProgressDialog dialog;

    boolean hideSkip;

    public static final String LOGOUT_BROADCAST_ACTION = "com.eventshigh.nearme.ACTION_LOGOUT";

    boolean showSpecialText;

    BaseActivity activity;

    boolean closeActivity;

    public static FbLoginFragment newInstance(boolean hideSkip, boolean showSpecialText, boolean closeActivity) {

        Bundle args = new Bundle();
        args.putBoolean("hide_skip", hideSkip);
        args.putBoolean("show_special_text", showSpecialText);
        args.putBoolean("close_activity", closeActivity);
        FbLoginFragment fragment = new FbLoginFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (BaseActivity) getActivity();
        hideSkip = getArguments().getBoolean("hide_skip", false);
        showSpecialText = getArguments().getBoolean("show_special_text", false);
        closeActivity = getArguments().getBoolean("close_activity", false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(activity);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_fb_login_layout, container, false);


        view.findViewById(R.id.fb_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fbLoginButtonPressed();
            }
        });

        TextView fbLoginText = (TextView) view.findViewById(R.id.fb_login_text);

        TextView skip = (TextView) view.findViewById(R.id.skip_login);
        if (hideSkip) {
            skip.setVisibility(View.GONE);

        } else {
            skip.setVisibility(View.VISIBLE);
            skip.setText("SKIP");
            skip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.reportActionToAnalytics("skipClicked");
                    clearBackStackActivities();
                    activity.finish();
                }
            });

        }

        if (showSpecialText) {
            fbLoginText.setText("Please login to continue the action");
        } else {
            fbLoginText.setText("Create your profile and \nsee what's trending with your friends");
        }

        return view;
    }

    public void clearBackStackActivities() {
        Intent intent = new Intent();
        intent.setAction(LOGOUT_BROADCAST_ACTION);
        activity.sendBroadcast(intent);
    }

    void fbLoginButtonPressed() {
        activity.reportActionToAnalytics("fbLoginBtnClicked");

        dialog = ProgressDialog.show(activity, null, "Fetching info. Please wait...");
        LoginManager.getInstance().logOut();
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email", "user_friends"));
        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginRes) {
                dialog.dismiss();
                loginResult = loginRes;
                requestUserProfile();
            }

            @Override
            public void onCancel() {
                dialog.dismiss();
                Toast.makeText(activity, "Login Cancelled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException e) {
                dialog.dismiss();
                Log.e("Problem conn fb", e.toString());
                Toast.makeText(activity, "Problem connecting to Facebook", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (callbackManager != null && data != null) {
            super.onActivityResult(requestCode, resultCode, data);
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    void requestUserProfile() {
        dialog = ProgressDialog.show(activity, null, "Signing in. Please wait...");
        System.out.println("onSuccess");
        final String accessToken = loginResult.getAccessToken().getToken();
        Log.i("accessToken", accessToken);

        GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                Log.i("LoginActivity", response.toString());
                // Get facebook data from login
                if (object != null) {
                    try {
                        JSONObject responseObj = new JSONObject();

                        if (object.has("id")) {
                            String userId = object.getString("id");
                            responseObj.put("fb_id", userId);
                            responseObj.put("fb_profile_pic", "https://graph.facebook.com/" + userId + "/picture?type=large");
                        } else {
                            dialog.dismiss();
                            AppAlertDialog.show("Problem connecting to facebook", "No facebook Id associated with this facebook account.", activity);
                            return;
                        }

                        if (object.has("email")) {
                            String email = object.getString("email");
                            responseObj.put("fb_email", email);
                        } else {
                            dialog.dismiss();
                            AppAlertDialog.show("Problem connecting to facebook", "No email associated with this facebook account.", activity);
                            return;
                        }

                        if (object.has("name")) {
                            String name = object.getString("name");
                            responseObj.put("fb_name", name);
                        } else {
                            dialog.dismiss();
                            AppAlertDialog.show("Problem connecting to facebook", "No name associated with this facebook account.", activity);
                            return;
                        }

                        responseObj.put("fb_token", accessToken);
                        responseObj.put("android_id", Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID));

                        addFacebookUserInfo(responseObj);

                    } catch (JSONException e) {
                        dialog.dismiss();
                        Log.i("User Profile", "JSON Exception");
                    }
                    Log.e("obj ", object.toString());
                }
            }
        });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "name, email");//add fields to fetch in graph response
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void addFacebookUserInfo(final JSONObject object) {
        AddFacebookUserInfoRequest.submit(activity, object, Request.Priority.HIGH,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject, boolean b) {
                        //updateProfile();
                        dialog.dismiss();
                        try {
                            new Account(activity).recordEmailId(object.getString("fb_name"), object.getString("fb_profile_pic"), object.getString("fb_email"), true);
                            activity.setResult(Activity.RESULT_OK);
                            clearBackStackActivities();
                            if (closeActivity)
                                activity.finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            dialog.dismiss();
                            Toast.makeText(activity, "Some problem fetching info. Please try again", Toast.LENGTH_SHORT).show();
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        dialog.dismiss();
                        Toast.makeText(activity, "Some problem fetching info. Please try again", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
