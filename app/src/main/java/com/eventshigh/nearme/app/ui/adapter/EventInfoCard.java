package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.CustomUrlActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.utils.DateTimeUtils;

import java.text.MessageFormat;
import java.util.Date;

/**
 * Created by umesh on 17/06/16.
 */
public class EventInfoCard extends RecyclerView.ViewHolder {


    TextView eventName;
    TextView eventOrganizer, eventTime, timeDetails, venueView, travelTimeView, alsoOnView, addToCalender;
    HorizontalScrollView futureTimesViewGroup;
    LinearLayout futureTimesView, youtubeFragment;
    WebView descriptionView;
    View timeGroupView, eventTimeFirstView;

    public static EventInfoCard newInstance(BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_event_info_layout, parent, false);
        return new EventInfoCard(view);
    }

    public EventInfoCard(View itemView) {
        super(itemView);
        eventName = (TextView) itemView.findViewById(R.id.event_name);
        eventOrganizer = (TextView) itemView.findViewById(R.id.event_organizer);
        eventTime = (TextView) itemView.findViewById(R.id.event_time);
        timeDetails = (TextView) itemView.findViewById(R.id.event_time_details);
        venueView = (TextView) itemView.findViewById(R.id.event_venue);
        alsoOnView = (TextView) itemView.findViewById(R.id.also_on);
        futureTimesViewGroup = (HorizontalScrollView) itemView.findViewById(R.id.event_future_times_hs);
        futureTimesView = (LinearLayout) itemView.findViewById(R.id.event_future_times);
        descriptionView = (WebView) itemView.findViewById(R.id.event_description);
        youtubeFragment = (LinearLayout) itemView.findViewById(R.id.youtube_fragment);
        addToCalender = (TextView) itemView.findViewById(R.id.btn_add_calender);

        travelTimeView = (TextView) itemView.findViewById(R.id.event_travel_time);

        timeGroupView = itemView.findViewById(R.id.event_time_group);
        eventTimeFirstView = itemView.findViewById(R.id.event_time_first);

    }

    public void onBindData(final Event event, final BaseContextActivity activity) {
        eventName.setText(event.title);
        eventOrganizer.setText(event.organizerName);
        DateTimeUtils.EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
        timeGroupView.setVisibility(eventTime == null ? View.GONE : View.VISIBLE);
        if (!event.description.isEmpty()) {
            CustomUrlActivity.setupWebView(descriptionView, activity, false);
            descriptionView.loadData(toHtmlNoFrame(event.description), "text/html; charset=UTF-8", null);
        }
        if (eventTime != null) {
            this.eventTime.setText(eventTime.toString());
            int numDays = DateTimeUtils.getDaysLater(event);
            if (numDays >= 0) {
                timeDetails.setVisibility(View.VISIBLE);
                timeDetails.setText(MessageFormat.format(
                        activity.getString(R.string.event_time_details), numDays));
            } else {
                timeDetails.setVisibility(View.GONE);
            }

            futureTimesView.removeAllViews();
            if (event.eventTimings.size() > 1) {
                for (int i = 1; i < event.eventTimings.size(); i++) {
                    eventTime = DateTimeUtils.getEventTime(event, i);
                    if (eventTime == null) {
                        break;
                    }

                    final Date eventDateCurr = new Date(event.eventTimings.get(i));
                    View timeView = activity.getLayoutInflater().inflate(
                            R.layout.view_event_time, futureTimesView, false);
                    ((TextView) timeView.findViewById(R.id.event_day)).setText(
                            eventTime.day + ", " + eventTime.date);
                    ((TextView) timeView.findViewById(R.id.event_time)).setText(eventTime.time);
                    futureTimesView.addView(timeView);
                    timeView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            activity.addToCalendar(event, eventDateCurr);
                        }
                    });
                }

                alsoOnView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (futureTimesViewGroup.getVisibility() == View.GONE) {
                            futureTimesViewGroup.setVisibility(View.VISIBLE);
                            eventTimeFirstView.setVisibility(View.GONE);
                        } else {
                            futureTimesViewGroup.setVisibility(View.GONE);
                            eventTimeFirstView.setVisibility(View.VISIBLE);
                        }
                    }
                });
            } else {
                alsoOnView.setVisibility(View.GONE);
            }
        }
    }

    private static String toHtmlNoFrame(String html) {
        return "<body>" + html.replaceAll("<iframe.*/iframe>", "") + "</body>";
    }
}
