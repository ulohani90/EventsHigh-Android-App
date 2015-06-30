package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.task.ContactPhoneResolverTask;
import com.eventshigh.nearme.app.task.ContactsLoaderTask;
import com.eventshigh.nearme.app.ui.ContactsAdapter;
import com.eventshigh.nearme.app.user.AccountStateReporter;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * UI to show the user's friends. We read the user phone contacts and match it
 * against the user on EH to show friends list.
 */
public class ContactsFragment extends Fragment implements Response.Listener<JSONObject> {
    private BaseActivity activity;
    private ContactsAdapter contactsAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        AutofitRecyclerView gridView = (AutofitRecyclerView) view.findViewById(R.id.grid);

        contactsAdapter = new ContactsAdapter(activity);
        gridView.setAdapter(contactsAdapter);

        new ContactsLoaderTask(activity, new ContactsLoaderTask.ContactsCallback() {
            @Override
            public void onContactLoad(List<UserContact> contacts) {
                if (isAdded()) {
                    contactsAdapter.setContacts(contacts);
                }
            }
        }).execute();

        Uri requestUrl = AccountStateReporter.getBaseUri(activity, "get_social_friends").build();
        try {
            VolleyHelper.addToRequestQueue(activity, new JsonObjectRequest(
                    Request.Method.GET, Signer.sign(requestUrl).toString(), null, this, errorListener)
            );
        } catch (IOException | GeneralSecurityException e) {
            Crashlytics.getInstance().core.logException(e);
        }

        return view;
    }


    @Override
    public void onResponse(JSONObject jsonObject, boolean isIntermediate) {
        new ContactPhoneResolverTask(activity, new ContactPhoneResolverTask.Callback() {
            @Override
            public void onContacsResolved(@Nullable List<String> contactsOnEh) {
                if (!isAdded()) {
                    return;
                }
                contactsAdapter.setContactsOnEh(contactsOnEh);
            }
        }).execute(jsonObject);
    }

    private ErrorListener errorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            VolleyHelper.log(activity, volleyError);
        }
    };
}
