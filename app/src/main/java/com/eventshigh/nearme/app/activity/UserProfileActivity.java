package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphRequestAsyncTask;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class UserProfileActivity extends AppCompatActivity implements View.OnClickListener{
    LoginButton loginButton;

    CallbackManager callbackManager;
    TextView tv1;
    Button btnFbLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_user_profile);
        tv1 = (TextView)findViewById(R.id.tv1);
        btnFbLogin = (Button)findViewById(R.id.btn_fb_login);
        btnFbLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_fb_login:
                fbLoginButtonPressed();
                break;
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
                Toast.makeText(UserProfileActivity.this, "", Toast.LENGTH_SHORT).show();
                Toast.makeText(getBaseContext(), "Login Cancelled", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(FacebookException e) {
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
                        tv1.setText(object.toString());
                        Log.e("obj ",object.toString());
                     }
            });
            Bundle parameters = new Bundle();
            parameters.putString("fields", "name, email, link"); // Parámetros que pedimos a facebook
            request.setParameters(parameters);
            request.executeAsync();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


}