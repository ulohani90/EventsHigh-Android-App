package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.network.SocialActionsRequest.SocialActions;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.AccountStateReporter;
import com.eventshigh.nearme.app.utils.ContactUtils;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SocialActionsRequest extends JsonRequest<SocialActions> {

    public static class SocialActions {
        public final Map<String, Set<SocialFriend>> eventFavourites;
        public final Map<String, Set<SocialFriend>> tagFollowers;

        public SocialActions(Map<String, Set<SocialFriend>> eventFavourites,
                             Map<String, Set<SocialFriend>> tagFollowers) {
            this.eventFavourites = eventFavourites;
            this.tagFollowers = tagFollowers;
        }

        public @Nullable Set<SocialFriend> getTagFollowers(String tag) {
            return tagFollowers.get(EventCategory.toCategoryParsableString(tag));
        }

        public int getNumFollowers(String tag) {
            Set<SocialFriend> followers = getTagFollowers(tag);
            return followers == null ? 0 : followers.size();
        }
    }

    public static void submit(Context context, Priority priority, Object tag, boolean shouldBypassCache,
            Listener<SocialActions> listener, ErrorListener errorListener) {
        try {
            Uri getSocialActionsUri =
                    AccountStateReporter.getBaseUri(context, "get_social_actions").build();
            SocialActionsRequest request = new SocialActionsRequest(
                    context, getSocialActionsUri, priority, shouldBypassCache, listener, errorListener);
            request.setTag(tag);
            VolleyHelper.addToRequestQueue(context, request);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            errorListener.onErrorResponse(new VolleyError(e));
        }
    }

    private final Context context;
    private final Priority priority;
    private final Uri getSocialActionsUri;

    public SocialActionsRequest(Context context, Uri getSocialActionsUri, Priority priority,
            boolean shouldBypassCache,  Listener<SocialActions> listener, ErrorListener errorListener)
            throws GeneralSecurityException, UnsupportedEncodingException {
        super(Method.GET, Signer.sign(getSocialActionsUri).toString(), null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);

        this.context = context;
        this.priority = priority;
        this.getSocialActionsUri = getSocialActionsUri;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    public String getCacheKey() {
        return getSocialActionsUri.toString();
    }

    @Override
    protected Response<SocialActions> parseNetworkResponse(NetworkResponse response) {
        Map<String, Set<SocialFriend>> eventFavourites  = Utils.getMap();
        Map<String, Set<SocialFriend>> tagFollowers = Utils.getMap();

        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            JSONObject resp = new JSONObject(jsonString);
            JSONArray friends = resp.getJSONArray("social");
            String myMobileNo = new Account(context).getUserInfo().phoneNo;

            for (int i = 0; i < friends.length(); i++) {
                String mobileNo = friends.getJSONObject(i).getString("mobile_no");
                if (mobileNo.equalsIgnoreCase(myMobileNo)) {
                    continue;
                }
                UserContact contact = ContactUtils.getContactForServerPhone(context, mobileNo);
                if (contact == null) {
                    continue;
                }

                JSONArray favouritesArray = friends.getJSONObject(i).getJSONArray("favourites");
                for (int j = 0; j < favouritesArray.length(); j++) {
                    add(eventFavourites, favouritesArray.getString(j), new SocialFriend(contact));
                }

                JSONArray followingsArray = friends.getJSONObject(i).getJSONArray("followings");
                for (int j = 0; j < followingsArray.length(); j++) {
                    String key = EventCategory.toCategoryParsableString(followingsArray.getString(j));
                    add(tagFollowers, key, new SocialFriend(contact));
                }
            }

            return Response.success(new SocialActions(eventFavourites, tagFollowers),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }

    private static boolean add(Map<String, Set<SocialFriend>> usersMap, String key, SocialFriend friend) {
        Set<SocialFriend> friends = usersMap.get(key);
        if (friends == null) {
            friends = new HashSet<>();
            usersMap.put(key, friends);
        }

        return friends.add(friend);
    }
}
