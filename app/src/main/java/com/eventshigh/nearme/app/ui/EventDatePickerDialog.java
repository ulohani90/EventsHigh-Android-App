package com.eventshigh.nearme.app.ui;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.squareup.timessquare.CalendarCellDecorator;
import com.squareup.timessquare.CalendarPickerView;
import com.squareup.timessquare.DefaultDayViewAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

/**
 * Created by umesh on 02/06/16.
 */
public class EventDatePickerDialog {

    public static void showDatePickerDialog(Context context) {
       /* final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.date_picker_dialog_layout);
        final Calendar nextYear = Calendar.getInstance();
        nextYear.add(Calendar.YEAR, 1);

        final Calendar lastYear = Calendar.getInstance();
        lastYear.add(Calendar.YEAR, -1);

        CalendarPickerView calendar = (CalendarPickerView) dialog.findViewById(R.id.calendar_view);

        Calendar today = Calendar.getInstance();
        ArrayList<Date> dates = new ArrayList<Date>();
        for (int i = 0; i < 5; i++) {
            today.add(Calendar.DAY_OF_MONTH, 3);
            dates.add(today.getTime());
        }
        calendar.setCustomDayView(new DefaultDayViewAdapter());
        calendar.setDecorators(Collections.<CalendarCellDecorator>emptyList());
        calendar.init(new Date(), nextYear.getTime()) //
                .inMode(CalendarPickerView.SelectionMode.MULTIPLE) //
                .withSelectedDates(dates);

        TextView confirmBtn = (TextView) dialog.findViewById(R.id.confirm_btn);
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();*/
        
    }
}
