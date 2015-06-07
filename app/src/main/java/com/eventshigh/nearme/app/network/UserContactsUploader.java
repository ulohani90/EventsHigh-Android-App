package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.text.format.DateUtils;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.UserContact;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UserContactsUploader implements Listener<JSONObject>, ErrorListener {
    private static final int MAX_CONTACTS_TO_UPLOAD = 200;

    private static final String PARAM_LAST_CONTACTS_SYNC_TIMESTAMP = "last_contacts_sync_timestamp";
    private static final String PARAM_LAST_CONTACTS_SYNC_TRY_TIMESTAMP = "last_contacts_sync_try_timestamp";

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private long currentTimeMillis;

    public UserContactsUploader(Context context) {
        this.context = context.getApplicationContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    public void uploadContacts() {
        // Don't do anything if we have tried in the last 1 day.
        final long lastTry = sharedPreferences.getLong(PARAM_LAST_CONTACTS_SYNC_TRY_TIMESTAMP, 0);
        currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastTry < DateUtils.DAY_IN_MILLIS) {
            return;
        }

        // Don't do anything if we have uploaded contacts in the last 7 day.
        final long lastSync = sharedPreferences.getLong(PARAM_LAST_CONTACTS_SYNC_TIMESTAMP, 0);
        if (currentTimeMillis - lastSync < DateUtils.WEEK_IN_MILLIS) {
            return;
        }

        new Thread() {
            public void run() {
                String[] projection = {
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        Email.DATA,
                };
                String selection;
                String order = null;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    // On android versions that have the contact updated timestamp, get only the
                    // contacts that have changed since the last sync
                    selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1 and "
                            + ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP
                            + " >= " + lastSync;
                    order = ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP +
                            " LIMIT " + MAX_CONTACTS_TO_UPLOAD;
                } else {
                    selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1";
                }

                Cursor cursor = context.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        projection, selection, null, order);
                List<UserContact> contacts = new ArrayList<>();
                while(cursor.moveToNext()) {
                    try {
                        contacts.add(UserContact.parseFromCursor(cursor));
                    } catch (JSONException e) {
                        Crashlytics.getInstance().core.logException(e);
                    }
                }
                cursor.close();

                ContactsUploadRequest.submit(context, contacts, Priority.LOW,
                        UserContactsUploader.this, UserContactsUploader.this);
            }
        }.start();
    }

    @Override
    public void onResponse(JSONObject jsonObject, boolean isIntermediate) {
        // TODO: replace currentTimeMillis with last contact timestamp.
        sharedPreferences.edit().putLong(PARAM_LAST_CONTACTS_SYNC_TIMESTAMP, currentTimeMillis).apply();
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        Crashlytics.getInstance().core.logException(volleyError.getCause());
    }
}
