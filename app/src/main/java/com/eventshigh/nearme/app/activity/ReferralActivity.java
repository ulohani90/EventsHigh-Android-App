package com.eventshigh.nearme.app.activity;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.FeaturedEventsRequest;
import com.eventshigh.nearme.app.network.FeaturedEventsRequest.EventCollection;
import com.eventshigh.nearme.app.network.MyDiscountVouchersRequest;
import com.eventshigh.nearme.app.network.MyDiscountVouchersRequest.DiscountCode;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.IntentUtils;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ReferralActivity extends BaseActivity {

    public static final String FROM_NOTIFICATION_PARAM = "is_from_notification";
    private LinearLayout vouchersContainer;
    boolean isFromNotification;
    Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_referral);

        vouchersContainer = (LinearLayout) findViewById(R.id.my_vouchers_container);

        isFromNotification  = getIntent().getBooleanExtra(FROM_NOTIFICATION_PARAM,false);
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(Color.TRANSPARENT);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        ((TextView)(findViewById(R.id.terms_text))).setText(Html.fromHtml(getResources().getString(R.string.terms_condition_text)));

    }

    public View getViewForSnackbar() {
        return vouchersContainer;
    }

    @Override
    protected void onPause() {
        // Stop all requests associated with this activity.
        VolleyHelper.getRequestQueue(this).cancelAll(this);

        super.onPause();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (BaseActivity.NOTIFICATION_ACTION.equals(BaseActivity.NOTIFICATION_ACTION)) {
            reportActionToAnalytics("openNotification");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Account account = new Account(this);
        City city = account.getLastCity();
        if (city == null) {
            city = City.BANGALORE;
        }
        EventsContext eventsContext = new EventsContext(city, "");
        FeaturedEventsRequest.submit(this, eventsContext, Priority.IMMEDIATE, this,
                false, new Listener<EventCollection>() {
                    @Override
                    public void onResponse(EventCollection eventCollection, boolean isIntermediate) {
                        if (!eventCollection.showReferrer) {
                            Toast.makeText(ReferralActivity.this, "Sorry, referral contest has ended!",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        VolleyHelper.log(ReferralActivity.this, volleyError);
                    }
                });

        MyDiscountVouchersRequest.submit(this, Priority.HIGH, this, false,
                new Listener<List<DiscountCode>>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onResponse(List<DiscountCode> discountCodes, boolean isIntermediate) {
                        if (discountCodes.isEmpty()) {
                            vouchersContainer.setVisibility(View.GONE);
                            return;
                        }

                        while (vouchersContainer.getChildCount() > 4) {
                            vouchersContainer.removeViewAt(4);
                        }

                        LayoutInflater inflater = getLayoutInflater();
                        for (final DiscountCode code : discountCodes) {
                            View view  = inflater.inflate(R.layout.card_discount_voucher, vouchersContainer, false);
                            view.findViewById(R.id.parent_layout).setTag(code);
                            (view.findViewById(R.id.parent_layout)).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    DiscountCode code = (DiscountCode)v.getTag();
                                    if (!code.isUsed) {
                                        copyTextToClipBoard("Coupon Code", code.code," coupon code copied to Clipboard");
                                    } else {
                                        copyTextToClipBoard("Coupon Code", code.code," coupon code has already been redeemed.");
                                    }
                                }
                            });
                           // View parent = vouchersContainer.getChildAt(vouchersContainer.getChildCount() - 1);
                            if (code.isUsed) {
                                //noinspection deprecation
                                ((TextView) view.findViewById(R.id.discount_validity)).setText("Already redeemed");
                                ((TextView) view.findViewById(R.id.discount_validity)).setTextColor(getResources().getColor(R.color.red_dark));
                            }else{
                                ((TextView) view.findViewById(R.id.discount_validity)).setTextColor(getResources().getColor(R.color.valid_text_grey));

                                EventTime validTillTime = DateTimeUtils.dateToEventTime(
                                        new Date(code.validTillTimestamp),
                                        TimeZone.getTimeZone(City.BANGALORE.timeZone));
                                ((TextView) view.findViewById(R.id.discount_validity)).setText(
                                        "Valid Till: " + validTillTime.day + ", " + validTillTime.date);
                            }


                            ((TextView) view.findViewById(R.id.discount_code)).setText(code.code);
                            ((TextView) view.findViewById(R.id.discount_value)).setText("₹ " + code.amount);

                            vouchersContainer.addView(view);
                        }
                        vouchersContainer.setVisibility(View.VISIBLE);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        VolleyHelper.log(ReferralActivity.this, volleyError);
                    }
                });
    }

    public void copyTextToClipBoard(String label, String text,String message){
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);

        showMessage(text+message);
    }

    public void invite(View view) {
        shareApp();
    }

    public void redeem(View view) {
        reportActionToAnalytics("redeem");
        EventsContext param = new EventsContext(null, "eventshigh specials");
        Intent intent = new Intent(this, EventsGridActivity.class)
                .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, param);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if(isFromNotification){
            Intent intent = new Intent(this,LaunchActivity.class);
            startActivity(intent);
        }
            super.onBackPressed();
    }
}
