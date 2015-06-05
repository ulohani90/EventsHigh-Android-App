package com.eventshigh.nearme.app.data;

import android.database.Cursor;
import android.provider.ContactsContract;

import org.json.JSONException;
import org.json.JSONObject;

public class UserContact extends JSONObject {
  public UserContact(String name, String phoneNumber) throws JSONException {
    put("name", name);
    put("mobile_no", phoneNumber);
  }

  public static UserContact parseFromCursor(Cursor cursor) throws JSONException {
    String name = cursor.getString(
        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
    String phoneNumber =
        cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
    return new UserContact(name, phoneNumber);
  }
}
