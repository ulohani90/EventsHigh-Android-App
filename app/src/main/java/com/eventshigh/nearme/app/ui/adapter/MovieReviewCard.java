package com.eventshigh.nearme.app.ui.adapter;

import android.media.Image;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.data.MovieReviewObject;
import com.eventshigh.nearme.app.view.CircularImageView;

/**
 * Created by umesh on 05/05/16.
 */
public class MovieReviewCard extends RecyclerView.ViewHolder{


    TextView reviewTitle,reviewDesc,reviewSource;
    CircularImageView imageView;
    LinearLayout parentLayout;

    public static MovieReviewCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_movie_review, parent, false);
        return new MovieReviewCard(view);
    }

    public MovieReviewCard(View itemView) {
        super(itemView);
        reviewTitle = (TextView)itemView.findViewById(R.id.source_title);
        reviewDesc = (TextView)itemView.findViewById(R.id.source_desc);
        reviewSource = (TextView)itemView.findViewById(R.id.source_name);
        imageView =(CircularImageView)itemView.findViewById(R.id.source_img);
        parentLayout = (LinearLayout)itemView.findViewById(R.id.review_parent);
    }



    public void bindData(final BaseContextActivity activity, final MovieReviewObject review){

        Glide.with(activity).load(review.getImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_launcher).crossFade().centerCrop()
                .into(imageView);

        reviewTitle.setText(Html.fromHtml(review.getReviewTitle()));
        reviewDesc.setText(review.getReviewBlob());
        if(review.getReviewerName()!=null) {
            SpannableString sourceText = new SpannableString("Source: " + review.getReviewerName());
            sourceText.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.primary)), 0, 7, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE);
            reviewSource.setText(sourceText);
        }else{
            reviewSource.setVisibility(View.GONE);
        }

        parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showCustomUrlActivity(review.getSourceUrl(),review.getReviewerName());
            }
        });


    }
}
