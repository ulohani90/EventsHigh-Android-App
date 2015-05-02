package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.EventsMarkerManager.Editor;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.data.UserActionDbHelper;
import com.eventshigh.nearme.app.task.FetchLocalityTask;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.google.android.gms.maps.model.LatLng;

/**
 * An abstact class for activity with context.
 */
public abstract class BaseContextActivity extends BaseActivity
        implements FetchLocalityTask.Listener {
    private static final String LOG_TAG = BaseContextActivity.class.getSimpleName();

    protected EventsContext eventsContext;
    protected Editor eventsMarkerEditor;

    protected View topProgressBar;
    protected View retryView;

    @Override
    protected void onStart() {
        super.onStart();

        // Initialize the EventsMarkerManager.Editor.
        eventsMarkerEditor = EventsMarkerManager.getInstance(this).getEditor();
    }

    @Override
    protected void onStop() {
        eventsMarkerEditor.close();

        super.onStop();
    }

    protected abstract boolean isDataShown();

    public LatLng getUserLocation() {
        return eventsContext.location;
    }

    public @Nullable
    EventMark getEventMark(Event event) {
        return eventsMarkerEditor.getEventsMarkerManager().getEventMark(event.id);
    }

    public void recordEventMark(Event event, @Nullable EventMark mark) {
        if (EventMark.isFavourite(mark)) {
            showMyEventsClue(event);
        } else {
            hideMyEventsClue();
        }

        eventsMarkerEditor.recordEventMark(event, mark);
    }

    public void reportEventAction(Event event, String actionName, int position) {
        reportActionToAnalytics(actionName,
                eventsContext.dateFilter,
                1,
                isFavourite(event) ? "Favourite" : "No-Favourite",
                event.ehRecommended ? "Recommended" : "Non-Recommended",
                eventsContext.query.isEmpty() ? " " : eventsContext.query,
                Integer.toString(position));
    }

    public void showSearchView(String query) {
        reportActionToAnalytics("showSearchView", query);
        EventsContext param = new EventsContext(eventsContext.location, query);
        Intent intent = new Intent(this, this.getClass())
                .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, param);
        startActivity(intent);
    }

    public void showMyEventsClue(@Nullable Event event) {
        // do nothing.
    }

    public void hideMyEventsClue() {
        // do nothing.
    }

    public void showEventDetails(Uri eventDetailsURI) {
        reportActionToAnalytics("showEventDetails");
        Intent detailIntent = new Intent(this, EventDetailActivity.class);
        detailIntent.setData(eventDetailsURI);
        startActivity(detailIntent);
    }

    public void showEventDetails(Event event, @Nullable Bundle bundle) {
        reportEventAction(event, "showEventDetails", event.id);
        UserActionDbHelper.getInstance(this).recordAction(
            UserActionDbHelper.EventAction.OPEN_EVENT_DETAIL, event.id);
        Intent detailIntent = new Intent(this, EventDetailActivity.class);
        detailIntent.putExtra(EventDetailActivity.EXTRA_EVENT_PARAM, event);
        startActivity(detailIntent, bundle);
    }

    protected ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);
            if (isDataShown()) {
                Toast.makeText(BaseContextActivity.this, R.string.failed_refresh, Toast.LENGTH_SHORT).show();
            } else {
                retryView.setVisibility(View.VISIBLE);
            }

            Throwable cause = volleyError.getCause();
            if (cause != null) {
                Log.w(LOG_TAG, "Volley Error: " + volleyError.getMessage(), cause);
                reportActionToAnalytics("failedRequest", cause.getClass().getSimpleName());
            } else {
                Log.w(LOG_TAG, "Volley Error: " + volleyError.getMessage());
                reportActionToAnalytics("failedRequest");
            }
        }
    };

    @Override
    public void onLocationUpdated(String locality) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && locality != null && !locality.isEmpty()) {
            actionBar.setSubtitle(locality);
        }
    }
}
