package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.network.MyContactsRequest;
import com.eventshigh.nearme.app.network.URLShortenerRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.ui.ContactsAdapter;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Account.UserInfo;
import com.eventshigh.nearme.app.user.AccountStateReporter;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.List;

public class PlanActivity extends BaseActivity {

    private Event event;
    private String planId;
    private UserInfo userInfo;
    private boolean isPlanPublished = false;

    private View topProgressBar;
    private View retryView;
    private View inviteView;

    private ContactsAdapter contactsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userInfo = new Account(this).getUserInfo();

        event = getIntent().getParcelableExtra(EventDetailActivity.EXTRA_EVENT_PARAM);
        if (event == null) {
            finish();
            return;
        }

        planId = getIntent().getParcelableExtra(EventDetailActivity.EXTRA_PLAN_ID_PARAM);
        if (planId == null) {
            planId = Utils.md5(userInfo.phoneNo + event.id);
            isPlanPublished = false;
        }

        setContentView(R.layout.activity_plan);
        topProgressBar = findViewById(R.id.top_progress_bar);
        retryView = findViewById(R.id.view_retry);
        inviteView = findViewById(R.id.invite_screen);

        // Setup the events adapter to show data.
        AutofitRecyclerView gridView = (AutofitRecyclerView) findViewById(R.id.grid);
        contactsAdapter = new ContactsAdapter(this);
        gridView.setAdapter(contactsAdapter);

        onRetry(null);
    }

    public void onRetry(View view) {
        reportActionToAnalytics("retry", planId);

        topProgressBar.setVisibility(View.VISIBLE);
        retryView.setVisibility(View.GONE);
        inviteView.setVisibility(View.GONE);

        VolleyHelper.getRequestQueue(this).cancelAll(this);
        MyContactsRequest.submit(this, Priority.IMMEDIATE, this, false,
                myContactsListener, errorListener);
    }

    public void invite(View view) {
        reportActionToAnalytics("invite", planId, contactsAdapter.getSelectedContacts().size());
        topProgressBar.setVisibility(View.VISIBLE);

        publishPlan(new Runnable() {
            @Override
            public void run() {
                sendInvitations();
            }
        });
    }

    public void shareApp() {
        reportActionToAnalytics("shareApp", "inviteToPlan");
        topProgressBar.setVisibility(View.VISIBLE);

        publishPlan(new Runnable() {
            @Override
            public void run() {
                shareAppWithPlan();
            }
        });
    }

    private void shareAppWithPlan() {
        final String appShareUri =  Uri.parse("https://play.google.com/store/apps/details").buildUpon()
                .appendQueryParameter("id", getPackageName())
                .appendQueryParameter("referrer", "plan_" + planId)
                .build().toString();
        URLShortenerRequest.submit(this, appShareUri,
                new Listener<String>() {
                    @Override
                    public void onResponse(String shortenUri, boolean isIntermediate) {
                        shareApp(shortenUri);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        shareApp(appShareUri);
                    }
                }
        );
    }

    private void shareApp(String appDownloadLink) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                String.format(getString(R.string.invite_to_plan), userInfo.name, event.title,
                        event.getShortAddress(), appDownloadLink));

        shareIntent.setType("text/plain");
        shareIntent.setPackage(PACKAGE_NAME_WHATSAPP);
        startActivity(shareIntent);
        topProgressBar.setVisibility(View.GONE);
    }

    private void publishPlan(Runnable callback) {
        if (!isPlanPublished) {
            try {
                String url = Signer.sign(
                    AccountStateReporter.getBaseUri(this, "register_event_to_plan")
                            .appendQueryParameter("plan_id", planId)
                            .appendQueryParameter("event_id", event.id)
                            .appendQueryParameter("expiry_timestamp", Long.toString(max(event.eventTimings)))
                            .build()
                ).toString();
                JsonObjectRequest request = new JsonObjectRequest(url, null,
                        new PublishPlanIdListener(callback), tryAgainErrorListener);
                request.setTag(this);
                VolleyHelper.addToRequestQueue(this, request);
            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                Crashlytics.getInstance().core.logException(e);
                topProgressBar.setVisibility(View.GONE);
                showMessage(R.string.failed_load);
            }
        } else {
            callback.run();
        }
    }

    private void sendInvitations() {
        try {
            JSONArray invitations = new JSONArray();
            for (UserContact contact : contactsAdapter.getSelectedContacts()) {
                JSONObject invitation = new JSONObject();
                invitation.put("name", contact.name);
                invitation.put("mobile_no", contact.mobileNo);
                invitations.put(invitation);
            }

            JSONObject req = new JSONObject();
            req.put("plan_id", planId);
            req.put("mobile_no", userInfo.phoneNo);
            req.put("name", userInfo.name);
            req.put("invitations", invitations);

            String url = Signer.sign(
                    AccountStateReporter.getBaseUri(this, "invite_to_plan").build()).toString();
            JsonObjectRequest request = new JsonObjectRequest(url, req, invitationResponseListener,
                    tryAgainErrorListener);
            request.setTag(this);
            VolleyHelper.addToRequestQueue(this, request);
        } catch (JSONException | GeneralSecurityException | UnsupportedEncodingException e) {
            Crashlytics.getInstance().core.logException(e);
            topProgressBar.setVisibility(View.GONE);
            showMessage(R.string.failed_load);
        }
    }

    private Listener<List<UserContact>> myContactsListener = new Listener<List<UserContact>>() {
        @Override
        public void onResponse(List<UserContact> contacts, boolean isIntermediate) {
            if (isIntermediate) {
                return;
            }

            topProgressBar.setVisibility(View.GONE);
            inviteView.setVisibility(View.VISIBLE);
            contactsAdapter.setEventContacts(event, contacts);
        }
    };

    private class PublishPlanIdListener implements Listener<JSONObject> {
        private final Runnable publishPlanCallback;

        private PublishPlanIdListener(Runnable publishPlanCallback) {
            this.publishPlanCallback = publishPlanCallback;
        }

        @Override
        public void onResponse(JSONObject res, boolean isIntermediate) {
            isPlanPublished = true;
            publishPlanCallback.run();
        }
    }

    private Listener<JSONObject> invitationResponseListener = new Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject res, boolean isIntermediate) {
            showMessage("Invitation sent!");
            finish();
        }
    };

    private ErrorListener errorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            VolleyHelper.log(PlanActivity.this, volleyError);
            topProgressBar.setVisibility(View.GONE);
            retryView.setVisibility(View.VISIBLE);
        }
    };

    private ErrorListener tryAgainErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            VolleyHelper.log(PlanActivity.this, volleyError);
            topProgressBar.setVisibility(View.GONE);
            showMessage(R.string.failed_load);
        }
    };

    public long max(long[] arr) {
        return arr.length == 0 ? 0 : max(arr, 0, arr.length);
    }

    public long max(long[] arr, int start, int end) {
        return (start >= end - 1) ? arr[start]
                : Math.max(arr[start], max(arr, start + 1, end));
    }
}
