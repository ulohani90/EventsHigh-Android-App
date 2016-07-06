package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.data.MovieMarkerManager;
import com.eventshigh.nearme.app.data.MovieUserReviewObject;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.Utils;

/**
 * @author shubham
 * @since 27/6/16.
 */
public class MyReviewCard extends RecyclerView.ViewHolder {

    ImageView entityImage;
    TextView reviewText;
    TextView entityName, entityLocation;
    TextView reviewedTime;
    RatingBar entityRating;
    ImageView favouriteButton, favouritedButton, shareButton;
    static BaseActivity baseActivity;

    public static MyReviewCard newInstance(final BaseActivity activity, ViewGroup parent) {
        baseActivity = activity;
        View view = activity.getLayoutInflater().inflate(R.layout.card_my_review, parent, false);
        return new MyReviewCard(view);
    }

    public MyReviewCard(View view) {
        super(view);
        entityImage = (ImageView) view.findViewById(R.id.reviewed_entity_image);
        entityLocation = (TextView) view.findViewById(R.id.reviewed_entity_location);
        entityName = (TextView) view.findViewById(R.id.reviewed_entity_name);
        reviewedTime = (TextView) view.findViewById(R.id.reviewed_time);
        reviewText = (TextView) view.findViewById(R.id.review_text);
        entityRating = (RatingBar) view.findViewById(R.id.entity_rating);
        favouriteButton = (ImageView) view.findViewById(R.id.reviewed_entity_add_fav);
        favouritedButton = (ImageView) view.findViewById(R.id.reviewed_entity_remove_fav);
        shareButton = (ImageView) view.findViewById(R.id.reviewed_entity_share);
    }

    public void bindData(final MovieUserReviewObject movieUserReviewObject) {
        entityName.setText(Utils.capitalize(movieUserReviewObject.getReviewEntity()));
        reviewText.setText(movieUserReviewObject.getReviewText());
        entityRating.setRating(movieUserReviewObject.getReviewRating());
        Glide.with(baseActivity).load(movieUserReviewObject.getReviewedEntityImage())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_launcher).crossFade().centerCrop()
                .into(entityImage);
        if (!Utils.checkIfStringEmpty(movieUserReviewObject.getReviewedEntityLocation())) {
            entityLocation.setText(Utils.capitalize(movieUserReviewObject.getReviewedEntityLocation()));
        } else {
            entityLocation.setVisibility(View.GONE);
        }
        if (movieUserReviewObject.getCreatedAt() > 0) {
            reviewedTime.setText(DateTimeUtils.getPastTime(movieUserReviewObject.getCreatedAt()) + "ago");
        } else {
            reviewedTime.setVisibility(View.GONE);
        }
        if (movieUserReviewObject.getReviewFor().equalsIgnoreCase("movie")) {
            if (isMovieFavourite(movieUserReviewObject.getReviewedEntityId()))
                favouritedButton.setVisibility(View.VISIBLE);
            else
                favouritedButton.setVisibility(View.GONE);

            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    baseActivity.shareMovie(movieUserReviewObject.getMovieDetailObject());
                }
            });
            if (isMovieFavourite(movieUserReviewObject.getReviewedEntityId())) {

            }
            favouriteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addMovieFavourite(movieUserReviewObject.getMovieDetailObject());
                }
            });
            favouritedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeMovieFavourite(movieUserReviewObject.getMovieDetailObject());
                }
            });
        } else if (movieUserReviewObject.getReviewFor().equalsIgnoreCase("event")) {
            if (isEventFavourite(movieUserReviewObject.getReviewedEntityId()))
                favouritedButton.setVisibility(View.VISIBLE);
            else
                favouritedButton.setVisibility(View.GONE);

            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    baseActivity.shareEventWithBranch(movieUserReviewObject.getEvent(), null, "MyReviews");
                }
            });
            favouriteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addEventFavourite(movieUserReviewObject.getEvent());
                }
            });
            favouritedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeEventFavourite(movieUserReviewObject.getEvent());
                }
            });
        } else {
            shareButton.setVisibility(View.GONE);
            favouriteButton.setVisibility(View.GONE);
            favouritedButton.setVisibility(View.GONE);
        }
    }

    //util methods for favourite event and movie
    private boolean isEventFavourite(String id) {
        return EventsMarkerManager.getInstance(baseActivity).isFavourite(id);
    }

    private boolean isMovieFavourite(String id) {
        return MovieMarkerManager.getInstance(baseActivity).isFavourite(id);
    }

    private void removeEventFavourite(Event event) {
        baseActivity.reportEventAction(event, "removeFavourite");

        EventsMarkerManager.Editor eventsMarkerEditor =
                EventsMarkerManager.getInstance(baseActivity).getEditor();
        eventsMarkerEditor.recordEventMark(event, null);
        setFavouriteView(false);
        eventsMarkerEditor.close();
    }

    private void addEventFavourite(Event event) {
        baseActivity.reportEventAction(event, "addFavourite");

        Account account = new Account(baseActivity);
        if (!account.getUserInfo().isVerified) {
            PhoneVerificationDialog.show(baseActivity, R.string.ui_verify_phone, R.string.ui_phone_verify_pa);
        }
        EventsMarkerManager.Editor eventsMarkerEditor =
                EventsMarkerManager.getInstance(baseActivity).getEditor();
        eventsMarkerEditor.recordEventMark(event, EventsMarkerManager.EventMark.FAVOURITE);
        setFavouriteView(true);
        eventsMarkerEditor.close();
    }

    private void removeMovieFavourite(MovieDetailObject movie) {
        baseActivity.reportMovieAction(movie.getMovieInfo(), "removeMovieFavourite", movie.getMovieInfo().getName());

        MovieMarkerManager.Editor movieMarkerEditor =
                MovieMarkerManager.getInstance(baseActivity).getEditor();
        movieMarkerEditor.recordMovieMark(movie.getMovieInfo(), null);
        setFavouriteView(false);
        movieMarkerEditor.close();
    }

    private void addMovieFavourite(MovieDetailObject movie) {
        baseActivity.reportMovieAction(movie.getMovieInfo(), "addMovieFavourite", movie.getMovieInfo().getName());

        Account account = new Account(baseActivity);
        if (!account.getUserInfo().isVerified) {
            PhoneVerificationDialog.show(baseActivity, R.string.ui_verify_phone, R.string.ui_phone_verify_pa);
        }

        MovieMarkerManager.Editor eventsMarkerEditor =
                MovieMarkerManager.getInstance(baseActivity).getEditor();
        eventsMarkerEditor.recordMovieMark(movie.getMovieInfo(), MovieMarkerManager.MovieMark.FAVOURITE);
        setFavouriteView(true);
        eventsMarkerEditor.close();
    }


    private void setFavouriteView(boolean isFavourite){
        favouritedButton.setVisibility(isFavourite ? View.VISIBLE : View.GONE);
        favouriteButton.setVisibility(isFavourite ? View.GONE : View.VISIBLE);
    }

}
