package com.eventshigh.nearme.app.ui.adapter;

import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.data.MovieInfoObject;
import com.eventshigh.nearme.app.data.MovieMarkerManager;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.view.JustifyTextView;
import com.eventshigh.nearme.app.view.ZCustomFlowLayout;

import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * Created by umesh on 06/05/16.
 */
public class MovieInfoCard extends RecyclerView.ViewHolder {

    TextView movieCertificate, movieName, movieReviews, movieCategory, movieDirector;
    TextView movieSynopsis;
    TextView favouriteView, favouritedView;
    ZCustomFlowLayout leadCastFlowLayout;

    public static MovieInfoCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_movie_info, parent, false);
        return new MovieInfoCard(view);
    }

    public MovieInfoCard(View view) {
        super(view);
        //movieCertificate = (TextView) view.findViewById(R.id.movie_certificate);
        movieName = (TextView) view.findViewById(R.id.movie_name);
        movieCategory = (TextView) view.findViewById(R.id.movie_category);
        movieSynopsis = (TextView) view.findViewById(R.id.movie_synopsis);
        movieDirector = (TextView) view.findViewById(R.id.movie_director);
        favouriteView = (TextView) itemView.findViewById(R.id.action_favourite);
        favouritedView = (TextView) itemView.findViewById(R.id.action_favourited);
        leadCastFlowLayout = (ZCustomFlowLayout) view.findViewById(R.id.lead_cast_flowlayout);
    }


    public void bindData(final MovieInfoObject movie, final BaseContextActivity activity) {

        StringBuilder name = new StringBuilder();
        name.append(getShortenedMovieName(movie.getName()));
        if (movie.getCertification() != null && movie.getCertification().length() > 0) {
            name.append(" ( " + movie.getCertification() + " ) ");
        }
        movieName.setText(name);
        String category = getCategoryText(movie.getGenre());
        movieCategory.setText(category);
        movieSynopsis.setText(Html.fromHtml("<p>" + movie.getSynopsis().replaceAll("\\r", "<br>").replaceAll("\\r", "<br>") + "</p>"));

        if (movie.getDirector() != null && movie.getDirector().length() > 0) {
            movieDirector.setVisibility(View.VISIBLE);
            SpannableString string = new SpannableString("Director: " + movie.getDirector());
            string.setSpan(new StyleSpan(Typeface.BOLD), 0, 9, 0);
            movieDirector.setText(string);
        } else {
            movieDirector.setVisibility(View.GONE);
        }

        leadCastFlowLayout.setReceipentsForMoVieCasts(movie.getCast(), false);
        favouriteView.setVisibility(View.VISIBLE);
        setFavouriteView(MovieMarkerManager.MovieMark.isFavourite(activity.getMovieMark(movie)));
        favouriteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v != null) {
                    activity.reportMovieAction(movie, "addMovieFavourite", movie.getName());

                    Account account = new Account(activity);
                    if (!account.getUserInfo().isVerified) {
                        PhoneVerificationDialog.show(activity, R.string.ui_verify_phone, R.string.ui_phone_verify_pa);
                    }
                }

                MovieMarkerManager.Editor eventsMarkerEditor =
                        MovieMarkerManager.getInstance(activity).getEditor();
                eventsMarkerEditor.recordMovieMark(movie, MovieMarkerManager.MovieMark.FAVOURITE);
                setFavouriteView(true);
                eventsMarkerEditor.close();

            }
        });

        favouritedView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportMovieAction(movie, "removeMovieFavourite", movie.getName());

                MovieMarkerManager.Editor movieMarkerEditor =
                        MovieMarkerManager.getInstance(activity).getEditor();
                movieMarkerEditor.recordMovieMark(movie, null);
                setFavouriteView(false);
                movieMarkerEditor.close();
            }
        });

    }

    private void setFavouriteView(boolean isFavourite) {
        favouritedView.setVisibility(isFavourite ? View.VISIBLE : View.GONE);
        favouriteView.setVisibility(isFavourite ? View.GONE : View.VISIBLE);
    }


    public String getCategoryText(ArrayList<String> names) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(names.get(i));
        }
        return builder.toString();
    }


    public String getShortenedMovieName(String movieName) {
        int position = -1;
        for (int i = movieName.length() - 1; i >= 0; i--) {
            if (movieName.charAt(i) == '(') {
                position = i;
                break;
            }
        }
        if (position != -1) {
            return movieName.substring(0, position);
        }
        return movieName;
    }


}
