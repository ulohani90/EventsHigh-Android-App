package com.eventshigh.nearme.app.activity;

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.appindexing.AppIndex;
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
    // View for this activity.
    private ListView citySelector;

    // Client to Google api so that we can report the deep links
    // and fetch the user location if its not passed in intent.
    private GoogleApiClient client;

    // GCM registration helper.
    private GcmRegistration gcmRegistration;

    // Intent to launch the target
    private EventFetcherParam param = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set View.
        setContentView(R.layout.activity_launch);
        citySelector = ((ListView) findViewById(R.id.city_selector));
        citySelector.setAdapter(new CityListAdapter());

        // Set defaults for preferences.
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);

        // Process the incoming intent.
        param = IntentUtils.processIntent(this, getIntent());
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
        if (param.location == null && param.query.isEmpty() && pref.shouldShowOnBoarding()) {
            startActivity(new Intent(this, OnBoardingActivity.class));
            return;
        }

        // Register with GCM if needed. GCM is used for notifications messages.
        gcmRegistration = GcmRegistration.getInstance(getApplicationContext());
        gcmRegistration.updateGcmRegistrationIdIfNeeded();

        // Setup GoogleApiClient
        GoogleApiClient.Builder clientBuilder =
                new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API)
                        .addConnectionCallbacks(mConnectionCallbacks)
                        .addOnConnectionFailedListener(mOnConnectionFailedListener);
        if (param == null || param.city == null) {
            clientBuilder.addApi(LocationServices.API);
        }

        client = clientBuilder.build();
        client.connect();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (client != null && client.isConnected()) {
            Uri webUri = EventsHighEndpoints.getWebUri(param);
            AppIndex.AppIndexApi.viewEnd(client, this, Utils.getAppUri(webUri));
            client.disconnect();
        }
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
            // Report the start of deep index view.
            Uri webUri = EventsHighEndpoints.getWebUri(param);
            String title = param.toString();
            AppIndex.AppIndexApi.view(client, LaunchActivity.this, Utils.getAppUri(webUri),
                    title, webUri, null);

            // Set the location from FusedLocation if needed.
            if (param.city == null) {
                Location location = LocationServices.FusedLocationApi.getLastLocation(client);
                if (location != null) {
                    param.changeLocation(LocationUtils.locationToLatLng(location));
                    if (param.city != null) {
                        gcmRegistration.setLastCity(param.city);
                    } else {
                        reportActionToAnalytics("unsupportedCity");
                    }
                }
            }

            // Start the next activity if possible or ask user for city.
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
            if (param.city == null) {
                City lastCity = gcmRegistration.getLastCity();
                if (lastCity != null) {
                    reportActionToAnalytics("usedLastCity");
                    param.changeLocation(lastCity.cityBounds.getCenter());
                }
            }

            // If we have user location, start next activity.
            if (param.city != null) {
                startNextActivity();
                return;
            }

            // We do not have user location. Lets populate the City chooser and let user
            // select the city.
            reportActionToAnalytics("locationFailed");
            citySelector.setVisibility(View.VISIBLE);
            if (connectionResult != null) {
                Toast.makeText(LaunchActivity.this, R.string.failed_location, Toast.LENGTH_SHORT).show();
            }
        }
    };


    // ***********************
    // Helper methods
    // ***********************

    private void startNextActivity() {
        // Set defaults when there is no incoming intent.
        Class target = pref.isMapsViewDefault() ? EventsMapsActivity.class : EventsGridActivity.class;
        Intent outIntent = new Intent(this, target);
        outIntent.putExtra(IntentUtils.EXTRA_EVENT_FETCHER_PARAM, param);
        startActivity(outIntent);
    }

    public class CityListAdapter extends ArrayAdapter<City> {

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
                    param.changeLocation(getItem(position).cityBounds.getCenter());
                    gcmRegistration.setLastCity(city);
                    startNextActivity();
                }
            });

            return view;
        }
    }
}
