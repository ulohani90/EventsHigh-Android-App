package com.eventshigh.nearme.app.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.task.LatLngFetcherTask;
import com.google.android.gms.maps.model.LatLng;

/**
 * Shows a dialog box with text box to let user enter location.
 */
public class LocationPickerDialog {

    public static interface OnLocationSelection {
        public void onLocationSelection(String locationString, LatLng locationPoint);
    }

    private AlertDialog dialog = null;
    private OnLocationSelection onLocationSelection;
    private Context context;

    public void show(final Context context, String countryCode,
                     final OnLocationSelection onLocationSelection) {
        this.context = context;
        this.onLocationSelection = onLocationSelection;

        // Set up the input
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(context).inflate(R.layout.location_picker, null);
        final AutoCompleteTextView input = (AutoCompleteTextView) view.findViewById(R.id.localityName);

        // Set up the AutoCompleteTextView with adapter and callbacks.
        input.setAdapter(new PlacesAdapter(context,
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
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                close(input.getText().toString());
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
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
        new LatLngFetcherTask(context, onLocationSelection).execute(selectedPlace);
        if(dialog != null && dialog.isShowing()) {
            dialog.cancel();
        }
    }

}
