package com.eventshigh.nearme.app.task;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.utils.ContactUtils;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.fabric.sdk.android.services.concurrency.AsyncTask;

/**
 * AsyncTask which can be used to load the user contacts.
 */
public class ContactsLoaderTask extends AsyncTask<Void, Void, Map<UserContact, List<String>>> {
    private static final String LOG_TAG = ContactsLoaderTask.class.getSimpleName();

    public interface ContactsCallback {
        void onContactLoad(@Nullable Map<UserContact, List<String>> contacts);
    }

    private final Context context;
    private final ContactsCallback callback;

    public ContactsLoaderTask(Context context, ContactsCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected @Nullable
    Map<UserContact, List<String>> doInBackground(Void... params) {
        Cursor cursor = ContactUtils.getContactsCursor(context, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        if (cursor == null) {
            return null;
        }

        Map<UserContact, List<String>> contactsToMobileNumbers = new TreeMap<>(new Comparator<UserContact>() {
            @Override
            public int compare(UserContact lhs, UserContact rhs) {
                return lhs.name.compareTo(rhs.name);
            }
        });
        try {
            while (cursor.moveToNext()) {
                try {
                    UserContact contact = UserContact.parseFromCursor(cursor);
                    if (!contactsToMobileNumbers.containsKey(contact)) {
                        contactsToMobileNumbers.put(contact, new ArrayList<String>());
                    }
                    contactsToMobileNumbers.get(contact).add(contact.mobileNo);
                } catch (JSONException e) {
                    Log.w(LOG_TAG, "failed to load contact", e);
                    Crashlytics.getInstance().core.logException(e);
                }
            }
        } finally {
            cursor.close();
        }

        return contactsToMobileNumbers;
    }

    @Override
    protected void onPostExecute(@Nullable Map<UserContact, List<String>> contactsToMobileNumbers) {
        callback.onContactLoad(contactsToMobileNumbers);
    }
}
