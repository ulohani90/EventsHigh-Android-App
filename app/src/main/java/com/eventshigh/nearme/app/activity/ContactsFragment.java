package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.task.ContactsLoaderTask;
import com.eventshigh.nearme.app.task.ContactsLoaderTask.ContactsCallback;
import com.eventshigh.nearme.app.ui.ContactsAdapter;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;

import java.util.List;

public class ContactsFragment extends Fragment {
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

        new ContactsLoaderTask(activity, new ContactsCallback() {
            @Override
            public void onContactLoad(@Nullable List<UserContact> contacts) {
                if (isAdded() && contacts != null) {
                    contactsAdapter.setContacts(contacts);
                }
            }
        }).execute();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        gridView.addOnScrollListener(new HideActionBarOnScroll(activity));
    }
}
