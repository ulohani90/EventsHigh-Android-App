package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.EventFetcherParam;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.GAHelper;
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
 * either {@link com.eventshigh.nearme.app.activity.MapsActivity} or
 * {@link com.eventshigh.nearme.app.activity.EventGridActivity} based on user preference.
 *
 * For now, this activity sets the preference 50%-50% for first time and then use this
 * preference in future.
 */
public class LaunchActivity extends Activity {

    // Web URI associated with this activity, it is used to
    // report the URI in as deep link to Google App Indexing.
    private Uri webUri;
    private String title;

    // Client to Google api so that we can reprot the deep links.
    private GoogleApiClient client;

    // GCM registration helper.
    private GcmRegistration gcmRegistration;
    // Analytics helper.
    private GAHelper gaHelper;

    // Intent to launch the target
    EventFetcherParam param = null;
    private Intent outIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Set defaults when there is no incoming intent.
        Class target = new Preferences(this).isMapsViewDefault() ?
                MapsActivity.class : EventGridActivity.class;
        outIntent = new Intent(this, target);
        webUri = Uri.parse(EventsHighEndpoints.WEB_URI_BASE);
        title = "amazing events near you";

        // Setup the Analytic helper so that we can report events.
        gaHelper = GAHelper.getInstance(getApplicationContext());

        // Process the incoming intent.
        processIntent(getIntent());
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

        if (client.isConnected()) {
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
                        reportToAnalytics("unsupportedCity");
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
                    reportToAnalytics("usedLastCity");
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
            reportToAnalytics("locationFailed");
            if (connectionResult != null) {
                Toast.makeText(LaunchActivity.this, R.string.failed_location, Toast.LENGTH_SHORT).show();
            }

            ViewGroup container = (ViewGroup) findViewById(R.id.container);
            container.removeAllViews();

            ListView cityChooser = new ListView(LaunchActivity.this);
            cityChooser.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            cityChooser.setAdapter(new CityListAdapter());
            container.addView(cityChooser);
        }
    };


    // ***********************
    // Helper methods
    // ***********************

    private void startNextActivity() {
        outIntent.putExtra(LocationAwareEventActivity.EXTRA_EVENT_FETCHER_PARAM, param);
        startActivity(outIntent);
    }

    private void processIntent(Intent inIntent) {
        param = new EventFetcherParam(null, "");

        if (Intent.ACTION_SEARCH.equals(inIntent.getAction())) {
            processSearchIntent(inIntent);
        } else if (Intent.ACTION_VIEW.equals(inIntent.getAction())) {
            processViewIntent(inIntent);
        }

        webUri = EventsHighEndpoints.getWebUri(param);
        title = getTitle(param);
    }

    private void processSearchIntent(Intent inIntent) {
        String query = inIntent.getStringExtra(SearchManager.QUERY);
        reportToAnalytics("search", query);

        Bundle appData = inIntent.getBundleExtra(SearchManager.APP_DATA);
        if (appData != null) {
            EventFetcherParam param1 =
                    appData.getParcelable(LocationAwareEventActivity.EXTRA_EVENT_FETCHER_PARAM);
            if (param1 != null) {
                param = param1;
            }
        }

        param.query = query;
    }

    private void processViewIntent(Intent inIntent) {
        Uri inUri = inIntent.getData();

        if (inUri.getPath().startsWith("/city")) {
            processCityViewIntent(inUri);
        } else if (inUri.getPath().startsWith("/search")) {
            processSearchViewIntent(inUri);
        }

        reportToAnalytics("deepLink", "homepage");
    }

    private void processCityViewIntent(Uri webUri) {
        reportToAnalytics("deepLink", "city");

        try {
            City city = City.valueOf(webUri.getLastPathSegment().toUpperCase());
            param.changeLocation(city.cityBounds.getCenter());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Invalid city in URI. Ignore.
        }
    }

    private void processSearchViewIntent(Uri webUri) {
        reportToAnalytics("deepLink", "search");

        try {
            City city = City.valueOf(webUri.getQueryParameter("city").toUpperCase());
            param.changeLocation(city.cityBounds.getCenter());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Invalid city in URI. Ignore.
        }

        String query = webUri.getQueryParameter("interest");
        if (query != null) {
            param.query = query;
        }
    }

    private static String getTitle(EventFetcherParam param) {
        return (param.query.isEmpty() ? "amazing" : param.query) + " events " +
                (param.city == null ? "near you" : "in " + param.city.toString().toLowerCase());
    }

    private void reportToAnalytics(String action) {
        reportToAnalytics(action, "");
    }

    private void reportToAnalytics(String action, String label) {
        gaHelper.reportActionToAnalytics(LaunchActivity.class.getSimpleName(), action, label, 1);
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
