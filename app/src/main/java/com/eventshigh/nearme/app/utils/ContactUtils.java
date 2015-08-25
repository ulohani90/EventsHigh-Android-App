package com.eventshigh.nearme.app.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.PhoneLookup;
import android.support.annotation.Nullable;
import android.util.LruCache;

import com.eventshigh.nearme.app.data.UserContact;

import java.util.ArrayList;
import java.util.List;

public class ContactUtils {
    public static List<UserContact> getContacts(Context context, @Nullable String selectionExtras,
            @Nullable String order, boolean addEmail) {
        // Build contact query.
        if (selectionExtras == null) {
            selectionExtras = "";
        }
        String selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1 " + selectionExtras;

        // Parse contacts data.
        String[] projection = new String[] { Phone.CONTACT_ID, Phone.DISPLAY_NAME, Phone.NUMBER };
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, null, order);

        // Parse the contacts data.
        List<UserContact> contacts = new ArrayList<>();
        if (cursor == null) {
            return contacts;
        }

        try {
            while (cursor.moveToNext()) {
                Cursor emailCursor = null;
                if (addEmail) {
                    String contactId = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                    emailCursor = ContactUtils.getEmailCursorForContactId(context, contactId);
                }
                try {
                    UserContact userContact = UserContact.parseFromCursor(cursor, emailCursor);
                    contacts.add(userContact);
                } finally {
                    if (emailCursor != null) {
                        emailCursor.close();
                    }
                }
            }
        } finally {
            cursor.close();
        }

        return contacts;
    }

    public static @Nullable UserContact getContactForServerPhone(Context context, String phone) {
        UserContact contact = PHONE_TO_CONTACT_MAP.get(phone);
        if (contact != null) {
            return contact;
        }

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phone));

        // Build contact query.
        String[] projection = { PhoneLookup._ID, PhoneLookup.DISPLAY_NAME };

        // Parse contacts data.
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) {
            return null;
        }

        try {
            if (!cursor.moveToNext()) {
                return null;
            }
            contact = new UserContact(
                    cursor.getString(cursor.getColumnIndex(PhoneLookup._ID)),
                    phone,
                    cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME)),
                    null
            );
            PHONE_TO_CONTACT_MAP.put(phone, contact);
            return contact;
        } finally {
            cursor.close();
        }
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static String[] getAllPhoneNo(Context context, String contactId) {
        // Build contact query.
        String[] projection = { Phone.NUMBER };
        Cursor cursor = context.getContentResolver().query(
                Phone.CONTENT_URI, projection, Phone.CONTACT_ID + " = " + contactId, null, null);

        // Parse contacts data.
        if (cursor == null) {
            return new String[0];
        }

        try {
            String[] allPhoneNo = new String[cursor.getCount()];
            for (int i = 0; i < allPhoneNo.length; i++) {
                cursor.moveToNext();
                allPhoneNo[i] = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
            }
            return allPhoneNo;
        } finally {
             cursor.close();
        }
    }

    public static Cursor getEmailCursorForContactId(Context context, String contactId) {
        String[] projection = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
        };
        String selection = ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + contactId;
        return context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                projection, selection, null, null);
    }

    private static final LruCache<String, UserContact> PHONE_TO_CONTACT_MAP = new LruCache<>(200);
}
