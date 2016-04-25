package com.eventshigh.nearme.app.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.data.stream.OfferObject;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.VoucherSelectDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.Signer;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import pl.snowdog.material.ui.ToolbarColorizeHelper;

/**
 * Created by umesh on 14/04/16.
 */
public class RedeemCouponActivity extends BaseActivity {


    OfferObject obj;
    TextInputLayout coupon;
    TextInputLayout mobileNum;
    TextInputLayout fullName;
    TextInputLayout emailAdd;
    EditText couponEditText;
    EditText mobileEditText;
    EditText fullNameEditText;
    EditText emailAddEditText;
    TextView termsText;
    long totalPoints;
    View progressBar;
    ImageView offerBg;
    Account account;
    int selectedVoucherPos = -1;
    Toolbar toolbar;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redeem_coupon);
        progressBar = findViewById(R.id.top_progress_bar);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        account = new Account(this);

        if (getIntent() != null) {
            obj = getIntent().getParcelableExtra("offer");
            totalPoints = getIntent().getLongExtra("total_points", 0);
        }
        if (obj != null) {
            setUpData();
        }
    }

    private void setLightToolbarIcons() {
        toolbar.post(new Runnable() {
            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                ToolbarColorizeHelper.colorizeToolbar(toolbar,
                        getResources().getColor(android.R.color.white), RedeemCouponActivity.this);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_event_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        } else if (item.getItemId() == R.id.action_share) {
            shareCoupon(obj);
        }
        return super.onOptionsItemSelected(item);
    }


    public void setUpData() {
        offerBg = (ImageView) findViewById(R.id.offer_bg);
        Glide.with(this).load(obj.imgUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                .into(offerBg);
        ((TextView) findViewById(R.id.title)).setText(obj.name);
        ((TextView) findViewById(R.id.subtitle)).setText(obj.desc);
        coupon = (TextInputLayout) findViewById(R.id.coupon);
        mobileNum = (TextInputLayout) findViewById(R.id.mobile_no);
        fullName = (TextInputLayout) findViewById(R.id.fullname);
        emailAdd = (TextInputLayout) findViewById(R.id.email_id);
        couponEditText = coupon.getEditText();
        mobileEditText = mobileNum.getEditText();

        fullNameEditText = fullName.getEditText();
        emailAddEditText = emailAdd.getEditText();
        termsText = (TextView) findViewById(R.id.terms_text);
        if (account.getUserInfo().isVerified) {
            mobileEditText.setText(account.getUserInfo().phoneNo);
            fullNameEditText.setText(account.getUserInfo().name);
        }
        ((TextView) findViewById(R.id.confirm_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClicked();
            }
        });
        setFirstValidVoucher();


        couponEditText.setFocusable(false);
        couponEditText.setClickable(true);
        couponEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportActionToAnalytics("couponChangeClicked");
                VoucherSelectDialog.show(RedeemCouponActivity.this, obj.vouchers, selectedVoucherPos, new VoucherSelectDialog.VoucherSelectionCallback() {
                    @Override
                    public void onCouponSelected(int pos) {
                        if (obj.vouchers.get(pos).pointsReq <= totalPoints) {
                            selectedVoucherPos = pos;
                            couponEditText.setText(obj.vouchers.get(pos).voucherName);
                        } else {
                            showMessage("You don't have enough points to claim this coupon");
                        }
                    }
                });
            }
        });

        String[] terms = obj.termsConditions.split("\\.");
        if (terms.length > 0) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < terms.length; i++) {
                builder.append("\u2022 ");
                builder.append(terms[i]);
                builder.append("\n");

            }
            termsText.setText(builder.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (toolbar != null)
            setLightToolbarIcons();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

    }

    public void setFirstValidVoucher() {
        for (int i = 0; i < obj.vouchers.size(); i++) {
            if (obj.vouchers.get(i).pointsReq <= totalPoints) {
                selectedVoucherPos = i;
                couponEditText.setText(obj.vouchers.get(i).voucherName);
                break;
            }
        }

        couponEditText.setText("Select a voucher");


    }

    public void confirmClicked() {

        if (checkIfDetailsCorrect()) {
            progressDialog = ProgressDialog.show(this, null, "Confirming.Please wait..");
            reportActionToAnalytics("redeemButtonClick");

            Uri requestUrl = UpdateAccountInfoService.getBaseUri(this, "offer_redeem")
                    .appendQueryParameter("mobile_no", mobileEditText.getText().toString())
                    .appendQueryParameter("offer_id", obj.id + "")
                    .appendQueryParameter("name", fullNameEditText.getText().toString())
                    .appendQueryParameter("email", emailAddEditText.getText().toString())
                    .appendQueryParameter("voucher_name", obj.vouchers.get(selectedVoucherPos).voucherName)
                    .build();

            try {
                VolleyHelper.addToRequestQueue(this,
                        new JsonObjectRequest(Request.Method.GET, Signer.sign(requestUrl).toString(), null,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject s, boolean isIntermediate) {

                                        updatePreferencesForOffer();
                                        if (progressDialog != null) {
                                            progressDialog.dismiss();
                                        }
                                        progressBar.setVisibility(View.GONE);
                                        showMessage("You have successfully confirmed the coupon");
                                        finish();
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError volleyError) {
                                        if (progressDialog != null) {
                                            progressDialog.dismiss();
                                        }
                                        progressBar.setVisibility(View.GONE);
                                        VolleyHelper.log(RedeemCouponActivity.this, volleyError);
                                        showRetryMessage();
                                    }
                                }
                        )
                );
            } catch (UnsupportedEncodingException | GeneralSecurityException e) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                progressBar.setVisibility(View.GONE);
                Crashlytics.getInstance().core.logException(e);
                showRetryMessage();
            }

        }
    }

    public boolean checkIfDetailsCorrect() {
        if (fullNameEditText.getText() != null && fullNameEditText.getText().toString().length() > 0) {
            fullName.setErrorEnabled(false);
            if (mobileEditText.getText() != null && mobileEditText.getText().toString().length() == 10) {
                mobileNum.setErrorEnabled(false);

                if (emailAddEditText.getText() != null && Utils.isValidEmail(emailAddEditText.getText())) {
                    emailAdd.setErrorEnabled(false);
                    if (selectedVoucherPos != -1) {
                        coupon.setErrorEnabled(false);
                        return true;
                    } else {
                        coupon.setErrorEnabled(true);
                        coupon.setError("No coupon selected.");
                        return false;
                    }


                } else {
                    emailAdd.setErrorEnabled(true);
                    emailAdd.setError("Valid Email Address required");
                    return false;
                }

            } else {
                mobileNum.setErrorEnabled(true);
                mobileNum.setError("Valid Mobile number required");
                return false;
            }
        } else {
            fullName.setErrorEnabled(true);
            fullName.setError("Full Name required");
            return false;
        }
    }


    private void showRetryMessage() {
        showMessage(R.string.retry);
    }


    @Override
    public void onBackPressed() {
        this.finish();
        //   overridePendingTransition(R.anim.stay,R.anim.animate_up_bottom);
    }

    public void updatePreferencesForOffer() {

        Preferences preferences = Preferences.getInstance(this);

        StringBuilder builder = new StringBuilder();
        builder.append(preferences.getPrefOfferActedId());
        if (builder.length() > 0) {
            builder.append(",");
        }
        builder.append(obj.id + "");
        preferences.setPrefOfferActedId(builder.toString());

    }
}
