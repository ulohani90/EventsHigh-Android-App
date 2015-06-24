package com.eventshigh.nearme.app.data;

import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the user contact.
 */
public class UserContact {
    public final String mobileNo;
    @Nullable public final String name;
    @Nullable public final String[] emails;

    public UserContact(String mobileNo, @Nullable String name, @Nullable String[] emails) {
        this.mobileNo = mobileNo;
        this.name = name;
        this.emails = emails;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mobile_no", mobileNo);
        if (name != null) {
            jsonObject.put("name", name);
        }
        if (emails != null && emails.length > 0) {
            JSONArray jsonArray = new JSONArray();
            for (String email : emails) {
                jsonArray.put(email);
            }
            jsonObject.put("emails", jsonArray);
        }

        return jsonObject;
    }

    public String toString() {
        try {
            return toJSON().toString();
        } catch (JSONException e) {
            return super.toString();
        }
    }

    public static UserContact parseFromCursor(Cursor cursor, Cursor emailCursor) throws JSONException {
        String phoneNumber = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        String name = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

        String[] emails = new String[emailCursor.getCount()];
        for (int i = 0; i < emails.length; i++) {
            emailCursor.moveToNext();
            emails[i] = emailCursor.getString(
                    emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
        }

        return new UserContact(phoneNumber, name, emails);
    }

    public static UserContact parseFromCursor(Cursor cursor) throws JSONException {
        String phoneNumber = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        String name = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
        return new UserContact(phoneNumber, name, null);
    }
}
