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
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
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

    AlertDialog dialog = null;

    public  void show(final Context context, final OnLocationSelection onLocationSelection) {
        // Set up the input
        final AutoCompleteTextView input = new AutoCompleteTextView(context);
        input.setSingleLine();
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setAdapter(new PlacesAutoCompleteAdapter(context, android.R.layout.simple_dropdown_item_1line));
        input.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    new LatLngFetcher(context, onLocationSelection).execute(input.getText().toString());
                    close();
                    return true;
                }
                return false;
            }
        });

        // Setup the dialog box.
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.ask_locality);
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

        dialog = builder.show();
        input.requestFocus();
    }

    public void close() {
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
