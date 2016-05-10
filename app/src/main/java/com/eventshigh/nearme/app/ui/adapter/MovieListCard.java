package com.eventshigh.nearme.app.ui.adapter;

import android.content.Intent;
import android.media.Image;
import android.os.Build;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.MovieDetailActivity;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.utils.DateTimeUtils;

import java.util.ArrayList;

/**
 * Created by umesh on 09/05/16.
 */
public class MovieListCard extends RecyclerView.ViewHolder {

    ImageView movieBg;
    TextView movieName;
    TextView movieReviews;
    TextView movieGenre;
    LinearLayout parent;
    ImageView share;
    ImageView favourite;
    TextView movieReleaseDate;
    TextView languages;

    public static MovieListCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_movie, parent, false);
        return new MovieListCard(view);
    }

    public MovieListCard(View itemView) {
        super(itemView);
        movieBg = (ImageView) itemView.findViewById(R.id.movie_bg);
        movieName = (TextView) itemView.findViewById(R.id.movie_name);
        movieReviews = (TextView) itemView.findViewById(R.id.reviews);
        parent = (LinearLayout) itemView.findViewById(R.id.parent);
        movieGenre = (TextView) itemView.findViewById(R.id.movie_genre);
        share = (ImageView) itemView.findViewById(R.id.share);
        favourite = (ImageView) itemView.findViewById(R.id.action_favourite);
        movieReleaseDate = (TextView) itemView.findViewById(R.id.movie_release_date);
        languages = (TextView)itemView.findViewById(R.id.languages);
    }

    public void bindData(final MovieDetailObject movie, final BaseActivity activity) {
        Glide.with(activity).load(movie.getMovieInfo().getImg_url())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                .into(movieBg);
        StringBuilder string = new StringBuilder();
        string.append(getShortenedMovieName(movie.getMovieInfo().getName()));
        if (movie.getMovieInfo().getCertification() != null && movie.getMovieInfo().getCertification().length() > 0) {
            string.append(" ( " + movie.getMovieInfo().getCertification() + " ) ");
        }
        movieName.setText(string);
        if (movie.getReviews().size() > 0) {
            movieReviews.setVisibility(View.VISIBLE);
            movieReviews.setText(movie.getReviews().size() + "Reviews");
        } else {
            movieReviews.setVisibility(View.GONE);
        }
        languages.setText(getCategoryText(movie.getMovieInfo().getLaunguages(),","));
        movieGenre.setText(getCategoryText(movie.getMovieInfo().getGenre(),"|"));
        movieReleaseDate.setText(DateTimeUtils.getMovieShowDate(movie.getMovieInfo().getRelease_date()));
        parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, MovieDetailActivity.class);
                intent.putExtra("movie", movie);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ActivityOptionsCompat options = ActivityOptionsCompat.
                            makeSceneTransitionAnimation(activity, movieBg, activity.getString(R.string.activity_movie_trans));

                    activity.startActivity(intent, options.toBundle());
                } else {
                    activity.startActivity(intent);
                }


            }
        });
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.shareMovie(movie);
            }
        });

        favourite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });


    }

    public String getCategoryText(ArrayList<String> names,String separator) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (builder.length() > 0) {
                builder.append(" "+separator+" ");
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
        if(position!=-1){
           return movieName.substring(0,position);
        }
        return movieName;
    }
}
