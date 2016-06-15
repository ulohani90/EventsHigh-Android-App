package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.os.PersistableBundle;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

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
    }

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
