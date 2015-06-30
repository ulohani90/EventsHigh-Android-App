package com.eventshigh.nearme.app.task;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.utils.ContactUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * AsyncTask used to resolve the phone numbers sent by server to the phone numbers as stored in
 * the contacts DB.
 */
public class ContactPhoneResolverTask extends AsyncTask<JSONObject, Void, List<String>> {
    public interface Callback {
        void onContacsResolved(@Nullable List<String> contactsOnEh);
    }

    private final Context context;
    private final Callback callback;

    public ContactPhoneResolverTask(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected List<String> doInBackground(JSONObject... params) {
        try {
            JSONObject jsonObject = params[0];
            JSONArray friends = jsonObject.getJSONArray("friends");
            List<String> contactOnEh = new ArrayList<>();
            for (int i = 0; i < friends.length(); i++) {
                String mobileNo = friends.getJSONObject(i).getString("mobile_no");
                contactOnEh.add(ContactUtils.getContactIdForServerPhone(context, mobileNo));
            }
            return contactOnEh;
        } catch (JSONException e) {
            Crashlytics.getInstance().core.logException(e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(@Nullable List<String> contacts) {
        callback.onContacsResolved(contacts);
    }
}
