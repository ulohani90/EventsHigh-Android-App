package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.network.VolleyHelper;
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
