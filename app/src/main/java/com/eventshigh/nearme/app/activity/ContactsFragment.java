package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.task.ContactPhoneResolverTask;
import com.eventshigh.nearme.app.task.ContactsLoaderTask;
import com.eventshigh.nearme.app.ui.ContactsAdapter;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.user.AccountStateReporter;

import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public class ContactsFragment extends Fragment implements Response.Listener<JSONObject>, Response.ErrorListener {
    private BaseContextActivity activity;
    private RecyclerView gridView;
    private ContactsAdapter contactsAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseContextActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        gridView = (RecyclerView) view.findViewById(R.id.grid);

        contactsAdapter = new ContactsAdapter(activity);
        gridView.setAdapter(contactsAdapter);

        new ContactsLoaderTask(activity, new ContactsLoaderTask.ContactsCallback() {
            @Override
            public void onContactLoad(@Nullable List<UserContact> contacts) {
                if (isAdded() && contacts != null) {
                    contactsAdapter.setContacts(contacts);
                }
            }
        }).execute();

        Uri requestUrl = AccountStateReporter.getBaseUri(activity, "get_social_friends").build();
        try {
            VolleyHelper.addToRequestQueue(activity, new JsonObjectRequest(
                    Request.Method.GET, Signer.sign(requestUrl).toString(), null, this, this)
            );
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        gridView.addOnScrollListener(new HideActionBarOnScroll(activity));
    }

    @Override
    public void onResponse(JSONObject jsonObject, boolean b) {
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

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        volleyError.printStackTrace();
    }
}
