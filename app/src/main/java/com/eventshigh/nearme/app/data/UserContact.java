package com.eventshigh.nearme.app.data;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.eventshigh.nearme.app.utils.ContactUtils;
import com.eventshigh.nearme.app.utils.ImageUtils;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents the user contact.
 */
public class UserContact implements Comparable<UserContact>, Serializable {
    public final String contactId;
    public final String mobileNo;
    public final String name;
    @Nullable public final String[] emails;

    public static UserContact parseFromCursor(Cursor cursor, Cursor emailCursor) {
        String contactId = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
        String name = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
        String phoneNumber = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

        String[] emails = null;
        if (emailCursor != null && emailCursor.getCount() > 0) {
            emails = new String[emailCursor.getCount()];
            for (int i = 0; i < emails.length; i++) {
                emailCursor.moveToNext();
                emails[i] = emailCursor.getString(
                        emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
            }
        }

        return new UserContact(contactId, phoneNumber, name, emails);
    }

    public UserContact(String contactId, String mobileNo, String name, @Nullable String[] emails) {
        this.contactId = contactId;
        this.mobileNo = mobileNo;
        this.name = name;
        this.emails = emails;
    }

    public UserContact withEmails(Context context) {
        return new UserContact(contactId, mobileNo, name,
                parseEmails(ContactUtils.getEmailCursorForContactId(context, contactId)));
    }

    public Drawable getDrawable(Context context, int size) {
        Bitmap bitmap = getPhotoForContactId(context, contactId);
        if (bitmap != null) {
            return new BitmapDrawable(context.getResources(), bitmap);
        }

        return getDrawableForName(name, size);
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

    public static Drawable getDrawableForName(String name, int size) {
        int color = ColorGenerator.MATERIAL.getColor(name);
        return TextDrawable.builder().buildRoundRect(Character.toString(name.charAt(0)), color, size);
    }

    // Use 10% memory for bitmap cache.
    private static final int BITMAP_CACHE_SIZE = (int)(Runtime.getRuntime().maxMemory() / ( 10 * 1024));
    private static final int MAX_PHOTO_SIZE_DP = 72;
    private static final Set<String> NULL_PHOTO_CONTACT_ID = new HashSet<>();
    private static final LruCache<String, Bitmap> CONTACT_PHOTO_CACHE =
        new LruCache<String, Bitmap>(BITMAP_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };

    public static @Nullable Bitmap getPhotoForContactId(Context context, String contactId) {
        if (NULL_PHOTO_CONTACT_ID.contains(contactId)) {
            return null;
        }

        Bitmap bitmap = CONTACT_PHOTO_CACHE.get(contactId);
        if (bitmap != null) {
            return bitmap;
        }

        long contactIdLong = Long.parseLong(contactId);
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactIdLong);
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);

        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    byte[] bitmapData = cursor.getBlob(0);
                    if (bitmapData != null) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length, options);

                        int size = Utils.dpToPx(context, MAX_PHOTO_SIZE_DP);
                        options.inSampleSize = (options.outHeight * options.outWidth) / (size * size);
                        options.inJustDecodeBounds = false;
                        bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length, options);
                        bitmap = ImageUtils.getCircularBitmapFrom(bitmap);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        if (bitmap == null) {
            NULL_PHOTO_CONTACT_ID.add(contactId);
        } else {
            CONTACT_PHOTO_CACHE.put(contactId, bitmap);
        }
        return bitmap;
    }

    private @Nullable String[] parseEmails(@Nullable Cursor emailCursor) {
        String[] emails = null;
        if (emailCursor != null && emailCursor.getCount() > 0) {
            emails = new String[emailCursor.getCount()];
            for (int i = 0; i < emails.length; i++) {
                emailCursor.moveToNext();
                emails[i] = emailCursor.getString(
                        emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
            }
        }
        return emails;
    }
}
