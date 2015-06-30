package com.eventshigh.nearme.app.task;

import android.content.Context;

import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.utils.ContactUtils;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import io.fabric.sdk.android.services.concurrency.AsyncTask;

/**
 * AsyncTask which can be used to load the user contacts.
 */
public class ContactsLoaderTask extends AsyncTask<Void, Void, List<UserContact>> {
    public interface ContactsCallback {
        void onContactLoad(List<UserContact> contacts);
    }

    private final Context context;
    private final ContactsCallback callback;

    public ContactsLoaderTask(Context context, ContactsCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    @SuppressWarnings("Null")
    protected List<UserContact> doInBackground(Void... params) {
        // Read the contacts and remove duplicates.
        HashSet<UserContact> contacts = new HashSet<>();
        contacts.addAll(ContactUtils.getContacts(context, null, null, false));
        List<UserContact> uniqueContacts = new ArrayList<>(contacts.size());
        uniqueContacts.addAll(contacts);

        // Remove the ones without any name.
        Iterator<UserContact> contactIterator = uniqueContacts.iterator();
        while ((contactIterator.hasNext())) {
            if (Utils.checkIfUnknown(contactIterator.next().name) == null) {
                contactIterator.remove();
            }
        }

        // Sort by Name.
        Collections.sort(uniqueContacts, new Comparator<UserContact>() {
            @Override
            public int compare(UserContact lhs, UserContact rhs) {
                if (lhs.name == null || rhs.name == null) {
                    return 0;
                }
                return lhs.name.compareTo(rhs.name);
            }
        });
        return uniqueContacts;
    }

    @Override
    protected void onPostExecute(List<UserContact> contacts) {
        callback.onContactLoad(contacts);
    }
}
