package com.eventshigh.nearme.app.data;

import android.content.Context;
import android.graphics.Movie;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.TimeUtils;

import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by umesh on 04/05/16.
 */
public class MovieDetailObject implements Parcelable {

    public MovieDetailObject(Parcel in) {
        movieInfo = in.readParcelable(MovieInfoObject.class.getClassLoader());
        reviews = new ArrayList<>();
        in.readTypedList(reviews, MovieReviewObject.CREATOR);
        showtimes = new ArrayList<>();
        in.readTypedList(showtimes, MovieShowTimeObject.CREATOR);
        userReviews = new ArrayList<>();
        in.readTypedList(userReviews, MovieUserReviewObject.CREATOR);
    }

    private MovieInfoObject movieInfo;

    private ArrayList<MovieReviewObject> reviews;

    private ArrayList<MovieShowTimeObject> showtimes;

    private ArrayList<MovieUserReviewObject> userReviews;


    public MovieDetailObject(Context context, JSONObject obj) {
        try {
            movieInfo = new MovieInfoObject(obj);
            this.reviews = new ArrayList<>();
            if (obj.has("movie_reviews")) {
                JSONArray reviewsArray = obj.getJSONArray("movie_reviews");
                if (reviewsArray != null) {
                    for (int i = 0; i < reviewsArray.length(); i++) {
                        reviews.add(new MovieReviewObject(reviewsArray.getJSONObject(i)));
                    }
                }
            }

            this.showtimes = new ArrayList<>();
            if (obj.has("movie_showtimes")) {
                JSONObject showTimesObject = obj.getJSONObject("movie_showtimes");
                String cityName = new Account(context).getLastCity().name();
                if (showTimesObject.has(cityName.toLowerCase())) {
                    JSONArray showTimesArray = showTimesObject.getJSONArray(cityName.toLowerCase());
                    if (showTimesArray != null) {
                        for (int i = 0; i < showTimesArray.length(); i++) {
                            MovieShowTimeObject showTimeObject = new MovieShowTimeObject(showTimesArray.getJSONObject(i));
                            if (showTimeObject.getShowDates() != null && showTimeObject.getShowDates().size() > 0) {
                                showtimes.add(showTimeObject);
                            }
                        }
                    }
                }
            }

            this.userReviews = new ArrayList<>();
            if (obj.has("reviews")) {
                JSONArray userReviewsArray = obj.getJSONArray("reviews");
                if (userReviewsArray != null) {
                    for (int i = 0; i < userReviewsArray.length(); i++) {
                        userReviews.add(new MovieUserReviewObject(userReviewsArray.getJSONObject(i)));
                    }
                }
            }


        } catch (JSONException e) {

        }
    }

    public static final Creator<MovieDetailObject> CREATOR = new Creator<MovieDetailObject>() {
        @Override
        public MovieDetailObject createFromParcel(Parcel in) {
            return new MovieDetailObject(in);
        }

        @Override
        public MovieDetailObject[] newArray(int size) {
            return new MovieDetailObject[size];
        }
    };

    public MovieInfoObject getMovieInfo() {
        return movieInfo;
    }

    public void setMovieInfo(MovieInfoObject movieInfo) {
        this.movieInfo = movieInfo;
    }

    public ArrayList<MovieReviewObject> getReviews() {
        return reviews;
    }

    public void setReviews(ArrayList<MovieReviewObject> reviews) {
        this.reviews = reviews;
    }

    public ArrayList<MovieUserReviewObject> getUserReviews() {
        return userReviews;
    }

    public void setUserReviews(ArrayList<MovieUserReviewObject> userReviews) {
        this.userReviews = userReviews;
    }


    public ArrayList<MovieShowTimeObject> getShowtimes() {
        return showtimes;
    }

    public void setShowtimes(ArrayList<MovieShowTimeObject> showtimes) {
        this.showtimes = showtimes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(movieInfo, flags);
        dest.writeTypedList(reviews);

        dest.writeTypedList(showtimes);
        dest.writeTypedList(userReviews);
    }


}
