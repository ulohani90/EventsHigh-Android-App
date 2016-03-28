package com.eventshigh.nearme.app.activity;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
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
                            inflater.inflate(R.layout.card_discount_voucher, vouchersContainer, true);

                            (findViewById(R.id.parent_layout)).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (!code.isUsed) {
                                        copyTextToClipBoard("Coupon Code", code.code);
                                    }
                                }
                            });
                            View parent = vouchersContainer.getChildAt(vouchersContainer.getChildCount() - 1);
                            if (code.isUsed) {
                                //noinspection deprecation
                                parent.setBackgroundColor(getResources().getColor(R.color.gray600));
                            }

                            EventTime validTillTime = DateTimeUtils.dateToEventTime(
                                    new Date(code.validTillTimestamp),
                                    TimeZone.getTimeZone(City.BANGALORE.timeZone));
                            ((TextView) parent.findViewById(R.id.discount_code)).setText(code.code);
                            ((TextView) parent.findViewById(R.id.discount_value)).setText("₹ " + code.amount);
                            ((TextView) parent.findViewById(R.id.discount_validity)).setText(
                                    "Valid Till: " + validTillTime.day + ", " + validTillTime.date);
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

    public void copyTextToClipBoard(String label, String text){
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);

        showMessage(text+" coupon code copied to Clipboard");
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
