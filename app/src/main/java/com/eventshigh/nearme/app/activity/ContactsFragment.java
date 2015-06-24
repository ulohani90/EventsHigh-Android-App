package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.ui.ContactsAdapter;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.services.concurrency.AsyncTask;

public class ContactsFragment extends Fragment {
    private static final String LOG_TAG = ContactsFragment.class.getSimpleName();

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

        AsyncTask task = new AsyncTask<Void, Void, List<UserContact>>() {
            @Override
            protected List<UserContact> doInBackground(Void... voids) {
                return loadContacts();
            }

            @Override
            protected void onPostExecute(List<UserContact> contacts) {
                if (contacts != null) {
                    contactsAdapter.setContacts(contacts);
                }
            }
        };
        task.execute(new Void[]{});

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        gridView.setOnScrollListener(new HideActionBarOnScroll(activity));
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public List<UserContact> loadContacts() {
        // Build contact query.
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
        };
        String selection;
        String order = null;

        selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1";
        order = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME;

        // Parse contacts data.
        Cursor cursor = activity.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, null, order);
        if (cursor == null) {
            return null;
        }

        List<UserContact> contacts = new ArrayList<>();
        while (cursor.moveToNext()) {
            String contactId = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
            try {
                contacts.add(UserContact.parseFromCursor(cursor));
            } catch (JSONException e) {
                Log.w(LOG_TAG, "failed to load contact", e);
                Crashlytics.getInstance().core.logException(e);
            }
        }
        cursor.close();

        return contacts;
    }
}
