package com.eventshigh.nearme.app.activity;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.app.Activity;
import android.os.PersistableBundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.ui.adapter.GooglePlacesAutocompleteAdapter;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import pl.snowdog.material.ui.ToolbarColorizeHelper;

public class PlacesAutocompleteBoundedActivity extends AppCompatActivity implements TextWatcher, GoogleApiClient.OnConnectionFailedListener {

    private GooglePlacesAutocompleteAdapter dataAdapter;
    EditText etSearchBar;
    ListView listView;

    GoogleApiClient mGoogleApiClient;
    Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places_autocomplete_bounded);
        toolbar = (Toolbar) findViewById(R.id.toolbar_et);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etSearchBar = (EditText) findViewById(R.id.et_search_place);
        Account account = new Account(this);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0 /* clientId */, this)
                .addApi(Places.GEO_DATA_API)
                .build();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
        dataAdapter = new GooglePlacesAutocompleteAdapter(PlacesAutocompleteBoundedActivity.this, mGoogleApiClient, account.getLastCity().cityBounds, null, account.getLastCity().name());
        listView = (ListView) findViewById(R.id.lv_search_place_list);
        listView.setAdapter(dataAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ;
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, ((AutocompletePrediction) dataAdapter.getItem(position)).getPlaceId());
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            }
        });
        listView.setTextFilterEnabled(true);
        etSearchBar.addTextChangedListener(this);
        (findViewById(R.id.select_current_location)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onSelectLocationClick();

                /*if (ActivityCompat.checkSelfPermission(PlacesAutocompleteBoundedActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(PlacesAutocompleteBoundedActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    if (location != null) {

                        LatLng latLng = LocationUtils.locationToLatLng(location);
                        Geocoder gcd = new Geocoder(PlacesAutocompleteBoundedActivity.this, Locale.getDefault());
                        List<Address> addresses = null;
                        try {
                            addresses = gcd.getFromLocation(latLng.latitude, latLng.longitude, 1);
                            if (addresses.size() > 0) {
                                System.out.println(addresses.get(0).getLocality());
                                Intent intent = new Intent();
                                intent.putExtra("place_lat_lng", latLng);
                                intent.putExtra("place_name", addresses.get(0).getLocality());
                                setResult(Activity.RESULT_OK, intent);
                                finish();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }*/

                //noinspection ConstantConditions
            }
        });
    }

    GoogleApiClient googleApiClient;

    public void onSelectLocationClick() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {

                        }

                        @Override
                        public void onConnectionSuspended(int i) {

                        }
                    })
                    .addOnConnectionFailedListener(this).build();
            googleApiClient.connect();
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        //**************************
        builder.setAlwaysShow(true); //this is the key ingredient
        //**************************

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:

                        if (ActivityCompat.checkSelfPermission(PlacesAutocompleteBoundedActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(PlacesAutocompleteBoundedActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                            Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                            if (location != null) {

                                LatLng latLng = LocationUtils.locationToLatLng(location);
                                Geocoder gcd = new Geocoder(PlacesAutocompleteBoundedActivity.this, Locale.getDefault());
                                List<Address> addresses = null;
                                try {
                                    addresses = gcd.getFromLocation(latLng.latitude, latLng.longitude, 1);
                                    if (addresses.size() > 0) {
                                        System.out.println(addresses.get(0).getLocality());
                                        Intent intent = new Intent();
                                        intent.putExtra("place_lat_lng", latLng);
                                        intent.putExtra("place_name", addresses.get(0).getSubLocality());
                                        setResult(Activity.RESULT_OK, intent);
                                        finish();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    PlacesAutocompleteBoundedActivity.this, 1000);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }
    }

    private GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            if (ActivityCompat.checkSelfPermission(PlacesAutocompleteBoundedActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(PlacesAutocompleteBoundedActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (location != null) {
                    LatLng latLng = LocationUtils.locationToLatLng(location);
                    Geocoder gcd = new Geocoder(PlacesAutocompleteBoundedActivity.this, Locale.getDefault());
                    List<Address> addresses = null;
                    try {
                        addresses = gcd.getFromLocation(latLng.latitude, latLng.longitude, 1);
                        if (addresses.size() > 0) {
                            System.out.println(addresses.get(0).getLocality());
                            Intent intent = new Intent();
                            intent.putExtra("place_lat_lng", latLng);
                            intent.putExtra("place_name", addresses.get(0).getLocality());
                            setResult(Activity.RESULT_OK, intent);
                            finish();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            //noinspection ConstantConditions
        }
        // Start the next activity if possible or ask user for city.


        @Override
        public void onConnectionSuspended(int i) {
            // do nothing.
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (toolbar != null) {
            setLightToolbarIcons();
        }
    }

    private void setLightToolbarIcons() {
        toolbar.post(new Runnable() {
            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                ToolbarColorizeHelper.colorizeToolbar(toolbar,
                        getResources().getColor(android.R.color.white), PlacesAutocompleteBoundedActivity.this);
            }
        });
    }

    /**
     * Callback for results from a Places Geo Data API query that shows the first place result in
     * the details view on screen.
     */
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                Log.e("TAG", "Place query did not complete. Error: " + places.getStatus().toString());
                places.release();
                return;
            }
            // Get the Place object from the buffer.
            final Place place = places.get(0);

            Intent intent = new Intent();
            intent.putExtra("place_lat_lng", place.getLatLng());
            intent.putExtra("place_name", place.getName());
            places.release();
            setResult(Activity.RESULT_OK, intent);
            finish();
            // Format details of the place for display and show it in a TextView.


        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void afterTextChanged(Editable s) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        dataAdapter.getFilter().filter(s.toString());
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.e("TAG", "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

        // TODO(Developer): Check error code and notify the user of error state and resolution.
        Toast.makeText(this,
                "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();
    }
}
