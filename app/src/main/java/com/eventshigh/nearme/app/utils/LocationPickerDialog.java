package com.eventshigh.nearme.app.utils;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

/**
 * Shows a dialog box with text box to let user enter location.
 */
public class LocationPickerDialog {
    private static final String LOG_TAG = LocationPickerDialog.class.getSimpleName();

    public static interface OnLocationSelection {
        public void onLocationSelection(String locationString, LatLng locationPoint);
    }

    private AlertDialog dialog = null;
    private OnLocationSelection onLocationSelection;
    private Context context;

    public  void show(final Context context, String countryCode,
                      final OnLocationSelection onLocationSelection) {
        this.context = context;
        this.onLocationSelection = onLocationSelection;

        // Set up the input
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(context).inflate(R.layout.location_picker, null);
        final AutoCompleteTextView input = (AutoCompleteTextView) view.findViewById(R.id.localityName);

        // Set up the AutoCompleteTextView with adapter and callbacks.
        input.setAdapter(new PlacesAutoCompleteAdapter(context,
                android.R.layout.simple_dropdown_item_1line, countryCode));
        input.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    close(input.getText().toString());
                    return true;
                }
                return false;
            }
        });
        input.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                close(parent.getAdapter().getItem(position).toString());
            }
        });

        // Setup the dialog box.
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);
        builder.setTitle(R.string.ask_locality);
        builder.setIcon(R.drawable.ic_action_place_dark);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                close(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });


        dialog = builder.create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
        input.requestFocus();
    }

    private void close(String selectedPlace) {
        new LatLngFetcher(context, onLocationSelection).execute(selectedPlace);
        if(dialog != null && dialog.isShowing()) {
            dialog.cancel();
        }
    }

    public static class LatLngFetcher extends AsyncTask<String, Void, Pair<String, LatLng>> {

        private final Context context;
        private final OnLocationSelection onLocationSelection;

        private LatLngFetcher(Context context, OnLocationSelection onLocationSelection) {
            this.context = context;
            this.onLocationSelection = onLocationSelection;
        }

        @Override
        protected Pair<String, LatLng> doInBackground(String... params) {
            try {
                Geocoder geocoder = new Geocoder(context);
                List<Address> addresses = geocoder.getFromLocationName(params[0], 1);
                if (addresses.isEmpty() ||
                        !addresses.get(0).hasLatitude() ||
                        !addresses.get(0).hasLatitude()) {
                    throw new IOException("Geocoding failed, no address returned.");
                }

                return Pair.create(params[0],
                        new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude()));
            } catch (IOException e) {
                Log.w(LOG_TAG, "failed to fetch the address", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(@Nullable Pair<String, LatLng> locality) {
            if (locality != null) {
                onLocationSelection.onLocationSelection(locality.first, locality.second);
            } else {
                Toast.makeText(context, R.string.failed_locality, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
