package com.eventshigh.nearme.app.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.eventshigh.nearme.app.ui.adapter.ContactsAdapter;
import com.eventshigh.nearme.app.ui.adapter.ContactsAdapter.FriendCardType;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.user.UserContactsUploader;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.List;

/**
 * UI to show the user's friends. We read the user phone contacts and match it
 * against the user on EH to show friends list.
 */
public class ContactsFragment extends Fragment {
    private BaseActivity activity;

    private View topProgressBar;
    private View retryView;
    private View noFriendsOnEhView;
    private AutofitRecyclerView gridView;

    private ContactsAdapter contactsAdapter;

    private boolean hasAskForContactsDialogShown = false;

    private View uploadContact;

    private TextView uploadContactsBtn;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.activity = (BaseActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Setup the events adapter to show data.
        gridView = (AutofitRecyclerView) view.findViewById(R.id.grid);
        contactsAdapter = new ContactsAdapter(activity);
        gridView.setAdapter(contactsAdapter);

        // More views.
        topProgressBar = view.findViewById(R.id.top_progress_bar);
        noFriendsOnEhView = view.findViewById(R.id.view_no_friends_on_eh);
        retryView = view.findViewById(R.id.view_retry);
        View inviteButtonView = view.findViewById(R.id.invite_button);

        uploadContact = view.findViewById(R.id.view_upload_contacts);
        uploadContactsBtn = (TextView)view.findViewById(R.id.upload_contacts_btn);

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

        view.findViewById(R.id.retry).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("retry");
                refresh(false);
            }
        });

        inviteButtonView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.shareApp();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        VolleyHelper.getRequestQueue(activity).cancelAll(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        refresh(false);
    }

    private void refresh(boolean shouldBypassCache) {

        VolleyHelper.getRequestQueue(activity).cancelAll(this);

        Preferences preferences = Preferences.getInstance(activity);
        if (preferences.canUploadContacts()) {
            topProgressBar.setVisibility(View.VISIBLE);
            retryView.setVisibility(View.GONE);
            noFriendsOnEhView.setVisibility(View.INVISIBLE);
            uploadContact.setVisibility(View.GONE);
            MyContactsRequest.submit(activity, Priority.IMMEDIATE, this, shouldBypassCache,
                myContactsListener, errorListener);
        } else if (!hasAskForContactsDialogShown) {
            topProgressBar.setVisibility(View.GONE);
            retryView.setVisibility(View.GONE);
            noFriendsOnEhView.setVisibility(View.INVISIBLE);
            hasAskForContactsDialogShown = true;
            uploadContact.setVisibility(View.VISIBLE);
            uploadContactsBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.reportActionToAnalytics("uploadContactsAccepted");
                    Preferences.getInstance(getActivity()).setCanUploadContacts(true);
                    new UserContactsUploader(activity).uploadContacts();

                }
            });
           // AskForContactsDialog.show(activity, preferences, contactsRequestCallback);
        }
    }

    private Listener<List<UserContact>> myContactsListener = new Listener<List<UserContact>>() {
        @Override
        public void onResponse(List<UserContact> contacts, boolean isIntermediate) {
            topProgressBar.setVisibility(isIntermediate ? View.VISIBLE : View.GONE);
            if (contacts.isEmpty()) {
                noFriendsOnEhView.setVisibility(View.VISIBLE);
            }
            contactsAdapter.setMyContacts(contacts, FriendCardType.FOLLOW);
        }
    };

    private ErrorListener errorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);
            if (gridView.getAdapter().getItemCount() > 0) {
                activity.showMessage(R.string.failed_refresh);
            } else {
                retryView.setVisibility(View.VISIBLE);
            }
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
