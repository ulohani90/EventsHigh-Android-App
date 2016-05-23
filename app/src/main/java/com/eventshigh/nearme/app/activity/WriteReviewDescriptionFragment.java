package com.eventshigh.nearme.app.activity;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.MovieUserReviewObject;
import com.eventshigh.nearme.app.network.MovieReviewSubmitRequest;
import com.eventshigh.nearme.app.network.RecordUserAction;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.CircularImageView;

import org.json.JSONException;
import org.json.JSONObject;

public class WriteReviewDescriptionFragment extends Fragment implements View.OnClickListener {

    static String RATING_COUNT = "rating_count";
    private static String JSON_KEY_REVIEWER_ID = "reviewer_id";//mobi no
    private static String JSON_KEY_REVIEW_ID = "review_id";
    private static String JSON_KEY_REVIEW_FOR = "review_for";
    private static String JSON_KEY_REVIEW_ENTITY = "reviewed_entity";
    private static String JSON_KEY_REVIEW_ENTITY_ID = "reviewed_entity_id";
    private static String JSON_KEY_REVIEW_TEXT = "review_text";
    private static String JSON_KEY_REVIEW_BY = "review_by";
    private static String JSON_KEY_REVIEW_PLATFORM = "review_platform";
    private static String JSON_KEY_REVIEW_DEVICE_ID = "review_device_id";
    private static String JSON_KEY_REVIEW_RATINGS = "ratings";

    AppCompatActivity mAppCompatActivity;
    EditText etWriteReviewDescription;
    TextView btnReviewSubmit;
    CircularImageView ivMoviePicture;
    TextView tvMovieName;
    RatingBar rbMovieRating;
    LinearLayout llReviewDescriptionHeader;
    WriteReviewActivity writeReviewActivity;

    ProgressDialog progress;

    public static WriteReviewDescriptionFragment newInstance(AppCompatActivity appCompatActivity) {
        return new WriteReviewDescriptionFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        writeReviewActivity = (WriteReviewActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_write_review_description, container, false);

        ivMoviePicture = (CircularImageView) rootView.findViewById(R.id.civ_movie_pic);
        tvMovieName = (TextView) rootView.findViewById(R.id.tv_write_review_description_movie_name);
        rbMovieRating = (RatingBar) rootView.findViewById(R.id.rb_write_rating_description);
        llReviewDescriptionHeader = (LinearLayout) rootView.findViewById(R.id.ll_review_description_header);
        etWriteReviewDescription = (EditText) rootView.findViewById(R.id.et_write_review_description);

        btnReviewSubmit = (TextView) rootView.findViewById(R.id.btn_write_review);
        if (writeReviewActivity.movieDetailObject != null) {
            Glide.with(writeReviewActivity).load(writeReviewActivity.movieDetailObject.getMovieInfo().getImg_url())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                    .into(ivMoviePicture);
            tvMovieName.setText(writeReviewActivity.movieDetailObject.getMovieInfo().getName());
        }
        writeReviewActivity.movie_rated =
                (int) writeReviewActivity.writeReviewRatingFragment.rbMovieRating.getRating();
        rbMovieRating.setRating(writeReviewActivity.movie_rated);
        llReviewDescriptionHeader.setOnClickListener(this);
        btnReviewSubmit.setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_write_review:
                if (etWriteReviewDescription != null && etWriteReviewDescription.getText() != null && etWriteReviewDescription.getText().toString().length() > 0) {
                    placeReviewAction();
                } else {
                    showNoDescDialog();
                }


                break;
            case R.id.ll_review_description_header:
                writeReviewActivity.onBackPressed();
                break;
        }

    }

    public void showNoDescDialog() {
        new AlertDialog.Builder(getActivity())
                .setMessage("Do you want to submit your rating without any review?")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override

                    public void onClick(DialogInterface dialog, int which) {
                        placeReviewAction();
                    }
                })
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }


    public void placeReviewAction() {
        try {
            final JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_KEY_REVIEWER_ID, (new Account(writeReviewActivity)).getUserInfo().phoneNo);
            jsonObject.put(JSON_KEY_REVIEW_FOR, "movie");
            jsonObject.put(JSON_KEY_REVIEW_ENTITY_ID, writeReviewActivity.movieDetailObject.getMovieInfo().getId() + "");
            jsonObject.put(JSON_KEY_REVIEW_ENTITY, tvMovieName.getText().toString());
            jsonObject.put(JSON_KEY_REVIEW_RATINGS, (int) rbMovieRating.getRating());
            jsonObject.put(JSON_KEY_REVIEW_TEXT, etWriteReviewDescription.getText().toString());
            jsonObject.put(JSON_KEY_REVIEW_BY, (new Account(writeReviewActivity)).getUserInfo().name);

            jsonObject.put(JSON_KEY_REVIEW_PLATFORM, "android");
            jsonObject.put(JSON_KEY_REVIEW_DEVICE_ID, Settings.Secure.getString
                    (getContext().getContentResolver(), Settings.Secure.ANDROID_ID));

            progress = ProgressDialog.show(getActivity(), null, "Submitting Review.Please Wait...");
            MovieReviewSubmitRequest.submit(writeReviewActivity,
                    jsonObject, Request.Priority.HIGH, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject, boolean b) {
                            if (getActivity() != null) {
                                if (progress != null)
                                    progress.dismiss();
                                Preferences.getInstance(getActivity()).setIsReviewAdded(true);
                                Log.i("Message Success", "true");
                                Toast.makeText(getActivity(), "Your review has been added successfully", Toast.LENGTH_SHORT).show();
                                closeParentActivity();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            if (progress != null)
                                progress.dismiss();
                            Log.i("Message failure", "true" + jsonObject.toString());
                        }
                    });
        } catch (JSONException e) {
            Crashlytics.getInstance().core.logException(e);
        }
    }

    public void closeParentActivity() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                writeReviewActivity.finish();
                // writeReviewActivity.overridePendingTransition(R.anim.animate_slide_down, R.anim.animate_slide_up);
            }
        }, 1000);
    }

}
