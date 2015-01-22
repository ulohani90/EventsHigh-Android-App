package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;

import java.text.MessageFormat;

public class ShareAppActivity extends BaseActivity {

    private Account account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_app);

        findViewById(R.id.share_app).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                shareApp();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        final TextView tv = (TextView) findViewById(R.id.share_app_message);
        tv.setText(R.string.loading);

        account = new Account(this);
        account.getNumReferrerInstalls(this,
                new Listener<Integer>() {
                    @Override
                    public void onResponse(Integer numInstalls) {
                        tv.setText(MessageFormat.format(getString(R.string.ui_referrer), numInstalls));
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Toast.makeText(ShareAppActivity.this, R.string.failed_load_num_installs, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    public void onStart() {
        super.onStart();

        // Show the Up button in the action bar.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        VolleyHelper.getRequestQueue(getApplicationContext()).cancelAll(this);
    }

    @Override
    public void shareApp() {
        reportActionToAnalytics("shareApp");
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(
                Intent.EXTRA_TEXT, String.format(
                        getResources().getString(R.string.share_app_text),
                        account.getUserReferrerCode())
        );
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }
}
