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
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import static com.eventshigh.nearme.app.activity.FBLoginActivity.RC_SIGN_IN;

/**
 * Created by umesh on 02/08/16.
 */
public class FbLoginFragment extends Fragment implements GoogleApiClient.OnConnectionFailedListener {
    CallbackManager callbackManager;
    LoginResult loginResult;

    ProgressDialog dialog;

    boolean hideSkip;

    public static final String LOGOUT_BROADCAST_ACTION = "com.eventshigh.nearme.ACTION_LOGOUT";

    boolean showSpecialText;

    BaseActivity activity;

    boolean closeActivity;

    GoogleApiClient mGoogleApiClient;

    boolean isLogout;

    public static FbLoginFragment newInstance(boolean hideSkip, boolean showSpecialText, boolean isLogout, boolean closeActivity) {

        Bundle args = new Bundle();

        args.putBoolean("hide_skip", hideSkip);
        args.putBoolean("show_special_text", showSpecialText);
        args.putBoolean("close_activity", closeActivity);
        args.putBoolean("is_logout", isLogout);
        FbLoginFragment fragment = new FbLoginFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (BaseActivity) getActivity();
        hideSkip = getArguments().getBoolean("hide_skip", false);
        isLogout = getArguments().getBoolean("is_logout", false);
        showSpecialText = getArguments().getBoolean("show_special_text", false);
        closeActivity = getArguments().getBoolean("close_activity", false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(activity);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestServerAuthCode("708156551009-b2tv3ajql72j31kb5rlf8ahhi6n9olrt.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .enableAutoManage(getActivity() /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        // mGoogleApiClient.connect();
    }


    @Override
    public void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
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
                    openPhoneLoginActivity();
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

    public void openPhoneLoginActivity() {
        Intent intent = new Intent(activity, PhoneLoginActivity.class);
        intent.putExtra("hide_skip", true);
        intent.putExtra("is_logout", isLogout);
        activity.startActivity(intent);
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
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        } else if (callbackManager != null && data != null) {
            super.onActivityResult(requestCode, resultCode, data);
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    JSONObject responseObj;

    void requestUserProfile() {
        dialog = ProgressDialog.show(activity, null, "Signing in. Please wait...");
        System.out.println("onSuccess");
        final String accessToken = loginResult.getAccessToken().getToken();
        // Log.i("accessToken", accessToken);

        GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                Log.i("LoginActivity", response.toString());
                // Get facebook data from login
                if (object != null) {
                    try {
                        responseObj = new JSONObject();

                        if (object.has("id")) {
                            String userId = object.getString("id");
                            responseObj.put("fb_id", userId);
                            responseObj.put("fb_profile_pic", "https://graph.facebook.com/" + userId + "/picture?type=large");
                        } else {
                            dialog.dismiss();
                            AppAlertDialog.show("No facebook id found", getString(R.string.fb_sign_in_error_facebook_id_string), activity, new OnStartGoogleLoginListener() {
                                @Override
                                public void onStartGoogleLogin() {
                                    startGoogleLoginProcess();
                                }
                            });
                            return;
                        }
                        if (object.has("name")) {
                            String name = object.getString("name");
                            responseObj.put("fb_name", name);
                        } else {
                            dialog.dismiss();
                            AppAlertDialog.show("No name found", getString(R.string.fb_sign_in_error_name_string), activity, new OnStartGoogleLoginListener() {
                                @Override
                                public void onStartGoogleLogin() {
                                    startGoogleLoginProcess();
                                }
                            });
                            return;
                        }

                        responseObj.put("fb_token", accessToken);
                        responseObj.put("android_id", Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID));


                        if (object.has("email")) {
                            String email = object.getString("email");
                            responseObj.put("fb_email", email);
                        } else {
                            dialog.dismiss();
                            AppAlertDialog.show("No email found", getString(R.string.fb_sign_in_error_email_string), activity, new OnStartGoogleLoginListener() {
                                @Override
                                public void onStartGoogleLogin() {
                                    startGoogleLoginProcess();
                                }
                            });
                            return;
                        }


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


    public void startGoogleLoginProcess() {
        dialog = ProgressDialog.show(activity, null, "Checking google info. Please wait...");
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void handleSignInResult(GoogleSignInResult result) {
        dialog.dismiss();
        Log.d("Google sign in user", "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            try {

                responseObj.put("fb_email", acct.getEmail());
                dialog = ProgressDialog.show(activity, null, "Signing in. Please wait...");
                addFacebookUserInfo(responseObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            Toast.makeText(getActivity(), "Problem connecting to Google", Toast.LENGTH_SHORT).show();
        }
    }

    public void startLaunchActivity() {
        Intent intent = new Intent(getActivity(), LaunchActivity.class);
        startActivity(intent);
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
                            if (isLogout) {
                                openPhoneLoginActivity();
                            }
                            /*if (closeActivity)
                                activity.finish();
*/
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

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


    public interface OnStartGoogleLoginListener {
        void onStartGoogleLogin();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mGoogleApiClient.stopAutoManage(getActivity());
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /*mGoogleApiClient.stopAutoManage(getActivity());
        mGoogleApiClient.disconnect();*/
    }
}
