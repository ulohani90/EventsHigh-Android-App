package com.eventshigh.nearme.app.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.widget.EditText;
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

    public static void show(final Context context, final OnLocationSelection onLocationSelection) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.ask_locality);

        // Set up the input
        final EditText input = new EditText(context);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new LatLngFetcher(context, onLocationSelection).execute(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private static class LatLngFetcher extends AsyncTask<String, Void, Pair<String, LatLng>> {

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
                Toast.makeText(context, R.string.failed_locality, Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        @Override
        protected void onPostExecute(@Nullable Pair<String, LatLng> locality) {
            if (locality != null) {
                onLocationSelection.onLocationSelection(locality.first, locality.second);
            }
        }
    }
}
