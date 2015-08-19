package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.format.DateUtils;
import android.util.Log;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.network.ContactsUploadRequest;
import com.eventshigh.nearme.app.utils.ContactUtils;

import org.json.JSONObject;

import java.util.List;

public class UserContactsUploader implements Listener<JSONObject>, ErrorListener, Runnable {
    private static final String LOG_TAG = UserContactsUploader.class.getSimpleName();
    private static final int MAX_CONTACTS_TO_UPLOAD = 1000;

    private static final String PARAM_LAST_CONTACTS_SYNC_TIMESTAMP = "last_contacts_sync_timestamp";
    private static final String PARAM_LAST_CONTACTS_SYNC_TRY_TIMESTAMP = "last_contacts_sync_try_timestamp";

    private final Context context;
    private final SharedPreferences preferences;
    private long currentTimeMillis;

    public UserContactsUploader(Context context) {
        this.context = context.getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    public void uploadContacts() {
        // Don't do anything if we have tried in the last 1 day.
        final long lastTry = preferences.getLong(PARAM_LAST_CONTACTS_SYNC_TRY_TIMESTAMP, 0);
        currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastTry < DateUtils.DAY_IN_MILLIS) {
            return;
        }

        new Thread(this).start();
        preferences.edit().putLong(PARAM_LAST_CONTACTS_SYNC_TRY_TIMESTAMP, currentTimeMillis).apply();
    }

    @Override
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public void run() {
        String selectionExtras = "";
        String order;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // On android versions that have the contact updated timestamp, get only the
            // contacts that have changed since the last sync
            selectionExtras += " and "
                    + ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP
                    + " >= " + preferences.getLong(PARAM_LAST_CONTACTS_SYNC_TIMESTAMP, 0);
            order = ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP;
        } else {
            order = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME;
        }
        order += " LIMIT " + MAX_CONTACTS_TO_UPLOAD;

        // Load the contacts data.
        List<UserContact> contacts = ContactUtils.getContacts(context, selectionExtras, order, true);
        if (contacts.isEmpty()) {
            return;
        }

        // Upload contacts data.
        ContactsUploadRequest.submit(context, contacts, Priority.LOW, this, this);
    }

    @Override
    public void onResponse(JSONObject jsonObject, boolean isIntermediate) {
        // TODO: replace currentTimeMillis with last contact timestamp.
        Log.i(LOG_TAG, "Successfully uploaded the contacts");
        preferences.edit().putLong(PARAM_LAST_CONTACTS_SYNC_TIMESTAMP, currentTimeMillis).apply();
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        Log.w(LOG_TAG, volleyError.getMessage(), volleyError.getCause());
        Crashlytics.getInstance().core.logException(volleyError.getCause());
    }
}
