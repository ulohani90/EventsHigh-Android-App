package com.eventshigh.nearme.app.data;

import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the user contact.
 */
public class UserContact {
    public final String mobileNo;
    @Nullable public final String name;
    @Nullable public final String emails;

    public UserContact(String mobileNo, @Nullable String name, @Nullable String emails) {
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
        if (emails != null) {
            jsonObject.put("email", emails);
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

    public static UserContact parseFromCursor(Cursor cursor) throws JSONException {
        String name = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
        String phoneNumber = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        String emails = cursor.getString(cursor.getColumnIndex(Email.DATA));
        return new UserContact(name, phoneNumber, emails);
    }
}
