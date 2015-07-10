package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

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
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.ui.ContactsAdapter;
import com.eventshigh.nearme.app.ui.EventsAdapter;
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
    private boolean pushPlanId = false;

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
            pushPlanId = true;
        }

        setContentView(R.layout.activity_plan);
        topProgressBar = findViewById(R.id.top_progress_bar);
        retryView = findViewById(R.id.view_retry);
        inviteView = findViewById(R.id.invite_screen);

        // Setup the events adapter to show data.
        AutofitRecyclerView gridView = (AutofitRecyclerView) findViewById(R.id.grid);
        contactsAdapter = new ContactsAdapter(this, true);
        gridView.setAdapter(contactsAdapter);

        // Show Event Card.
        EventsAdapter.getEventCard(event, this, (FrameLayout) findViewById(R.id.event_container));
        onRetry(null);
    }

    public void onRetry(View view) {
        topProgressBar.setVisibility(View.VISIBLE);
        retryView.setVisibility(View.GONE);
        inviteView.setVisibility(View.GONE);

        VolleyHelper.getRequestQueue(this).cancelAll(this);
        MyContactsRequest.submit(this, Priority.IMMEDIATE, this, false,
                myContactsListener, errorListener);
    }

    public void invite(View view) {
        reportActionToAnalytics("invite");
        topProgressBar.setVisibility(View.VISIBLE);

        if (pushPlanId) {
            try {
                String url = Signer.sign(
                    AccountStateReporter.getBaseUri(this, "register_event_to_plan")
                            .appendQueryParameter("plan_id", planId)
                            .appendQueryParameter("event_id", event.id)
                            .build()
                ).toString();
                JsonObjectRequest request = new JsonObjectRequest(url, null, pushPlanIdListener, tryAgainErrorListener);
                request.setTag(this);
                VolleyHelper.addToRequestQueue(this, request);
            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                Crashlytics.getInstance().core.logException(e);
                topProgressBar.setVisibility(View.GONE);
                showMessage(R.string.failed_load);
            }
        } else {
            sendInvitations();
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
            contactsAdapter.setMyContacts(contacts);
        }
    };

    private Listener<JSONObject> pushPlanIdListener = new Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject res, boolean isIntermediate) {
            sendInvitations();
        }
    };

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

}
