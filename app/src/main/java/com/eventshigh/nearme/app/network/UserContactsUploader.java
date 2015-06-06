package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.text.format.DateUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.user.AccountStateReporter;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UserContactsUploader implements Response.Listener<JSONObject>, Response.ErrorListener, RequestQueue.RequestFinishedListener<Object> {
    private static final int MAX_CONTACTS_TO_UPLOAD = 200;

    private final Context context;
    private final JSONObject objectToUpload;
    private final List<JsonObjectRequest> pendingRequests;

    JSONArray userContactArray = new JSONArray();
    private boolean syncFailed;

    private UserContactsUploader(Context context) throws JSONException {
        this.context = context;
        objectToUpload = new JSONObject();
        objectToUpload.put("android_id", Utils.getAndroidId(context));
        createContactsArray();
        pendingRequests = new ArrayList<>();
    }

    private void createContactsArray() throws JSONException {
        userContactArray = new JSONArray();
        objectToUpload.put("contacts", userContactArray);
    }

    private void addUserContact(Cursor cursor) throws JSONException {
        userContactArray.put(UserContact.parseFromCursor(cursor));
        if (userContactArray.length() == MAX_CONTACTS_TO_UPLOAD) {
            upload();
            createContactsArray();
        }
    }

    private void upload() {
        Uri requestUrl = AccountStateReporter.getBaseUriWithoutAndroidId(context,
                "record_user_contacts").build();
        System.out.println("--------> sending " + requestUrl.toString());
        System.out.println("--------> body " + objectToUpload);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                requestUrl.toString(), objectToUpload, this, this);
        synchronized (pendingRequests) {
            pendingRequests.add(request);
        }
        VolleyHelper.addToRequestQueue(context, request);
        VolleyHelper.getRequestQueue(context).addRequestFinishedListener(this);
    }

    private void flush() throws InterruptedException {
        if (userContactArray.length() > 0) {
            upload();
        }

        synchronized (pendingRequests) {
            while (pendingRequests.size() != 0) {
                pendingRequests.wait();
            }
        }
    }

    @Override
    public void onResponse(JSONObject jsonObject, boolean b) {
        System.out.println("--------> success " + jsonObject + "  " + b);
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        System.out.println("--------> failure " + volleyError);
        syncFailed = true;
    }

    @Override
    public void onRequestFinished(Request<Object> request) {
        synchronized (pendingRequests) {
            pendingRequests.remove(request);
            pendingRequests.notify();
        }
    }

    public static void uploadContacts(final Context context) {
        // Don't do anything if we have synced in the last 7 days
        final Preferences preferences = Preferences.getInstance(context);
        final long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - preferences.getLastContactsSyncTimestamp() < DateUtils.WEEK_IN_MILLIS) {
            return;
        }

        new Thread() {
            public void run() {
                String[] projection = {
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                };
                String selection;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    // On android versions that have the contact updated timestamp, get only the
                    // contacts that have changed since the last sync
                    selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1 and "
                            + ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP
                            + " >= " + preferences.getLastContactsSyncTimestamp();
                } else {
                    selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1";
                }
                Context applicationContext = context.getApplicationContext();
                Cursor cursor = applicationContext.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        projection, selection, null, null);

                try {
                    UserContactsUploader uploader = new UserContactsUploader(context);
                    while (cursor.moveToNext()) {
                        uploader.addUserContact(cursor);
                    }
                    uploader.flush();
                    if (!uploader.syncFailed) {
                        preferences.setLastContactsSyncTimestamp(currentTimeMillis);
                    }
                } catch (JSONException | InterruptedException e) {
                    e.printStackTrace();
                }

                cursor.close();
            }
        }.start();
    }
}
