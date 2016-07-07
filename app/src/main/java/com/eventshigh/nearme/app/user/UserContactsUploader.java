package com.eventshigh.nearme.app.user;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.util.Log;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.network.ContactsUploadRequest;
import com.eventshigh.nearme.app.ui.AskForContactsDialog;
import com.eventshigh.nearme.app.utils.ContactUtils;

import org.json.JSONObject;

import java.util.List;

public class UserContactsUploader {
    private static final String LOG_TAG = UserContactsUploader.class.getSimpleName();
    private static final int MAX_CONTACTS_TO_UPLOAD = 1000;

    private static final String PARAM_LAST_CONTACTS_SYNC_TIMESTAMP = "last_contacts_sync_timestamp";
    private static final String PARAM_LAST_CONTACTS_SYNC_TRY_TIMESTAMP = "last_contacts_sync_try_timestamp";

    private final BaseActivity activity;
    private final SharedPreferences preferences;

    public UserContactsUploader(BaseActivity activity) {
        this.activity = activity;
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    public void uploadContacts(OnUploadContactsSuccess listener) {
        // Don't do anything if we have tried in the last 1 day.

        final long lastTry = preferences.getLong(PARAM_LAST_CONTACTS_SYNC_TRY_TIMESTAMP, 0);
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastTry < DateUtils.DAY_IN_MILLIS) {
            return;
        }

        // Check if we have necessary permissions.
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            if (listener != null) {
                listener.onUploadSuccess();
            }
            new Thread(new UserContactsUploadRunner(preferences, activity, listener)).start();
            preferences.edit().putLong(PARAM_LAST_CONTACTS_SYNC_TRY_TIMESTAMP, currentTimeMillis).apply();
            return;
        }

        // We do not have necessary permissions, request for it.
        // Should we show an explanation?
      /*  if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS)) {
            // Show an expanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            Preferences preferences = Preferences.getInstance(activity);
            AskForContactsDialog.show(activity,preferences);
        } else {*/
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_CONTACTS},
                BaseActivity.PERMISSIONS_REQUEST_READ_CONTACTS);
        //}
    }

    private static class UserContactsUploadRunner implements Listener<JSONObject>, ErrorListener, Runnable {
        private final SharedPreferences preferences;
        private final Context context;
        private final OnUploadContactsSuccess listener;

        private UserContactsUploadRunner(SharedPreferences preferences, Context context, OnUploadContactsSuccess listener) {
            this.preferences = preferences;
            this.context = context.getApplicationContext();
            this.listener = listener;
        }

        @Override
        @SuppressWarnings("TryFinallyCanBeTryWithResources")
        public void run() {
            String selectionExtras = " and "
                    + ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP
                    + " >= " + preferences.getLong(PARAM_LAST_CONTACTS_SYNC_TIMESTAMP, 0);
            String order = ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP +
                    " LIMIT " + MAX_CONTACTS_TO_UPLOAD;

            // Load the contacts data.
            try {
                List<UserContact> contacts = ContactUtils.getContacts(context, selectionExtras, order, true);
                if (contacts.isEmpty()) {
                    return;
                }

                // Upload contacts data.
                ContactsUploadRequest.submit(context, contacts, Priority.LOW, this, this);
            } catch (Exception e) {
                Crashlytics.getInstance().core.logException(e);
            }
        }

        @Override
        public void onResponse(JSONObject jsonObject, boolean isIntermediate) {
            // TODO: replace currentTimeMillis with last contact timestamp.
          /*  if (listener != null)
                listener.onUploadSuccess();*/
            Log.i(LOG_TAG, "Successfully uploaded the contacts");
            preferences.edit().putLong(PARAM_LAST_CONTACTS_SYNC_TIMESTAMP, System.currentTimeMillis()).apply();
        }

        @Override
        public void onErrorResponse(VolleyError volleyError) {
            Log.w(LOG_TAG, volleyError.getMessage(), volleyError.getCause());
            Crashlytics.getInstance().core.logException(volleyError.getCause());
        }
    }

    public interface OnUploadContactsSuccess {
        public void onUploadSuccess();
    }

}
