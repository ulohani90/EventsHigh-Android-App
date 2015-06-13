package com.eventshigh.nearme.app.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.user.Account;
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
public abstract class BaseContextActivity extends BaseActivity {
    protected EventsContext eventsContext;
    protected EventsMarkerManager eventsMarkerManager;

    protected Toolbar toolbar;

    // GoogleApiClient to report the page view.
    private GoogleApiClient client;

    @Override
    protected void onStart() {
        super.onStart();

        // Initialize the EventsMarkerManager.Editor.
        eventsMarkerManager = EventsMarkerManager.getInstance(this);

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

        // Show the verify phone snakbar if needed.
        showVerifyPhoneSnackbar();
    }

    @Override
    protected void onStop() {
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_show_map) {
            reportActionToAnalytics("switchToMaps");
            switchTo(EventsMapsActivity.class);
            return true;
        }

        if (id == R.id.action_show_list) {
            reportActionToAnalytics("switchToList");
            switchTo(EventsGridActivity.class);
            return true;
        }

        if (id == R.id.action_share) {
            reportActionToAnalytics("shareEvents", eventsContext.getLabel());

            String uri = EventsHighEndpoints.getWebUri(eventsContext).buildUpon()
                    .appendQueryParameter("src", "ehm").toString();
            try {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, eventsContext.toString() + "\n\n" + uri);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            } catch (ActivityNotFoundException e) {
                Crashlytics.getInstance().core.logException(e);
                Snackbar.make(this.getViewForSnackbar(), R.string.failed_share, Snackbar.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void switchTo(Class<?> cls) {
        reportActionToAnalytics("switchView");
        Intent intent = new Intent(this, cls)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, eventsContext);
        startActivity(intent);
    }

    public LatLng getUserLocation() {
        return eventsContext.location;
    }

    public void showActionBar() {
        if (toolbar.getVisibility() == View.VISIBLE) {
            return;
        }

        toolbar.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        final int targetHeight = toolbar.getMeasuredHeight();

        toolbar.getLayoutParams().height = 0;
        toolbar.setVisibility(View.VISIBLE);
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                toolbar.getLayoutParams().height = interpolatedTime == 1
                        ? LayoutParams.WRAP_CONTENT
                        : (int)(targetHeight * interpolatedTime);
                toolbar.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        animation.setDuration(200);
        toolbar.startAnimation(animation);
    }

    public void hideActionBar() {
        if (toolbar.getVisibility() == View.GONE) {
            return;
        }

        final int initialHeight = toolbar.getMeasuredHeight();
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1){
                    toolbar.setVisibility(View.GONE);
                }else{
                    toolbar.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
                    toolbar.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        animation.setDuration(200);
        toolbar.startAnimation(animation);
    }

    protected void showVerifyPhoneSnackbar() {
        boolean isVerificationPending = Account.isPhoneVerifyPending(this);
        final View view = findViewById(R.id.verify_phone_container);
        view.setVisibility(isVerificationPending ? View.VISIBLE :View.GONE);
        view.findViewById(R.id.verify_phone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BaseContextActivity.this, PhoneLoginActivity.class));
                view.setVisibility(View.GONE);
            }
        });
        view.findViewById(R.id.verify_phone_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Account.disablePhoneVerifySnackbar();
                view.setVisibility(View.GONE);
            }
        });
    }

    public boolean isFavourite(Event event) {
        return eventsMarkerManager.isFavourite(event.id);
    }

    public @Nullable
    EventMark getEventMark(Event event) {
        return eventsMarkerManager.getEventMark(event.id);
    }

    public void recordEventMark(Event event, @Nullable EventMark mark) {
        eventsMarkerManager.getEditor().recordEventMark(event, mark).close();
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
        param.dateFilter = eventsContext.dateFilter;
        Intent intent = new Intent(this, getClass())
                .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, param);
        startActivity(intent);
    }

    public void showEventDetails(Uri eventDetailsURI, @Nullable String label) {
        reportActionToAnalytics("showEventDetails", label);
        Intent detailIntent = new Intent(this, EventDetailActivity.class);
        detailIntent.setData(eventDetailsURI);
        startActivity(detailIntent);
    }

    public void showEventDetails(Event event, @Nullable String label, @Nullable Bundle bundle) {
        reportEventAction(event, "showEventDetails", label);
        Intent detailIntent = new Intent(this, EventDetailActivity.class);
        detailIntent.putExtra(EventDetailActivity.EXTRA_EVENT_PARAM, event);
        startActivity(detailIntent, bundle);
    }

    @Override
    public View getViewForSnackbar() {
        return toolbar;
    }
}
