package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UserContactsUploader implements Listener<JSONObject>, ErrorListener, Runnable {
    private static final String LOG_TAG = UserContactsUploader.class.getSimpleName();
    private static final int MAX_CONTACTS_TO_UPLOAD = 400;

    private static final String PARAM_LAST_CONTACTS_SYNC_TIMESTAMP = "last_contacts_sync_timestamp10";
    private static final String PARAM_LAST_CONTACTS_SYNC_TRY_TIMESTAMP = "last_contacts_sync_try_timestamp10";

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private long currentTimeMillis;
    private long lastSync;

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
        lastSync = sharedPreferences.getLong(PARAM_LAST_CONTACTS_SYNC_TIMESTAMP, 0);
        if (currentTimeMillis - lastSync < DateUtils.WEEK_IN_MILLIS) {
            return;
        }

        new Thread(this).start();
        sharedPreferences.edit().putLong(PARAM_LAST_CONTACTS_SYNC_TRY_TIMESTAMP, currentTimeMillis).apply();
    }

    @Override
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public void run() {
        // Build contact query.
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
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

        // Parse contacts data.
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, null, order);
        List<UserContact> contacts = new ArrayList<>();
        while(cursor.moveToNext()) {
            String contactId = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
            Cursor emailCursor = getEmailCursorForContactId(contactId);
            try {
                contacts.add(UserContact.parseFromCursor(cursor, emailCursor));
            } catch (JSONException e) {
                Log.w(LOG_TAG, "failed to load contact", e);
                Crashlytics.getInstance().core.logException(e);
            } finally {
			    emailCursor.close();
			}
        }
        cursor.close();

        // Upload contacts data.
        ContactsUploadRequest.submit(context, contacts, Priority.LOW, this, this);
    }

    private Cursor getEmailCursorForContactId(String contactId) {
        String[] projection = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
        };
        String selection = ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + contactId;
        return context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                projection, selection, null, null);
    }

    @Override
    public void onResponse(JSONObject jsonObject, boolean isIntermediate) {
        // TODO: replace currentTimeMillis with last contact timestamp.
        Log.i(LOG_TAG, "Successfully uploaded the contacts");
        sharedPreferences.edit().putLong(PARAM_LAST_CONTACTS_SYNC_TIMESTAMP, currentTimeMillis).apply();
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        Log.w(LOG_TAG, volleyError.getMessage(), volleyError.getCause());
        Crashlytics.getInstance().core.logException(volleyError.getCause());
    }
}
