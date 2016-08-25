package com.eventshigh.nearme.app.data;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.ContactUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author shubham
 * @since 1/7/16.
 */

public class ProfileInfo implements Parcelable {

    private String profileId;
    private String name;
    private String lastCity;
    private String profilePic;
    private String email;
    private ArrayList<MovieUserReviewObject> movieUserReviewObjectArrayList;
    private ArrayList<MyEventsRequest.TopicEvents> myInterestEvents;
    private MyEventsRequest.MeEventFavouriteObject meEventFavouriteObject;
    private List<NewSocialFriend> friendList;

    //getter and setters
    public List<NewSocialFriend> getUserContactList() {
        return friendList;
    }

    public void setUserContactList(List<NewSocialFriend> friendList) {
        this.friendList = friendList;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastCity() {
        return lastCity;
    }

    public void setLastCity(String lastCity) {
        this.lastCity = lastCity;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public MyEventsRequest.MeEventFavouriteObject getMeEventFavouriteObject() {
        return meEventFavouriteObject;
    }

    public void setMeEventFavouriteObject(MyEventsRequest.MeEventFavouriteObject meEventFavouriteObject) {
        this.meEventFavouriteObject = meEventFavouriteObject;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public ArrayList<MovieUserReviewObject> getMovieUserReviewObjectArrayList() {
        return movieUserReviewObjectArrayList;
    }

    public void setMovieUserReviewObjectArrayList(ArrayList<MovieUserReviewObject> movieUserReviewObjectArrayList) {
        this.movieUserReviewObjectArrayList = movieUserReviewObjectArrayList;
    }

    public List<MyEventsRequest.TopicEvents> getMyInterestEvents() {
        return myInterestEvents;
    }

    public void setMyInterestEvents(ArrayList<MyEventsRequest.TopicEvents> myInterestEvents) {
        this.myInterestEvents = myInterestEvents;
    }

    public Uri getProfileShareURI(@Nullable String src) {
        return EventsHighEndpoints.getProfileShareURI(this, src);
    }


    public ProfileInfo(@Nullable String name, @Nullable String lastCity, @Nullable String profilePic, @Nullable String email,
                       ArrayList<MovieUserReviewObject> movieUserReviewObjects,
                       ArrayList<MyEventsRequest.TopicEvents> myInterestObjectList,
                       MyEventsRequest.MeEventFavouriteObject meEventFavouriteObject,
                       List<NewSocialFriend> friendList) {
        this.name = name;
        this.lastCity = lastCity;
        this.profilePic = profilePic;
        this.email = email;
        this.movieUserReviewObjectArrayList = movieUserReviewObjects;
        this.myInterestEvents = myInterestObjectList;
        this.meEventFavouriteObject = meEventFavouriteObject;
        this.friendList = friendList;
    }

    public ProfileInfo(Parcel in) {
        profileId = in.readString();
        name = in.readString();
        lastCity = in.readString();
        profilePic = in.readString();
        email = in.readString();

        movieUserReviewObjectArrayList = new ArrayList<>();
        in.readTypedList(movieUserReviewObjectArrayList, MovieUserReviewObject.CREATOR);

        myInterestEvents = new ArrayList<>();
        in.readTypedList(myInterestEvents, MyEventsRequest.TopicEvents.CREATOR);

        meEventFavouriteObject = in.readParcelable(MyEventsRequest.MeEventFavouriteObject.class.getClassLoader());

        friendList = new ArrayList<>();
        in.readTypedList(friendList, NewSocialFriend.CREATOR);

    }


    public static ProfileInfo fromJson(JSONObject jsonObject, Context context, String profileEmail) {
        ArrayList<MyEventsRequest.TopicEvents> events = new ArrayList<>();
        ArrayList<MovieUserReviewObject> movieUserReviewObjects = new ArrayList<>();

        try {
            if (jsonObject.has("interest_events")) {
                JSONArray eventsJsonArray = jsonObject.getJSONArray("interest_events");

                for (int i = 0; i < eventsJsonArray.length(); i++) {

                    List<Event> topicEvents = Event.fromJSON(eventsJsonArray.getJSONObject(i).getJSONArray("topic_events"), true, null);

                    if (profileEmail.equalsIgnoreCase(new Account(context).getUserInfo().email))
                        new Account(context).setIsFollowing(eventsJsonArray.getJSONObject(i).getString("topic"), true);
                    MyEventsRequest.TopicEvents eventData = new MyEventsRequest.TopicEvents(eventsJsonArray.getJSONObject(i).getString("topic"), topicEvents, eventsJsonArray.getJSONObject(i).getInt("event_count"));
                    events.add(eventData);
                }
            }

            JSONArray reviews = jsonObject.getJSONArray("reviews");
            if (reviews != null) {
                for (int i = 0; i < reviews.length(); i++) {
                    movieUserReviewObjects.add(new MovieUserReviewObject(reviews.getJSONObject(i)));
                }
            }

            String profileName = "";
            String profileLastCity = "";
            String profilePic = "";
            String email = "";
            if (jsonObject.has("user_info")) {
                JSONObject userInfo = jsonObject.getJSONObject("user_info");
                profileName = userInfo.getString("name");
                profileLastCity = userInfo.getString("last_city");
                profilePic = userInfo.getString("profile_pic");
                email = userInfo.getString("email");

            }

            List<MyEventsRequest.TopicEvents> favouriteTopicEvents = new ArrayList<>();
            List<MovieDetailObject> favouriteMovie = new ArrayList<>();

            if (jsonObject.has("fav_events")) {
                List<Event> topicEvents = Event.fromJSON(context, jsonObject.getJSONArray("fav_events"), true, profileEmail.equalsIgnoreCase(new Account(context).getUserInfo().email));
                favouriteTopicEvents.add(new MyEventsRequest.TopicEvents(MyEventsRequest.FAVOURITES_NAME, topicEvents));
            }
            if (jsonObject.has("fav_movies")) {
                favouriteMovie = MovieDetailObject.fromJSON(context, jsonObject.getJSONArray("fav_movies"), profileEmail.equalsIgnoreCase(new Account(context).getUserInfo().email));
            }

            List<NewSocialFriend> friendList = new ArrayList<>();
            if (jsonObject.has("friends")) {

                JSONArray friends = jsonObject.getJSONArray("friends");
                FriendsStore friendsStore = new FriendsStore(context);
                if (friends != null) {
                    for (int i = 0; i < friends.length(); i++) {

                        NewSocialFriend newFriend = NewSocialFriend.parseJsonObject(friends.getJSONObject(i));
                        friendList.add(newFriend);
                        if (profileEmail.equalsIgnoreCase(new Account(context).getUserInfo().email) && !friendsStore.isKeyExists(newFriend.getEmail()))
                            friendsStore.setFollowing(newFriend.getEmail(), null, true);
                    }
                }
               /* JSONArray friends = jsonObject.getJSONArray("friends");
                String myMobileNo = new Account(context).getUserInfo().phoneNo;
                Set<UserContact> contactOnEh = new HashSet<>();
                FriendsStore friendsStore = new FriendsStore(context);
                for (int i = 0; i < friends.length(); i++) {
                    String mobileNo = friends.getJSONObject(i).getString("mobile_no");
                    if (!mobileNo.equals(myMobileNo)) {
                        UserContact contact = ContactUtils.getContactForServerPhone(context, mobileNo);
                        if (contact != null) {
                            contactOnEh.add(contact);
                            if (profileEmail.equalsIgnoreCase(new Account(context).getUserInfo().email) && !friendsStore.isKeyExists(mobileNo))
                                friendsStore.setFollowing(contact.mobileNo, contact.contactId, true);
                        }
                    }
                }
                userContactList = new ArrayList<>(contactOnEh.size());
                userContactList.addAll(contactOnEh);
                // Sort by Name.
                Collections.sort(userContactList, new Comparator<UserContact>() {
                    @Override
                    public int compare(UserContact lhs, UserContact rhs) {
                        if (lhs.name == null || rhs.name == null) {
                            return 0;
                        }
                        return lhs.name.compareTo(rhs.name);
                    }
                });*/
            }
            return new ProfileInfo(profileName, profileLastCity, profilePic, email, movieUserReviewObjects,
                    events, new MyEventsRequest.MeEventFavouriteObject(favouriteTopicEvents, favouriteMovie), friendList);
        } catch (JSONException jse) {
            Log.e("ProfileInfo Json Parse", jse.toString());
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(emptyIfNull(profileId));
        dest.writeString(emptyIfNull(name));
        dest.writeString(emptyIfNull(lastCity));
        dest.writeString(emptyIfNull(profilePic));
        dest.writeString(emptyIfNull(email));
        dest.writeTypedList(movieUserReviewObjectArrayList);
        dest.writeTypedList(myInterestEvents);
        dest.writeParcelable(meEventFavouriteObject, flags);
        dest.writeTypedList(friendList);
    }

    private static String emptyIfNull(@Nullable String string) {
        return (string == null ? "" : string);
    }

    public static final Parcelable.Creator<ProfileInfo> CREATOR =
            new Parcelable.Creator<ProfileInfo>() {
                public ProfileInfo createFromParcel(Parcel in) {
                    return new ProfileInfo(in);
                }

                public ProfileInfo[] newArray(int size) {
                    return new ProfileInfo[size];
                }
            };

}
