package com.eventshigh.nearme.app.data;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author shubham
 * @since 1/7/16.
 */

public class ProfileInfo {

    private String profileId;
    private ArrayList<MovieUserReviewObject> movieUserReviewObjectArrayList;
    private ArrayList<MyEventsRequest.TopicEvents> myInterestEvents;

    //getter and setters
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



    public ProfileInfo(ArrayList<MovieUserReviewObject> movieUserReviewObjects, ArrayList<MyEventsRequest.TopicEvents> myInterestObjectList) {
        this.movieUserReviewObjectArrayList = movieUserReviewObjects;
        this.myInterestEvents = myInterestObjectList;
    }

    public static ProfileInfo fromJson(JSONObject jsonObject, Context context) {
        ArrayList<MyEventsRequest.TopicEvents> events = new ArrayList<>();
        ArrayList<MovieUserReviewObject> movieUserReviewObjects = new ArrayList<>();

        try {
            if (false && jsonObject.has("interest_events")) {
                JSONArray eventsJsonArray = jsonObject.getJSONArray("interest_events");

                for (int i = 0; i < eventsJsonArray.length(); i++) {

                    if (eventsJsonArray.getJSONObject(i).has("topic_events")) {
                    List<Event> topicEvents = Event.fromJSON(eventsJsonArray.getJSONObject(i).getJSONArray("topic_events"), true);
                    new Account(context).setIsFollowing(eventsJsonArray.getJSONObject(i).getString("topic"), true);
                    MyEventsRequest.TopicEvents eventData = new MyEventsRequest.TopicEvents(eventsJsonArray.getJSONObject(i).getString("topic"), topicEvents, eventsJsonArray.getJSONObject(i).getInt("event_count"));
                    events.add(eventData);
                    }
                }
            }

            JSONArray reviews = jsonObject.getJSONArray("reviews");
            if (reviews != null) {
                for (int i = 0; i < reviews.length(); i++) {
                    movieUserReviewObjects.add(new MovieUserReviewObject(reviews.getJSONObject(i)));
                }
            }
            return new ProfileInfo(movieUserReviewObjects,events);
        } catch (JSONException jse) {
            Log.e("ProfileInfo Json Parse", jse.toString());
        }
        return null;

    }
}
