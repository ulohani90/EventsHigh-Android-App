package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MultiAutoCompleteTextView;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.ui.ContactsAutoFillAdapter;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Account.UserInfo;
import com.eventshigh.nearme.app.user.AccountStateReporter;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;
import com.eventshigh.nearme.app.view.AutofitRecyclerView.SpanAllColumnLookup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

public class PlanActivity extends BaseActivity {
    private Event event;
    private String planId;
    private UserInfo userInfo;
    private boolean isPlanPublished = false;

    private View topProgressBar;
    private View inviteButtton;
    private View contactsInviteCard;
    private MultiAutoCompleteTextView contactsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Parse the incoming intent params.
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
        } else {
            isPlanPublished = true;
        }

        setContentView(R.layout.activity_plan);
        topProgressBar = findViewById(R.id.top_progress_bar);
        inviteButtton = findViewById(R.id.invite_button);
        ((AutofitRecyclerView) findViewById(R.id.contact_grid)).setAdapter(new PlanViewAdapter());
    }

    public void invite(View view) {
        reportActionToAnalytics("inviteToPlan", planId, 0 /*contactsAdapter.getSelectedFriends().size()*/);
        topProgressBar.setVisibility(View.VISIBLE);

        publishPlan(new Runnable() {
            @Override
            public void run() {
                sendInvitations();
            }
        });
    }

    public void whatsapp(View view) {
        shareEvent(event, PACKAGE_NAME_WHATSAPP);
    }

    public void facebook(View view) {
        shareEvent(event, PACKAGE_NAME_FACEBOOK);
    }

    public void twitter(View view) {
        shareEvent(event, PACKAGE_NAME_TWITTER);
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
            for (String friendData : contactsView.getText().toString().split(",")) {
                JSONObject invitation = new JSONObject();
                String[] nameMobile = friendData.split("\\(\\)");
                if (nameMobile.length == 3) {
                    invitation.put("name", nameMobile[0].trim());
                    invitation.put("mobile_no", nameMobile[1].trim());
                    invitations.put(invitation);
                }
            }

            JSONObject req = new JSONObject();
            req.put("plan_id", planId);
            req.put("mobile_no", userInfo.phoneNo);
            req.put("name", userInfo.name);
            req.put("invitations", invitations);
            Log.w("debug", req.toString(2));

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

    private ErrorListener tryAgainErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            VolleyHelper.log(PlanActivity.this, volleyError);
            topProgressBar.setVisibility(View.GONE);
            showMessage(R.string.failed_load);
        }
    };

    private class PlanViewAdapter extends RecyclerView.Adapter<ViewHolder> implements SpanAllColumnLookup {
        private static final int CARD_PLAN_HEADER = 1;
        private static final int CARD_PLAN_CONTACTS = 2;
        private static final int CARD_CONTACT = 3;

        private class PlanViewHolder extends ViewHolder {
            public PlanViewHolder(View itemView) {
                super(itemView);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return CARD_PLAN_HEADER;
            }
            if (position == 1) {
                return CARD_PLAN_CONTACTS;
            }
            return CARD_CONTACT;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == CARD_PLAN_HEADER) {
                return new PlanViewHolder(getLayoutInflater().inflate(R.layout.card_plan_header, parent, false));
            }
            if (viewType == CARD_PLAN_CONTACTS) {
                if (contactsInviteCard == null) {
                    contactsInviteCard = getLayoutInflater().inflate(R.layout.card_plan_contacts, parent, false);
                    contactsView = (MultiAutoCompleteTextView) contactsInviteCard.findViewById(R.id.contacts);
                    // Setup contacts selector.
                    contactsView.setAdapter(new ContactsAutoFillAdapter(PlanActivity.this));
                    contactsView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
                    contactsView.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            inviteButtton.setEnabled(s.toString().trim().endsWith(","));
                        }
                    });
                }

                return new PlanViewHolder(contactsInviteCard);
            }

            return new PlanViewHolder(getLayoutInflater().inflate(R.layout.card_plan_header, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (position > 1) {

            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        @Override
        public boolean spanAllColumns(int position) {
            return (position < 2);
        }
    }

    private static long max(long[] arr) {
        return arr.length == 0 ? 0 : max(arr, 0, arr.length);
    }

    private static long max(long[] arr, int start, int end) {
        return (start >= end - 1) ? arr[start]
                : Math.max(arr[start], max(arr, start + 1, end));
    }
}
