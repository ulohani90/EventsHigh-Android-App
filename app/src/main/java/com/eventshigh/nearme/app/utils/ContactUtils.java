package com.eventshigh.nearme.app.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ContactUtils {
    public static Cursor getContactsCursor(Context context, String selectionExtras, String order) {
        // Build contact query.
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
        };
        if (selectionExtras == null) {
            selectionExtras = "";
        }
        String selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1 " + selectionExtras;

        // Parse contacts data.
        return context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, null, order);
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

    public static byte[] getPhotoForContactId(Context context, String contactId) {
        long contactIdLong = Long.parseLong(contactId);
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactIdLong);
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                return cursor.getBlob(0);
            }
        } finally {
            cursor.close();
        }
        return null;
    }
}
