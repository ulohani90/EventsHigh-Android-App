package com.eventshigh.nearme.app.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.widget.SearchView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.FeaturedEventsRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.settings.Preferences;
import com.eventshigh.nearme.app.settings.SettingsActivity;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import java.util.Calendar;
import java.util.List;

/**
 * A placeholder {@link android.app.Activity} which is responsible for launching
 * either {@link EventsMapsActivity} or
 * {@link EventsGridActivity} based on user preference.
 *
 * For now, this activity sets the preference 50%-50% for first time and then use this
 * preference in future.
 */
public class LaunchActivity extends BaseActivity {

    // Constants
    private static final int MAX_FEATURED_EVENTS = 5;
    private static final int EXPLORE_CARD_WIDTH_DP = 160;
    private static final int MIN_EXPLORE_CARD_IN_ROW = 2;
    private static final String[] EXPLORE_TAGS = { "Parties", "Health & Wellness", "Tech",
            "Education", "Theatre", "Outdoors", "Kids", "Dance", "Shopping", "Food", "Literature",
            "Film", "Social Causes", "Environment", "Sports", "Spiritual", "Comedy", "Fashion"};

    // UI Elements for this activity.
    private ViewSwitcher viewSwitcher;
    private LinearLayout dotsView;
    private ViewPager featuredEventsPager;

    // Client to Google api so that we can fetch the user location if
    // its not passed in intent.
    private GoogleApiClient client;

    // GCM registration helper.
    private GcmRegistration gcmRegistration;

    // Context for next activity.
    private EventsContext eventsContext;

    // User preferences.
    protected Preferences pref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set View.
        setContentView(R.layout.activity_launch);
        getSupportActionBar().setTitle(R.string.title_explore);
        viewSwitcher = (ViewSwitcher) findViewById(R.id.view_switcher);
        dotsView = (LinearLayout) findViewById(R.id.dots_parent);
        featuredEventsPager = (ViewPager) findViewById(R.id.featured_events_pager);

        // Set defaults for preferences.
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);

        // Read Preferences
        pref = Preferences.getInstance(getApplicationContext());
    }

    public void onStart() {
        super.onStart();

        // Check for Google Play Services.
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            Toast.makeText(this, GooglePlayServicesUtil.getErrorString(status), Toast.LENGTH_SHORT).show();
            GooglePlayServicesUtil.getErrorDialog(status, this, 0, new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            }).show();
            return;
        }

        // We show the onboarding If this is first activity and there was no
        // location/query passed through intent.
        eventsContext = IntentUtils.processIntent(this, getIntent());
        if (eventsContext.location == null && eventsContext.query.isEmpty()) {
            if (pref.shouldShowOnBoarding()) {
                startActivity(new Intent(this, OnBoardingActivity.class));
                return;
            }

            if (!isTaskRoot()) {
                finish();
                return;
            }
        }

        // Register with GCM if needed. GCM is used for notifications messages.
        gcmRegistration = GcmRegistration.getInstance(getApplicationContext());
        gcmRegistration.updateGcmRegistrationIdIfNeeded();

        // Show next screen.
        showNextScreen(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_launch, menu);

        // Search View.
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // ***********************
    // Callbacks
    // ***********************

    // Callback for GoogleClientApi. This is called when googleClientApi is
    // ready to accept requests. We set the user location if needed and start
    // next activity.
    private ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(client);
            if (location != null) {
                eventsContext.changeLocation(LocationUtils.locationToLatLng(location));
                if (eventsContext.city != null) {
                    gcmRegistration.setLastCity(eventsContext.city);
                } else {
                    reportActionToAnalytics("unsupportedCity");
                }
            }

            // Start the next activity if possible or ask user for city.
            client.disconnect();
            mOnConnectionFailedListener.onConnectionFailed(null);
        }

        @Override
        public void onConnectionSuspended(int i) {
            // do nothing.
        }
    };

    private OnConnectionFailedListener mOnConnectionFailedListener = new OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            // Set the location from lastCity if needed.
            if (eventsContext.city == null) {
                City lastCity = gcmRegistration.getLastCity();
                if (lastCity != null) {
                    reportActionToAnalytics("usedLastCity");
                    eventsContext.changeLocation(lastCity.cityBounds.getCenter());
                }
            }

            // If we have user location, start next activity.
            if (eventsContext.city != null) {
                showNextScreen(false);
                return;
            }

            // We do not have user location. Lets populate the City chooser and let user
            // select the city.
            reportActionToAnalytics("locationFailed");
            viewSwitcher.setDisplayedChild(1);
            ListView citySelector = (ListView) findViewById(R.id.city_selector);
            citySelector.setAdapter(new CityListAdapter());
            if (connectionResult != null) {
                Toast.makeText(LaunchActivity.this, R.string.failed_location, Toast.LENGTH_SHORT).show();
            }
        }
    };


    // ***********************
    // Helper methods
    // ***********************

    public void showToday(View view) {
        eventsContext.setDateFilter(Calendar.getInstance());
        showNextScreen(true);
    }

    public void showAll(View view) {
        showNextScreen(true);
    }

    public void showThisWeekend(View view) {
        eventsContext.query = "this weekend";
        showNextScreen(true);
    }

    private void showNextScreen(boolean isUserAction) {
        // If we do not have user city, use GoogleLocation api to get user location.
        if (eventsContext.city == null) {
            client = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mOnConnectionFailedListener)
                    .build();
            client.connect();
            return;
        }

        // If we do not have query, show explore screen.
        if (!isUserAction && eventsContext.query.isEmpty()) {
            showExploreScreen();
            return;
        }

        // Launch the target activity.
        Class target = pref.isMapsViewDefault() ? EventsMapsActivity.class : EventsGridActivity.class;
        Intent outIntent = new Intent(this, target);
        outIntent.putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, eventsContext);

        startActivity(outIntent);
        if (!isUserAction) {
            finish();
        }
    }

    private boolean exploreScreenPopulated = false;
    private void showExploreScreen() {
        viewSwitcher.setDisplayedChild(0);

        if (!exploreScreenPopulated) {
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

            FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) featuredEventsPager.getLayoutParams();
            param.height = Math.min(displayMetrics.heightPixels,  3 * displayMetrics.widthPixels / 4);
            featuredEventsPager.setLayoutParams(param);

            int spacing = Utils.dpToPx(this, 10);
            int widthPixels = displayMetrics.widthPixels;
            int numColumns = Math.max(MIN_EXPLORE_CARD_IN_ROW,
                    (widthPixels - spacing * 2) / Utils.dpToPx(this, EXPLORE_CARD_WIDTH_DP));

            int size = (widthPixels - spacing * (numColumns + 1)) / numColumns;
            LayoutParams exploreCardLP = new LayoutParams(size, size);
            exploreCardLP.setMargins(0, spacing, spacing, 0);

            LayoutParams rowLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            rowLP.setMargins(spacing, 0, 0, 0);

            LinearLayout exploreLayout = (LinearLayout) findViewById(R.id.explore_layout);
            LinearLayout last = new LinearLayout(this);
            for (int i = 0; i < EXPLORE_TAGS.length; i++) {
                if (i % numColumns == 0) {
                    last = new LinearLayout(this);
                    last.setLayoutParams(rowLP);
                    exploreLayout.addView(last);
                }

                last.addView(getExploreCard(EXPLORE_TAGS[i], exploreCardLP, last));
            }

            exploreScreenPopulated = true;
        }

        // Submit the request to populate Featured Events.
        dotsView.removeAllViews();
        featuredEventsPager.setAdapter(mLoadingAdapter);
        FeaturedEventsRequest.submit(this, eventsContext, Priority.IMMEDIATE, false, mEventsListener,
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Toast.makeText(LaunchActivity.this, R.string.failed_load, Toast.LENGTH_SHORT).show();
                        // featuredEventsPager.setAdapter(mFailedAdaper);
                    }
                });
    }

    private class CityListAdapter extends ArrayAdapter<City> {
        public CityListAdapter() {
            super(LaunchActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1);
            addAll(City.values());
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    City city = getItem(position);
                    eventsContext.changeLocation(city.cityBounds.getCenter());
                    gcmRegistration.setLastCity(city);
                    showNextScreen(false);
                }
            });

            return view;
        }
    }

    private View getExploreCard(final String tagName, LayoutParams lp, ViewGroup parent) {
        final View view = getLayoutInflater().inflate(R.layout.explore_card, parent, false);
        view.setLayoutParams(lp);
        ((TextView) view.findViewById(R.id.explore_name)).setText(tagName);
        ((ImageView) view.findViewById(R.id.explore_image)).setImageResource(
                getInfoGraphId(tagName));

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                reportActionToAnalytics("exploreCategory", tagName);
                eventsContext.query = tagName.toLowerCase();
                showNextScreen(true);
            }
        });
        return view;
    }

    private static int getInfoGraphId(String tag) {
        try {
            return R.drawable.class.getField("infograph_" +
                    EventCategory.toCategoryParsableString(tag).toLowerCase()).getInt(null);
        } catch (IllegalAccessException| NoSuchFieldException e) {
            // Ignore
        }

        return R.drawable.eh_default_event_list;
    }

    private Listener<List<Event>> mEventsListener = new Listener<List<Event>>() {
        @Override
        public void onResponse(final List<Event> events, boolean isIntermediate) {
            int dp4 = Utils.dpToPx(LaunchActivity.this, 4);
            int dp8 = Utils.dpToPx(LaunchActivity.this, 8);
            int dp12 = Utils.dpToPx(LaunchActivity.this, 12);
            final LayoutParams smallDotLayoutParams = new LayoutParams(dp8, dp8);
            smallDotLayoutParams.setMargins(dp4, dp4, dp4, dp4);
            final LayoutParams bigDotLayoutParams = new LayoutParams(dp12, dp12);
            bigDotLayoutParams.setMargins(dp4, dp4, dp4, dp4);


            LayoutInflater layoutInflater = getLayoutInflater();
            int numEventsToShow = Math.min(events.size(), MAX_FEATURED_EVENTS);
            dotsView.removeAllViews();
            for (int i = 0; i < numEventsToShow; i++) {
                View view = layoutInflater.inflate(R.layout.viewpager_dot, dotsView, false);
                dotsView.addView(view);
                view.setLayoutParams(i == 0 ? bigDotLayoutParams : smallDotLayoutParams);
            }

            featuredEventsPager.setAdapter(new FeaturedEventsAdapter(events.subList(0, numEventsToShow)));
            featuredEventsPager.setOnPageChangeListener(new OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    // do nothing.
                }

                @Override
                public void onPageSelected(int position) {
                    for (int i = 0; i < dotsView.getChildCount(); i++) {
                        dotsView.getChildAt(i).setLayoutParams( i == position ? bigDotLayoutParams : smallDotLayoutParams);
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    // do nothing.
                }
            });
        }
    };

    private class FeaturedEventsAdapter extends PagerAdapter {
        private final List<Event> events;

        private FeaturedEventsAdapter(List<Event> events) {
            this.events = events;
        }

        @Override
        public int getCount() {
            return events.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View eventCard = getLayoutInflater().inflate(
                    R.layout.explore_event_card, container, false);
            final Event event = events.get(position);

            NetworkImageView imageView = (NetworkImageView) eventCard.findViewById(R.id.event_bg);
            if (event.imgUrl != null) {
                imageView.setImageUrl(
                        event.imgUrl, VolleyHelper.getImageLoader(LaunchActivity.this));
            } else {
                imageView.setImageBitmap(null);
            }

            ((TextView)eventCard.findViewById(R.id.event_title)).setText(event.title);
            ((TextView)eventCard.findViewById(R.id.event_venue)).setText(Utils.capitalize(
                    event.venue == null ? event.city.toString() : event.venue));

            EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
            if (eventTime != null) {
                ((TextView)eventCard.findViewById(R.id.event_date)).setText(eventTime.day + ", " + eventTime.date);
                if (eventTime.time != null) {
                    ((TextView) eventCard.findViewById(R.id.event_time)).setText(eventTime.time);
                }
            }

            eventCard.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEventAction(event, "featuredClick");
                    showEventDetails(event);
                }
            });

            container.addView(eventCard);
            return eventCard;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }

    private PagerAdapter mLoadingAdapter = new PagerAdapter() {
        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = getLayoutInflater().inflate(R.layout.view_loading, container, false);
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    };

}
