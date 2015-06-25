package com.eventshigh.nearme.app.data;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.utils.ContactUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the user contact.
 */
public class UserContact {
    public final String contactId;
    public final String mobileNo;
    @Nullable public final String name;
    @Nullable public String[] emails;

    public UserContact(String contactId, String mobileNo, @Nullable String name, @Nullable String[] emails) {
        this.contactId = contactId;
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

    public void parseEmailsFromCursor(Cursor emailCursor) throws JSONException {
        emails = new String[emailCursor.getCount()];
        for (int i = 0; i < emails.length; i++) {
            emailCursor.moveToNext();
            emails[i] = emailCursor.getString(
                    emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
        }
    }

    public static UserContact parseFromCursor(Cursor cursor) throws JSONException {
        String contactId = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
        String phoneNumber = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        String name = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
        return new UserContact(contactId, phoneNumber, name, null);
    }
}
