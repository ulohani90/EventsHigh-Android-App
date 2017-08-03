package com.eventshigh.nearme.app.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.ui.CitySelectDialog;
import com.eventshigh.nearme.app.ui.adapter.GooglePlacesAutocompleteAdapter;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
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

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import pl.snowdog.material.ui.ToolbarColorizeHelper;

public class PlacesAutocompleteBoundedActivity extends BaseActivity implements TextWatcher, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GooglePlacesAutocompleteAdapter dataAdapter;
    EditText etSearchBar;
    ListView listView;

    GoogleApiClient mGoogleApiClient;
    LinearLayout llChangeCity;
    TextView currentCity, changeCityText;
    Toolbar toolbar;

    boolean isShowSpecialText;

    public static final String TAG = PlacesAutocompleteBoundedActivity.class.getSimpleName();

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private static final int PERMISSION_ACCESS_FINE_LOCATION = 0x01;

    private static final int PERMISSION_ACCESS_COARSE_LOCATION = 0x02;

    private static final int SET_LOCATION_SETTINGS_ON = 0x01001;
    private LocationRequest mLocationRequest;

    boolean isSelectLocationClicked;

    boolean isLocationPermissionGranted = true;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places_autocomplete_bounded);
        toolbar = (Toolbar) findViewById(R.id.toolbar_et);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etSearchBar = (EditText) findViewById(R.id.et_search_place);
        final Account account = new Account(this);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0 /* clientId */, this)
                .addConnectionCallbacks(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API)
                .build();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();

        if (account.getLastCity() == null) {
            Toast.makeText(this, "No city selected", Toast.LENGTH_SHORT).show();
            this.finish();
        }
        dataAdapter = new GooglePlacesAutocompleteAdapter(PlacesAutocompleteBoundedActivity.this, mGoogleApiClient, (account.getLastCity() != null) ? account.getLastCity().cityBounds : null, null, account.getLastCity().name());
        listView = (ListView) findViewById(R.id.lv_search_place_list);
        listView.setAdapter(dataAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, ((AutocompletePrediction) dataAdapter.getItem(position)).getPlaceId());
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            }
        });
        listView.setTextFilterEnabled(true);
        etSearchBar.addTextChangedListener(this);

        View locView = findViewById(R.id.select_current_location);
        if (locView != null) {
            locView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isSelectLocationClicked = true;
                    onSelectLocationClick();
                }
            });
        }

        llChangeCity = (LinearLayout) findViewById(R.id.btn_change_city);
        currentCity = (TextView) findViewById(R.id.tv_current_city);
        currentCity.setText(account.getLastCity().name());
        changeCityText = (TextView) findViewById(R.id.tv_change_city_text);
        SpannableString content = new SpannableString("Change City");
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        changeCityText.setText(content);

        llChangeCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CitySelectDialog.show(PlacesAutocompleteBoundedActivity.this, account, new CitySelectDialog.CitySelectionCallback() {
                    @Override
                    public void onCityChanged(City city) {
                        currentCity.setText(city.name());
                        etSearchBar.setHint("Search Locality in " + Utils.capitalize(city.name()));
                        dataAdapter.changeCity(city.name(), city.cityBounds);
                    }
                });
            }
        });

        etSearchBar.setHint("Search Locality in " + Utils.capitalize(account.getLastCity().name()));
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000);
    }

    // GoogleApiClient googleApiClient;

    public void onSelectLocationClick() {

       /* if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, 0 *//* clientId *//*, this)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {

                        }

                        @Override
                        public void onConnectionSuspended(int i) {

                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                            if (connectionResult.hasResolution()) {
                                try {
                                    // Start an Activity that tries to resolve the error
                                    connectionResult.startResolutionForResult(PlacesAutocompleteBoundedActivity.this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                                } catch (IntentSender.SendIntentException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
                            }
                        }
                    }).build();
            mGoogleApiClient.connect();
        }*/


        if (mGoogleApiClient.isConnected()) {
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
                    LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    if (mGoogleApiClient.isConnected()) {
                        final Status status = result.getStatus();

                        final LocationSettingsStates state = result.getLocationSettingsStates();
                        switch (status.getStatusCode()) {
                            case LocationSettingsStatusCodes.SUCCESS:

                                if (ActivityCompat.checkSelfPermission(PlacesAutocompleteBoundedActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                                        ActivityCompat.checkSelfPermission(PlacesAutocompleteBoundedActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                                    Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                                    if (location != null) {

                                        handleNewLocation(location);
                                    } else {
                                        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, PlacesAutocompleteBoundedActivity.this);
                                    }
                                } else {
                                    if (ActivityCompat.checkSelfPermission(PlacesAutocompleteBoundedActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        ActivityCompat.requestPermissions(PlacesAutocompleteBoundedActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ACCESS_FINE_LOCATION);
                                    } else {
                                        ActivityCompat.requestPermissions(PlacesAutocompleteBoundedActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_ACCESS_COARSE_LOCATION);
                                    }
                                    // Toast.makeText(PlacesAutocompleteBoundedActivity.this, "Location Permission not allowed.", Toast.LENGTH_SHORT).show();
                                }

                                break;
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                // Location settings are not satisfied. But could be fixed by showing the user
                                // a dialog.
                                try {
                                    // Show the dialog by calling startResolutionForResult(),
                                    // and check the result in onActivityResult().
                                    status.startResolutionForResult(
                                            PlacesAutocompleteBoundedActivity.this, SET_LOCATION_SETTINGS_ON);
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
                }
            });
        }
    }

    private void handleNewLocation(Location location) {
        LatLng latLng = LocationUtils.locationToLatLng(location);
        Geocoder gcd = new Geocoder(PlacesAutocompleteBoundedActivity.this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses.size() > 0 && addresses.get(0).getLocality() != null) {
                // System.out.println(addresses.get(0).getLocality());
                Intent intent = new Intent();
                intent.putExtra("place_lat_lng", latLng);
                intent.putExtra("place_name", addresses.get(0).getSubLocality());
                if (addresses.get(0).getLocality().equalsIgnoreCase("Bangalore") ||
                        addresses.get(0).getLocality().equalsIgnoreCase("Bengaluru")) {
                    new Account(PlacesAutocompleteBoundedActivity.this).setLastCity(City.BANGALORE);
                } else if (addresses.get(0).getLocality().equalsIgnoreCase("Chennai")) {
                    new Account(PlacesAutocompleteBoundedActivity.this).setLastCity(City.CHENNAI);
                } else if (addresses.get(0).getLocality().equalsIgnoreCase("Delhi")) {
                    new Account(PlacesAutocompleteBoundedActivity.this).setLastCity(City.DELHI);
                } else if (addresses.get(0).getLocality().equalsIgnoreCase("Mumbai")) {
                    new Account(PlacesAutocompleteBoundedActivity.this).setLastCity(City.MUMBAI);
                } else if (addresses.get(0).getLocality().equalsIgnoreCase("Kolkata")) {
                    new Account(PlacesAutocompleteBoundedActivity.this).setLastCity(City.KOLKATA);
                } else if (addresses.get(0).getLocality().equalsIgnoreCase("Hyderabad")) {
                    new Account(PlacesAutocompleteBoundedActivity.this).setLastCity(City.HYDERABAD);
                } else if (addresses.get(0).getLocality().equalsIgnoreCase("Pune")) {
                    new Account(PlacesAutocompleteBoundedActivity.this).setLastCity(City.PUNE);
                } else if (addresses.get(0).getLocality().equalsIgnoreCase("Goa")) {
                    new Account(PlacesAutocompleteBoundedActivity.this).setLastCity(City.GOA);
                }

                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

    }


    @Override
    protected void onStop() {
        super.onStop();

     /*   if (googleApiClient != null) {
            googleApiClient.disconnect();
        }*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected() && isLocationPermissionGranted) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
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

        mGoogleApiClient.connect();
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
        public void onResult(@NonNull PlaceBuffer places) {
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

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (isSelectLocationClicked) {
            onSelectLocationClick();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_ACCESS_FINE_LOCATION || requestCode == PERMISSION_ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isLocationPermissionGranted = true;
                onSelectLocationClick();
            } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                isLocationPermissionGranted = false;
                Toast.makeText(PlacesAutocompleteBoundedActivity.this, "Location permission is off. Please grant location permission from settings.", Toast.LENGTH_SHORT).show();

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PERMISSION_ACCESS_COARSE_LOCATION || requestCode == PERMISSION_ACCESS_FINE_LOCATION) {

            onSelectLocationClick();
        } else if (requestCode == SET_LOCATION_SETTINGS_ON && resultCode != Activity.RESULT_OK) {
            isSelectLocationClicked = false;
        }
    }
}
