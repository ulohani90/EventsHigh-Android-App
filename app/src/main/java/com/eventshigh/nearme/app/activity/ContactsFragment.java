package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.network.MyContactsRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.AskForContactsDialog;
import com.eventshigh.nearme.app.ui.AskForContactsDialog.ContactsRequestCallback;
import com.eventshigh.nearme.app.ui.ContactsAdapter;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.List;

/**
 * UI to show the user's friends. We read the user phone contacts and match it
 * against the user on EH to show friends list.
 */
public class ContactsFragment extends Fragment {
    private BaseActivity activity;
    private long lastRefreshTimestamp;

    private View topProgressBar;
    private View retryView;

    private ContactsAdapter contactsAdapter;

    private boolean hasAskForContactsDialogShown = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseActivity) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        VolleyHelper.getRequestQueue(activity).cancelAll(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (lastRefreshTimestamp < System.currentTimeMillis() - BaseEventsFragment.DEFAULT_REFRESH_INTERVAL) {
            refresh(false);
            lastRefreshTimestamp = System.currentTimeMillis();
        }
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        refresh(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Setup the events adapter to show data.
        AutofitRecyclerView gridView = (AutofitRecyclerView) view.findViewById(R.id.grid);
        contactsAdapter = new ContactsAdapter(activity);
        gridView.setAdapter(contactsAdapter);

        // More views.
        topProgressBar = view.findViewById(R.id.top_progress_bar);
        retryView = view.findViewById(R.id.view_retry);

        // Setup the refresh on swipe down.
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                activity.reportActionToAnalytics("swipeRefresh");
                swipeRefreshLayout.setRefreshing(false);
                refresh(true /* bypass cache*/);
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);

        retryView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("retry");
                refresh(false);
            }
        });
    }

    private void refresh(boolean shouldBypassCache) {
        topProgressBar.setVisibility(View.VISIBLE);
        retryView.setVisibility(View.GONE);
        VolleyHelper.getRequestQueue(activity).cancelAll(this);

        Preferences preferences = Preferences.getInstance(activity);
        if (preferences.shouldUploadContacts()) {
            MyContactsRequest.submit(activity, Priority.IMMEDIATE, this, shouldBypassCache,
                myContactsListener, errorListener);
        } else if (!hasAskForContactsDialogShown) {
            hasAskForContactsDialogShown = true;
            AskForContactsDialog.show(activity, preferences, contactsRequestCallback);
        }
    }

    private Listener<List<UserContact>> myContactsListener = new Listener<List<UserContact>>() {
        @Override
        public void onResponse(List<UserContact> contacts, boolean isIntermediate) {
            topProgressBar.setVisibility(isIntermediate ? View.VISIBLE : View.GONE);
            if (!isIntermediate && contacts.isEmpty()) {
                errorListener.onErrorResponse(new VolleyError("failed contacts"));
            } else {
                contactsAdapter.setMyContacts(contacts);
            }
        }
    };

    private ErrorListener errorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);
            retryView.setVisibility(View.VISIBLE);
            VolleyHelper.log(activity, volleyError);
        }
    };

    private ContactsRequestCallback contactsRequestCallback  = new ContactsRequestCallback() {
        @Override
        public void onContactsUploadAccepted() {
            MyContactsRequest.submit(activity, Priority.IMMEDIATE, this, false,
                    myContactsListener, errorListener);
        }

        @Override
        public void onContactsUploadRejected() {
            activity.finish();
        }
    };
}
