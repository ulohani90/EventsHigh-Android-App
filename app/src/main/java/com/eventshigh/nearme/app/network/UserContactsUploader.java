package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UserContactsUploader {
  private final JSONObject objectToUpload;
  JSONArray userContactArray = new JSONArray();

  private UserContactsUploader(Context context) throws JSONException {
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
    if (userContactArray.length() == 2) {
      upload();
      createContactsArray();
    }
  }

  private void upload() {
    if (userContactArray.length() > 0) {
      // Upload contact list
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
