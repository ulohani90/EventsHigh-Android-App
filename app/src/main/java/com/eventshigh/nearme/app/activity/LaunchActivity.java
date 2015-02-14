package com.eventshigh.nearme.app.activity;

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

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
    private static String[] EXPLORE_TAGS = { "Parties", "Health & Wellness", "Tech",
            "Education", "Theatre", "Outdoors", "Kids", "Dance", "Shopping", "Food", "Literature",
            "Film", "Social Causes", "Environment", "Sports", "Spiritual", "Comedy", "Fashion"};

    // UI Elements for this activity.
    private ViewSwitcher viewSwitcher;

    // Client to Google api so that we can fetch the user location if
    // its not passed in intent.
    private GoogleApiClient client;

    // GCM registration helper.
    private GcmRegistration gcmRegistration;

    // Context for next activity.
    private EventsContext eventsContext = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set View.
        setContentView(R.layout.activity_launch);
        viewSwitcher = (ViewSwitcher) findViewById(R.id.view_switcher);

        ListView citySelector = (ListView) findViewById(R.id.city_selector);
        citySelector.setAdapter(new CityListAdapter());

        LinearLayout exploreLayout = (LinearLayout) findViewById(R.id.explore_layout);
        LinearLayout last = null;
        for (int i = 0; i < EXPLORE_TAGS.length; i++) {
            if (i % 3 == 0) {
                last = new LinearLayout(this);
                exploreLayout.addView(last);
            }

            last.addView(getExploreCard(EXPLORE_TAGS[i], last));
        }

        // Set defaults for preferences.
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);
    }

    @Override
    public void onNewIntent(Intent intent) {
        eventsContext = IntentUtils.processIntent(this, intent);
        showNextScreen(false);
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
        if (eventsContext.location == null && eventsContext.query.isEmpty() && pref.shouldShowOnBoarding()) {
            startActivity(new Intent(this, OnBoardingActivity.class));
            finish();
            return;
        }

        // Register with GCM if needed. GCM is used for notifications messages.
        gcmRegistration = GcmRegistration.getInstance(getApplicationContext());
        gcmRegistration.updateGcmRegistrationIdIfNeeded();

        // Show next screen.
        showNextScreen(false);
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
            if (connectionResult != null) {
                Toast.makeText(LaunchActivity.this, R.string.failed_location, Toast.LENGTH_SHORT).show();
            }
        }
    };


    // ***********************
    // Helper methods
    // ***********************

    public void showToday(View view) {
        eventsContext.tabName = "Today";
        showNextScreen(true);
    }

    public void showTomorrow(View view) {
        eventsContext.tabName = "Tomorrow";
        showNextScreen(true);
    }

    public void showThisWeekend(View view) {
        eventsContext.tabName = "This Weekend";
        showNextScreen(true);
    }

    private void showNextScreen(boolean isUserAction) {
        // If we do not have user city, use GoogleLocation api to get user location.
        if (eventsContext.city == null) {
            disconnectClient();
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
            viewSwitcher.setDisplayedChild(0);
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

    private static int getInfoGraphId(String tag) {
        try {
            return R.drawable.class.getField("infograph_" +
                    EventCategory.toCategoryParsableString(tag).toLowerCase()).getInt(null);
        } catch (IllegalAccessException| NoSuchFieldException e) {
            // Ignore
        }

        return R.drawable.eh_default_event_list;
    }

    private void disconnectClient() {
        if (isClientConnecting()) {
            client.disconnect();
        }
    }

    private boolean isClientConnecting() {
        return client != null && (client.isConnected() || client.isConnecting());
    }

    private View getExploreCard(final String tagName, ViewGroup parent) {
        final View view = getLayoutInflater().inflate(R.layout.explore_card, parent, false);
        view.setLayoutParams(new LayoutParams(Utils.dpToPx(this, 100), LayoutParams.MATCH_PARENT, 1));
        ((TextView) view.findViewById(R.id.explore_name)).setText(tagName);
        ((ImageView) view.findViewById(R.id.explore_image)).setImageResource(
                getInfoGraphId(tagName));

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                eventsContext.query = tagName;
                showNextScreen(true);
            }
        });

        Utils.waitForViewVisible(view, new Runnable() {
            @Override
            public void run() {
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                lp.height = lp.width;
                view.setLayoutParams(lp);
            }
        });
        return view;
    }
}
