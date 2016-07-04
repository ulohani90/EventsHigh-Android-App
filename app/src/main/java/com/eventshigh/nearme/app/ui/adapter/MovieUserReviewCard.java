package com.eventshigh.nearme.app.ui.adapter;

import android.media.Image;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.MovieUserReviewObject;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.view.CircularImageView;

/**
 * @author shubham
 * @since 16/5/16.
 */
public class MovieUserReviewCard extends RecyclerView.ViewHolder {
    TextView tvReviewText, tvReviewBy;
    ImageView ivReviewerImage;
    TextView tvReviewFor;
    RatingBar reviewRating;

    public static MovieUserReviewCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_user_movie_review, parent, false);
        return new MovieUserReviewCard(view);
    }

    public MovieUserReviewCard(View itemView) {
        super(itemView);
        tvReviewText = (TextView) itemView.findViewById(R.id.tv_user_review_text);
        tvReviewBy = (TextView) itemView.findViewById(R.id.tv_user_review_by);
        ivReviewerImage = (ImageView) itemView.findViewById(R.id.civ_user_review);
        reviewRating = (RatingBar) itemView.findViewById(R.id.rb_user_review_rating);
        tvReviewFor = (TextView) itemView.findViewById(R.id.tv_user_review_for);
    }

    public void bindData(final BaseContextActivity activity, final MovieUserReviewObject review, String reviewForId) {
        if(review.getReviewText()!=null)
            tvReviewText.setText(review.getReviewText().trim());
        tvReviewBy.setText(review.getReviewBy());
        int size = ivReviewerImage.getLayoutParams().height;
        ivReviewerImage.setImageDrawable(UserContact.getDrawableForName(review.getReviewBy(), size));
        reviewRating.setRating(review.getReviewRating());
        if ((review.getReviewedEntityId() == null || !reviewForId.equalsIgnoreCase(review.getReviewedEntityId())) && review.getReviewEntity() != null) {
            tvReviewFor.setVisibility(View.VISIBLE);
            tvReviewFor.setText("This review was for " + review.getReviewEntity());
        } else {
            tvReviewFor.setVisibility(View.GONE);
        }
    }

}
