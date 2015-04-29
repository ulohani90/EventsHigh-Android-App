package com.eventshigh.nearme.app.ui;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.widget.NumberPicker;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;

/**
 * Dialog box to show number picker -- used to selecting number of tickets.
 */
public class NumberPickerDialog {
    public interface Callback {
        void onSelection(int num);
    }

    public static void show(final BaseActivity activity, int title, final Callback callback) {
        @SuppressLint("InflateParams")
        final NumberPicker np = (NumberPicker)
                activity.getLayoutInflater().inflate(R.layout.dialog_number_picker, null);
        np.setMinValue(1);
        np.setMaxValue(10);
        np.setValue(1);

        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(np)
                .setPositiveButton(R.string.action_book, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        activity.reportActionToAnalytics("numTicketAccepted");
                        callback.onSelection(np.getValue());
                    }
                })
                .setNegativeButton(R.string.rate_app_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        activity.reportActionToAnalytics("numTicketRejected");
                    }
                })
                .setCancelable(true)
                .show();
    }
}
