package com.eventshigh.nearme.app.data;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.eventshigh.nearme.app.utils.ContactUtils;
import com.eventshigh.nearme.app.utils.ImageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the user contact.
 */
public class UserContact implements Comparable<UserContact> {
    public final String contactId;
    public final String mobileNo;
    public final String name;
    @Nullable public String[] emails;

    public static UserContact parseFromCursor(Cursor cursor) throws JSONException {
        String contactId = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
        String name = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
        String phoneNumber = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        return new UserContact(contactId, phoneNumber, name, null);
    }

    public UserContact(String contactId, String mobileNo, String name, @Nullable String[] emails) {
        this.contactId = contactId;
        this.mobileNo = mobileNo;
        this.name = name;
        this.emails = emails;
    }

    public Drawable getDrawable(Context context, int size) {
        Bitmap bitmap = ContactUtils.getPhotoForContactId(context, contactId, size);
        if (bitmap != null) {
            return new BitmapDrawable(context.getResources(), ImageUtils.getCircularBitmapFrom(bitmap));
        }

        int color = ColorGenerator.MATERIAL.getColor(name);
        return TextDrawable.builder().buildRoundRect(Character.toString(name.charAt(0)), color, size);
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

    public void parseEmailsFromCursor(@Nullable Cursor emailCursor) throws JSONException {
        if (emailCursor == null) {
            emails = null;
            return;
        }

        emails = new String[emailCursor.getCount()];
        for (int i = 0; i < emails.length; i++) {
            emailCursor.moveToNext();
            emails[i] = emailCursor.getString(
                    emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
        }
    }

    @Override
    public boolean equals(Object object) {
        return object != null && object instanceof UserContact &&
                contactId.equals(((UserContact) object).contactId);
    }

    @Override
    public int hashCode() {
        return contactId.hashCode();
    }

    @Override
    public int compareTo(@NonNull UserContact another) {
        return contactId.compareTo(another.contactId);
    }
}
