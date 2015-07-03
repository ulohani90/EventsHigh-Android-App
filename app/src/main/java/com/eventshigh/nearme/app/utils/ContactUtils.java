package com.eventshigh.nearme.app.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.UserContact;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ContactUtils {
    private static final String LOG_TAG = ContactUtils.class.getSimpleName();

    public static List<UserContact> getContacts(Context context, @Nullable String selectionExtras,
            String order, boolean addEmail) {
        // Build contact query.
        String[] projection = new String[] {
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
        };

        if (selectionExtras == null) {
            selectionExtras = "";
        }
        String selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1 " + selectionExtras;

        // Parse contacts data.
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
                try {
                    UserContact userContact = UserContact.parseFromCursor(cursor);
                    if (addEmail) {
                        emailCursor = ContactUtils.getEmailCursorForContactId(context, userContact.contactId);
                        userContact.parseEmailsFromCursor(emailCursor);
                    }
                    contacts.add(userContact);
                } catch (JSONException e) {
                    Log.w(LOG_TAG, "failed to load contact", e);
                    Crashlytics.getInstance().core.logException(e);
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

    public static Cursor getEmailCursorForContactId(Context context, String contactId) {
        String[] projection = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
        };
        String selection = ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + contactId;
        return context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                projection, selection, null, null);
    }

    public static @Nullable Bitmap getPhotoForPhone(Context context, String phone) {
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
            Crashlytics.getInstance().core.logException(e);
        } finally {
            cursor.close();
        }
        return null;
    }

    public static @Nullable Bitmap getPhotoForContactId(Context context, String contactId, int size) {
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
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length, options);

                    options.inSampleSize = (options.outHeight * options.outWidth)/(size * size);
                    options.inJustDecodeBounds = false;
                    return BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length, options);
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public static @Nullable UserContact getContactForServerPhone(Context context, String phone) {
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
            return new UserContact(
                    cursor.getString(cursor.getColumnIndex(PhoneLookup._ID)),
                    phone,
                    cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME)),
                    null
            );
        } finally {
            cursor.close();
        }
    }
}
