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
import com.eventshigh.nearme.app.network.MyContactsRequest.MyContacts;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.user.AccountStateReporter;
import com.eventshigh.nearme.app.utils.ContactUtils;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class MyContactsRequest extends JsonRequest<MyContacts> {
    public static class MyContacts {
        public final List<UserContact> EHContacts;
        public final List<UserContact> otherContacts;

        public MyContacts(List<UserContact> EHContacts, List<UserContact> otherContacts) {
            this.EHContacts = EHContacts;
            this.otherContacts = otherContacts;
        }

        public boolean isEmpty() {
            return EHContacts.isEmpty() && otherContacts.isEmpty();
        }

        public boolean isFriendsOnEhEmpty() {
            return EHContacts.isEmpty();
        }
    }

    public static void submit(Context context, Priority priority, Object tag, boolean shouldBypassCache,
            Listener<MyContacts> listener, ErrorListener errorListener) {
        try {
            Uri getSocialFriendsUri =
                    AccountStateReporter.getBaseUri(context, "get_social_friends").build();
            String url = Signer.sign(getSocialFriendsUri).toString();
            MyContactsRequest request = new MyContactsRequest(
                    context, url, priority, shouldBypassCache, listener, errorListener);
            request.setTag(tag);
            VolleyHelper.addToRequestQueue(context, request);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            errorListener.onErrorResponse(new VolleyError(e));
        }
    }

    private final Context context;
    private final Priority priority;

    public MyContactsRequest(Context context, String url, Priority priority, boolean shouldBypassCache,
            Listener<MyContacts> listener, ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);

        this.context = context;
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<MyContacts> parseNetworkResponse(NetworkResponse response) {
        try {
            // See the contacts which are already on EH.
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            JSONObject resp = new JSONObject(jsonString);
            JSONArray friends = resp.getJSONArray("friends");
            List<String> contactOnEh = new ArrayList<>();
            for (int i = 0; i < friends.length(); i++) {
                String mobileNo = friends.getJSONObject(i).getString("mobile_no");
                contactOnEh.add(ContactUtils.getContactIdForServerPhone(context, mobileNo));
            }

            // Read the local contacts and remove duplicates.
            HashSet<UserContact> contacts = new HashSet<>();
            contacts.addAll(ContactUtils.getContacts(context, null, null, false));
            List<UserContact> uniqueContacts = new ArrayList<>(contacts.size());
            uniqueContacts.addAll(contacts);

            // Remove the ones without any name.
            Iterator<UserContact> contactIterator = uniqueContacts.iterator();
            while ((contactIterator.hasNext())) {
                if (Utils.checkIfUnknown(contactIterator.next().name) == null) {
                    contactIterator.remove();
                }
            }

            // Sort by Name.
            Collections.sort(uniqueContacts, new Comparator<UserContact>() {
                @Override
                public int compare(UserContact lhs, UserContact rhs) {
                    if (lhs.name == null || rhs.name == null) {
                        return 0;
                    }
                    return lhs.name.compareTo(rhs.name);
                }
            });

            // Split the contacts into two sections, one which are on EH and one which are not on EH.
            List<UserContact> EHContacts = new ArrayList<>(contactOnEh.size());
            List<UserContact> otherContacts = new ArrayList<>(uniqueContacts.size());

            for (UserContact contact : uniqueContacts) {
                if (contactOnEh.contains(contact.contactId)) {
                    EHContacts.add(contact);
                } else {
                    otherContacts.add(contact);
                }
            }

            return Response.success(new MyContacts(EHContacts, otherContacts),
                    HttpHeaderParser.parseCacheHeaders(response));

        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }
}
