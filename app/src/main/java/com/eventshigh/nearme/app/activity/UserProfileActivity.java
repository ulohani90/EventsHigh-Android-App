package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.network.MovieReviewSubmitRequest;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Arrays;

public class UserProfileActivity extends BaseContextActivity implements View.OnClickListener{
    LoginButton loginButton;
    CallbackManager callbackManager;
    TextView tv1;
    Button btnFbLogin;
    ImageView ivProfilePic;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_user_profile);
        /*
        tv1 = (TextView)findViewById(R.id.tv1);
        btnFbLogin = (Button)findViewById(R.id.btn_fb_login);
        btnFbLogin.setOnClickListener(this);
        ivProfilePic = (ImageView)findViewById(R.id.iv_profile_pic);
        */
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        }
    }

    void fbLoginButtonPressed(){
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"));
        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                requestUserProfile(loginResult);
            }
            @Override
            public void onCancel() {
                Toast.makeText(getBaseContext(), "Login Cancelled", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(FacebookException e){
                Log.e("Problem conn fb",e.toString());
                Toast.makeText(getBaseContext(), "Problem connecting to Facebook" , Toast.LENGTH_SHORT).show();
            }
        });

    }

    void requestUserProfile(LoginResult loginResult){
                System.out.println("onSuccess");
                String accessToken = loginResult.getAccessToken().getToken();
                Log.i("accessToken", accessToken);

                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        Log.i("LoginActivity", response.toString());
                        // Get facebook data from login
                        try {
                            String userID = object.getString("id");
                            object.remove("id");
                            object.put("profile_pic","https://graph.facebook.com/" + userID + "/picture?type=large");
                        }catch (JSONException e){
                            Log.e("User Profile","JSON Exception");
                        }
                        tv1.setText(object.toString());
                        Log.e("obj ",object.toString());
                     }
            });
            Bundle parameters = new Bundle();
            parameters.putString("fields", "name, email"); // Parámetros que pedimos a facebook
            request.setParameters(parameters);
            request.executeAsync();

    }

    public void placeReviewAction(final JSONObject data){
        MovieReviewSubmitRequest.submit(this,
                data, Request.Priority.HIGH, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject, boolean b) {
                        Log.i("Message Success", "true");
                        Toast.makeText(getApplicationContext(), "Your profile created successfully", Toast.LENGTH_SHORT);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.i("Message failure", "true" + data.toString());
                    }
                });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


}