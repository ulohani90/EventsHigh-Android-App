package com.eventshigh.nearme.app.activity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.data.MovieInfoObject;
import com.eventshigh.nearme.app.data.MovieMarkerManager;
import com.eventshigh.nearme.app.ui.AskForContactsDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.model.LatLng;

/**
 * Base activity for location aware events listing. This class implements common methods to fetch
 * fetch event listings when needed and asking the parent activity to show events as per user
 * interactions.
 * <p/>
 * This class also implements base user interactions like tabs, filters etc.
 */
public abstract class BaseContextActivity extends BaseActivity {
    protected EventsContext eventsContext;
    protected EventsMarkerManager eventsMarkerManager;
    protected MovieMarkerManager moviesMarkerManager;

    protected Toolbar toolbar;

    // GoogleApiClient to report the page view.
    private GoogleApiClient client;
    private Action viewAction;

    @Override
    protected void onStart() {
        super.onStart();

        // Initialize the EventsMarkerManager.Editor.
        eventsMarkerManager = EventsMarkerManager.getInstance(this);
        moviesMarkerManager = MovieMarkerManager.getInstance(this);

        // Setup GoogleApiClient
        if (eventsContext != null) {
            Uri webUri = EventsHighEndpoints.getWebUri(eventsContext);
            String title = eventsContext.toString();
            viewAction = new Action.Builder(Action.TYPE_VIEW)
                    .setObject(new Thing.Builder()
                            .setName(title)
                            .setId(webUri.toString())
                            .setUrl(Utils.getAppUri(webUri))
                            .build())
                    .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                    .build();

            client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
            client.connect();
            AppIndex.AppIndexApi.start(client, viewAction);
        }

        // Show the verify phone snakbar if needed.
        showVerifyPhoneSnackbar();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (client != null && client.isConnected()) {
            if (viewAction != null) {
                AppIndex.AppIndexApi.end(client, viewAction);
            }
            client.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Upload contacts

        //AskForContactsDialog.show(BaseContextActivity.this, Preferences.getInstance(this));
        Intent inIntent = getIntent();
        if (eventsContext.city != null && toolbar != null &&
                (inIntent == null || !Intent.ACTION_VIEW.equals(inIntent.getAction()))) {
            toolbar.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isRunning()) {
                        AskForContactsDialog.doNeedful(BaseContextActivity.this);
                    }
                }
            }, 5000);
        }
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
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request missing location permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
            } else {
                reportActionToAnalytics("switchToMaps");
                switchTo(EventsMapsActivity.class);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View getViewForSnackbar() {
        return toolbar;
    }

    protected void setupLayout(@LayoutRes int layoutResID) {
        // Setup the UI.
        setContentView(layoutResID);

        // Setup action bar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // See if we have context passed to us within intent.
        eventsContext = IntentUtils.processIntent(this, getIntent());
    }

    protected void setTitle() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            String title = DateTimeUtils.queryToTitle(eventsContext.query);
            if (!eventsContext.dateFilter.isEmpty()) {
                title += " on " + DateTimeUtils.queryToTitle(eventsContext.dateFilter);
            }
            actionBar.setTitle(Utils.capitalize(title));
        }
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
                        : (int) (targetHeight * interpolatedTime);
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
                if (interpolatedTime == 1) {
                    toolbar.setVisibility(View.GONE);
                } else {
                    toolbar.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
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
        view.setVisibility(isVerificationPending ? View.VISIBLE : View.GONE);
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

    public
    @Nullable
    EventMark getEventMark(Event event) {
        return eventsMarkerManager.getEventMark(event.id);
    }

    public void recordEventMark(Event event, @Nullable EventMark mark) {
        eventsMarkerManager.getEditor().recordEventMark(event, mark).close();
    }

    public void recordMovieMark(MovieInfoObject movie, @Nullable MovieMarkerManager.MovieMark mark) {
        moviesMarkerManager.getEditor().recordMovieMark(movie, mark).close();
    }

    public boolean isMovieFavourite(MovieInfoObject movie) {
        return moviesMarkerManager.isFavourite(movie.getId() + "");
    }

    public
    @Nullable
    MovieMarkerManager.MovieMark getMovieMark(MovieInfoObject movie) {
        return moviesMarkerManager.getMovieMark(movie.getId() + "");
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


    public void showCategorySearchView(String query) {
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


    public void seeAll() {
        reportActionToAnalytics("seeAll", eventsContext.getLabel());
        EventsContext param = new EventsContext(eventsContext.location, eventsContext.query);
        Intent intent = new Intent(this, this.getClass())
                .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, param);
        startActivity(intent);
    }

    public void showCustomUrlActivity(String contestUrl, String title) {
        Intent intent = new Intent(this,
                contestUrl.contains(CustomUrlActivity.BLOG_HOST) ? BlogEntryActivity.class : CustomUrlActivity.class);
        intent.setData(Uri.parse(contestUrl));
        intent.putExtra(CustomUrlActivity.EXTRA_TITLE_KEY, title);
        startActivity(intent);
    }
}
