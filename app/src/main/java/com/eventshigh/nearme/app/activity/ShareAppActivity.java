package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.user.Account;

import java.text.MessageFormat;

public class ShareAppActivity extends BaseActivity {

    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_app);

        tv = (TextView) findViewById(R.id.share_app_message);
        tv.setText(R.string.ui_referrer_default);

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

        Account account = new Account(this);
        account.getNumReferrerInstalls(this,
                new Listener<Integer>() {
                    @Override
                    public void onResponse(Integer numInstalls, boolean isIntermediate) {
                        if (numInstalls > 0) {
                            tv.setText(MessageFormat.format(getString(R.string.ui_referrer), numInstalls));
                        }
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        // do nothing.
                    }
                }
        );
    }
}
