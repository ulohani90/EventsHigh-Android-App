package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.utils.EventFetcherParam;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

/**
 * A placeholder {@link android.app.Activity} which is responsible for launching
 * either {@link com.eventshigh.nearme.app.activity.MapsActivity} or
 * {@link com.eventshigh.nearme.app.activity.EventGridActivity} based on user preference.
 *
 * For now, this activity sets the preference 50%-50% for first time and then use this
 * preference in future.
 */
public class LaunchActivity extends Activity {
    private static final String WEB_URI_BASE = "http://www.eventshigh.com/";

    // Web URI associated with this activity, it is used to
    // report the URI in as deep link to Google App Indexing.
    private Uri webUri;
    private String title;

    // Client to Google api so that we can reprot the deep links.
    private GoogleApiClient client;

    // Intent to launch the target
    private Intent outIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Set defaults when there is no incoming intent.
        Class target = new Preferences(this).isMapsViewDefault() ?
                MapsActivity.class : EventGridActivity.class;
        outIntent = new Intent(this, target);
        webUri = Uri.parse(WEB_URI_BASE);
        title = "amazing events near you";

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

        // Setup GoogleApiClient
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API).build();
        client.registerConnectionFailedListener(new OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult connectionResult) {
                startActivity(outIntent);
            }
        });
        client.registerConnectionCallbacks(new ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                AppIndex.AppIndexApi.view(client, LaunchActivity.this, Utils.getAppUri(webUri),
                        title, webUri, null);
                startActivity(outIntent);
            }

            @Override
            public void onConnectionSuspended(int i) {
                // do nothing.
            }
        });

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

    private void processIntent(Intent inIntent) {
        EventFetcherParam param = null;

        if (Intent.ACTION_SEARCH.equals(inIntent.getAction())) {
            param = processSearchIntent(inIntent);
        } else if (Intent.ACTION_VIEW.equals(inIntent.getAction())) {
            param = processViewIntent(inIntent);
        }

        if (param != null) {
            outIntent.putExtra(LocationAwareEventActivity.EXTRA_EVENT_FETCHER_PARAM, param);
            webUri = getWebUri(param);
            title = getTitle(param);
        }
    }

    private static EventFetcherParam processSearchIntent(Intent inIntent) {
        String query = inIntent.getStringExtra(SearchManager.QUERY);

        EventFetcherParam param = null;
        Bundle appData = inIntent.getBundleExtra(SearchManager.APP_DATA);
        if (appData != null) {
            param = appData.getParcelable(LocationAwareEventActivity.EXTRA_EVENT_FETCHER_PARAM);
        }

        if (param == null) {
            param = new EventFetcherParam(null, 0, query);
        } else {
            param.query = query;
        }

        return param;
    }

    private static EventFetcherParam processViewIntent(Intent inIntent) {
        Uri inUri = inIntent.getData();

        if (inUri.getPath().startsWith("/city")) {
            return processCityViewIntent(inUri);
        } else if (inUri.getPath().startsWith("/search")) {
            return processSearchViewIntent(inUri);
        }

        return  null;
    }

    private static @Nullable EventFetcherParam processCityViewIntent(Uri webUri) {
        try {
            City city = City.valueOf(webUri.getLastPathSegment().toUpperCase());
            return getEventFetcherParamForCity(city);
        } catch (IllegalArgumentException | NullPointerException e) {
            // Invalid city in URI. Ignore.
        }

        return null;
    }

    private static @Nullable EventFetcherParam processSearchViewIntent(Uri webUri) {
        EventFetcherParam param = null;
        try {
            City city = City.valueOf(webUri.getQueryParameter("city").toUpperCase());
            param = getEventFetcherParamForCity(city);
        } catch (IllegalArgumentException | NullPointerException e) {
            // Invalid city in URI. Ignore.
        }

        String query = webUri.getQueryParameter("interest");
        if (query != null) {
            if (param == null) {
                param = new EventFetcherParam(null, 0, query);
            } else {
                param.query = query;
            }
        }

        return param;
    }

    private static EventFetcherParam getEventFetcherParamForCity(City city) {
        return new EventFetcherParam(city.cityBounds.getCenter(), 0, "");
    }

    private static String getTitle(EventFetcherParam param) {
        return (param.query.isEmpty() ? "amazing" : param.query) + " events " +
            (param.city == null ? "near you" : "in " + param.city.toString().toLowerCase());
    }

    private static Uri getWebUri(EventFetcherParam param) {
        StringBuilder sb = new StringBuilder(WEB_URI_BASE);
        if (param.query.isEmpty()) {
            if (param.city != null) {
                sb.append("city/").append(param.city.toString().toLowerCase());
            }
            return Uri.parse(sb.toString());
        }

        Uri.Builder builder = Uri.parse(sb.append("search").toString()).buildUpon();
        if (param.city != null) {
            builder.appendQueryParameter("city", param.city.toString().toLowerCase());
        }
        return builder.appendQueryParameter("interest", param.query).build();
    }
}
