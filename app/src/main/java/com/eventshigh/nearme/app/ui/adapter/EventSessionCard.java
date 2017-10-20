package com.eventshigh.nearme.app.ui.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventSession;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.TopCropImageView;

/**
 * Created by umesh on 16/09/16.
 */
public class EventSessionCard extends RecyclerView.ViewHolder {

    TextView timeText;
    TextView sessionTitle;
    TextView sessionDesc;
    TopCropImageView sessionImg;
    TextView learnMore;
    TextView sessionPerformers;


    public static EventSessionCard newInstance(Activity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_session_layout, parent, false);
        return new EventSessionCard(view);
    }

    public EventSessionCard(View itemView) {
        super(itemView);
        timeText = (TextView) itemView.findViewById(R.id.time_text);
        sessionTitle = (TextView) itemView.findViewById(R.id.session_title);
        sessionDesc = (TextView) itemView.findViewById(R.id.session_desc);
        sessionImg = (TopCropImageView) itemView.findViewById(R.id.session_img);
        learnMore = (TextView) itemView.findViewById(R.id.learn_more);
        sessionPerformers = (TextView) itemView.findViewById(R.id.session_performers);

    }

    public void bindData(final EventSession session, final BaseContextActivity activity, final String city) {
        sessionTitle.setText(session.getTitle().trim());
        if (session.getDescription() != null) {
            sessionDesc.setVisibility(View.VISIBLE);
            sessionDesc.setText(session.getDescription().trim());
        } else {
            sessionDesc.setVisibility(View.GONE);
        }
        timeText.setText(DateTimeUtils.getSessionTime(session.getStartTime(), session.getEndTime()));
        if (session.getImageUrl() != null && session.getImageUrl().length() > 0) {
            sessionImg.setVisibility(View.VISIBLE);
            Glide.with(activity).load(session.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.eh_default_event).crossFade()
                    .into(sessionImg);
        } else {
            sessionImg.setVisibility(View.GONE);
        }

        if (session.getRelatedEId() != null && session.getRelatedEId().length() > 0 && !(session.getRelatedEId().equalsIgnoreCase("empty"))) {
            learnMore.setVisibility(View.VISIBLE);
            learnMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.showEventDetails(EventsHighEndpoints.getEventDetailsURI(city, session.getRelatedEId()), null);
                }
            });
        } else {
            learnMore.setVisibility(View.GONE);
        }
        if (session.getPerformers() != null && session.getPerformers().length() > 0) {
            sessionPerformers.setVisibility(View.VISIBLE);
            sessionPerformers.setText("With: " + session.getPerformers());

        } else {
            sessionPerformers.setVisibility(View.GONE);
        }
    }
}
