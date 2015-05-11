package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;

import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.EventsMarkerManager.Editor;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.data.UserActionDbHelper;
import com.eventshigh.nearme.app.task.FetchLocalityTask;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.maps.model.LatLng;

/**
 * An abstact class for activity with context.
 */
public abstract class BaseContextActivity extends BaseActivity
        implements FetchLocalityTask.Listener {
    protected EventsContext eventsContext;
    protected Editor eventsMarkerEditor;

    // GoogleApiClient to report the page view.
    private GoogleApiClient client;

    @Override
    protected void onStart() {
        super.onStart();

        // Initialize the EventsMarkerManager.Editor.
        eventsMarkerEditor = EventsMarkerManager.getInstance(this).getEditor();

        // Setup GoogleApiClient
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API).build();
        client.registerConnectionCallbacks(new ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                if (eventsContext != null) {
                    Uri webUri = EventsHighEndpoints.getWebUri(eventsContext);
                    String title = eventsContext.toString();
                    AppIndex.AppIndexApi.view(client, BaseContextActivity.this, Utils.getAppUri(webUri),
                            title, webUri, null);
                }
            }

            @Override
            public void onConnectionSuspended(int i) {
                // do nothing.
            }
        });
    }

    @Override
    protected void onStop() {
        eventsMarkerEditor.close();

        if (client != null && client.isConnected()) {
            if (eventsContext != null) {
                Uri webUri = EventsHighEndpoints.getWebUri(eventsContext);
                AppIndex.AppIndexApi.viewEnd(client, BaseContextActivity.this, Utils.getAppUri(webUri));
            }
            client.disconnect();
        }

        super.onStop();
    }

    @Override
    public boolean onSearchRequested() {
        reportActionToAnalytics("onSearchRequested");
        Bundle appData = new Bundle();
        appData.putParcelable(IntentUtils.EXTRA_EVENT_CONTEXT, eventsContext);
        startSearch(null, false, appData, false);
        return true;
    }

    public LatLng getUserLocation() {
        return eventsContext.location;
    }

    public @Nullable
    EventMark getEventMark(Event event) {
        return eventsMarkerEditor.getEventsMarkerManager().getEventMark(event.id);
    }

    public void recordEventMark(Event event, @Nullable EventMark mark) {
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

    public void showEventDetails(Uri eventDetailsURI) {
        reportActionToAnalytics("showEventDetails", eventDetailsURI.getLastPathSegment());
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

    @Override
    public void onLocationUpdated(String locality) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && locality != null && !locality.isEmpty()) {
            actionBar.setSubtitle(locality);
        }
    }
}
