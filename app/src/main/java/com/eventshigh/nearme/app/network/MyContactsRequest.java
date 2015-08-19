package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.AccountStateReporter;
import com.eventshigh.nearme.app.utils.ContactUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyContactsRequest extends JsonRequest<List<UserContact>> {
    public static void submit(Context context, Priority priority, Object tag, boolean shouldBypassCache,
            Listener<List<UserContact>> listener, ErrorListener errorListener) {
        try {
            Uri socialFriendsUri =
                    AccountStateReporter.getBaseUri(context, "get_social_friends").build();
            MyContactsRequest request = new MyContactsRequest(
                    context, socialFriendsUri, priority, shouldBypassCache, listener, errorListener);
            request.setTag(tag);
            VolleyHelper.addToRequestQueue(context, request);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            errorListener.onErrorResponse(new VolleyError(e));
        }
    }

    private final Context context;
    private final Priority priority;
    private final Uri socialFriendsUri;

    public MyContactsRequest(Context context, Uri socialFriendsUri, Priority priority,
            boolean shouldBypassCache, Listener<List<UserContact>> listener, ErrorListener errorListener)
            throws GeneralSecurityException, UnsupportedEncodingException {
        super(Method.GET, Signer.sign(socialFriendsUri).toString(), null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);

        this.context = context;
        this.priority = priority;
        this.socialFriendsUri = socialFriendsUri;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    public String getCacheKey() {
        return socialFriendsUri.toString();
    }

    @Override
    protected Response<List<UserContact>> parseNetworkResponse(NetworkResponse response) {
        try {
            // See the contacts which are already on EH.
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            JSONObject resp = new JSONObject(jsonString);
            JSONArray friends = resp.getJSONArray("friends");

            String myMobileNo = new Account(context).getUserInfo().phoneNo;
            Set<UserContact> contactOnEh = new HashSet<>();
            for (int i = 0; i < friends.length(); i++) {
                String mobileNo = friends.getJSONObject(i).getString("mobile_no");
                if (!mobileNo.equals(myMobileNo)) {
                    UserContact contact = ContactUtils.getContactForServerPhone(context, mobileNo);
                    if (contact != null) {
                        contactOnEh.add(contact);
                    }
                }
            }

            // Read the local contacts and remove duplicates.
            List<UserContact> contacts = new ArrayList<>(contactOnEh.size());
            contacts.addAll(contactOnEh);

            // Sort by Name.
            Collections.sort(contacts, new Comparator<UserContact>() {
                @Override
                public int compare(UserContact lhs, UserContact rhs) {
                    if (lhs.name == null || rhs.name == null) {
                        return 0;
                    }
                    return lhs.name.compareTo(rhs.name);
                }
            });

            return Response.success(contacts, HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }
}
