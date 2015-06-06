package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.user.AccountStateReporter;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UserContactsUploader {
    private static final int MAX_CONTACTS_TO_UPLOAD = 200;

    private final Context context;
    private final JSONObject objectToUpload;
    JSONArray userContactArray = new JSONArray();

    private UserContactsUploader(Context context) throws JSONException {
        this.context = context;
        objectToUpload = new JSONObject();
        objectToUpload.put("android_id", Utils.getAndroidId(context));
        createContactsArray();
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
        if (userContactArray.length() > 0) {
            Uri requestUrl = AccountStateReporter.getBaseUriWithoutAndroidId(context,
                    "record_user_contacts").build();
            System.out.println("--------> sending " + requestUrl.toString());
            System.out.println("--------> body " + objectToUpload);

            VolleyHelper.addToRequestQueue(context, new JsonObjectRequest(Request.Method.POST,
                    requestUrl.toString(), objectToUpload, new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(JSONObject jsonObject, boolean b) {
                    System.out.println("--------> success " + jsonObject + "  " + b);
                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    System.out.println("--------> failure " + volleyError);
                }
            }));
        }
    }

    public static void uploadContacts(final Context context) {
        new Thread() {
            public void run() {
                String[] PROJECTION = {
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                };
                String SELECTION = ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1";
                Context applicationContext = context.getApplicationContext();
                Cursor cursor = applicationContext.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        PROJECTION, SELECTION, null, null);
                uploadContacts(applicationContext, cursor);
                cursor.close();
            }
        }.start();
    }

    private static void uploadContacts(Context context, Cursor cursor) {
        try {
            UserContactsUploader uploader = new UserContactsUploader(context);
            while (cursor.moveToNext()) {
                uploader.addUserContact(cursor);
            }
            uploader.upload();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
