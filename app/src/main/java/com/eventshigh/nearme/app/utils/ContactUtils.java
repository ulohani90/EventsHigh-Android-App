package com.eventshigh.nearme.app.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import java.io.IOException;

public class ContactUtils {
    public static Cursor getContactsCursor(Context context, @Nullable String selectionExtras, String order) {
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

    public static Bitmap getPhotoForPhone(Context context, String phone) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phone));

        // Build contact query.
        String[] projection = { ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI };

        // Parse contacts data.
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) {
            return null;
        }

        try {
            if (!cursor.moveToNext()) {
                return null;
            }
            String contactPhotoUri = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI));
            if (contactPhotoUri == null) {
                return null;
            }
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(),
                    Uri.parse(contactPhotoUri));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
        return null;
    }

    public static String getLocalPhotoForServerPhone(Context context, String phone) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phone));

        // Build contact query.
        String[] projection = { ContactsContract.PhoneLookup.NUMBER };

        // Parse contacts data.
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) {
            return null;
        }

        try {
            if (!cursor.moveToNext()) {
                return null;
            }
            return cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.NUMBER));
        } finally {
            cursor.close();
        }
    }

    public static Bitmap getPhotoForContactId(Context context, String contactId) {
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
                byte[] bitmapData = cursor.getBlob(0);
                if (bitmapData != null) {
                    return BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }
}
