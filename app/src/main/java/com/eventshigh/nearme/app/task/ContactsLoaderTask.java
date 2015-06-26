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
import java.util.List;

import io.fabric.sdk.android.services.concurrency.AsyncTask;

/**
 * AsyncTask which can be used to load the user contacts.
 */
public class ContactsLoaderTask extends AsyncTask<Void, Void, List<UserContact>> {
    private static final String LOG_TAG = ContactsLoaderTask.class.getSimpleName();

    public interface ContactsCallback {
        void onContactLoad(@Nullable List<UserContact> contacts);
    }

    private final Context context;
    private final ContactsCallback callback;

    public ContactsLoaderTask(Context context, ContactsCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected @Nullable
    List<UserContact> doInBackground(Void... params) {
        Cursor cursor = ContactUtils.getContactsCursor(context, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        if (cursor == null) {
            return null;
        }

        List<UserContact> contacts = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                try {
                    contacts.add(UserContact.parseFromCursor(cursor));
                } catch (JSONException e) {
                    Log.w(LOG_TAG, "failed to load contact", e);
                    Crashlytics.getInstance().core.logException(e);
                }
            }
        } finally {
            cursor.close();
        }

        return contacts;
    }

    @Override
    protected void onPostExecute(@Nullable List<UserContact> contacts) {
        callback.onContactLoad(contacts);
    }
}
