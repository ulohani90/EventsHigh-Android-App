package com.eventshigh.nearme.app.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.DatePicker;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseEventsActivity;
import com.eventshigh.nearme.app.data.City;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * An {@link android.app.DialogFragment} which can used to show date picker for user. On Date
 * selection, this dialog launches a search activity with date as query.
 */
public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener, DialogInterface.OnClickListener {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private Date today;
    private DatePickerDialog datePicker;

    @Override
    @SuppressWarnings("deprecation")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        City city = null;
        Bundle args = getArguments();
        if (args != null) {
            String cityStr = args.getString(City.class.getName());
            if (cityStr != null) {
                city = City.valueOf(cityStr);
            }
        }

        Calendar cal = Calendar.getInstance();
        if (city != null) {
            cal.setTimeZone(TimeZone.getTimeZone(city.timeZone));
        }

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        today = new Date(year - 1900, month, day);

        datePicker = new DatePickerDialog(getActivity(), this, year, month, day);
        datePicker.setCancelable(true);
        datePicker.setCanceledOnTouchOutside(true);
        datePicker.getDatePicker().setMinDate(today.getTime());
        datePicker.getDatePicker().setMaxDate(today.getTime() + 7 * 24 * 3600 * 1000L);

        datePicker.setButton(DialogInterface.BUTTON_POSITIVE,
                getActivity().getString(R.string.menu_filter), this);
        datePicker.getDatePicker().setCalendarViewShown(false);
        return datePicker;
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onClick(DialogInterface dialog, int which) {
        Date selectedDate = new Date(datePicker.getDatePicker().getYear() - 1900,
                datePicker.getDatePicker().getMonth(),
                datePicker.getDatePicker().getDayOfMonth());
        long numDaysAhead = (selectedDate.getTime() - today.getTime()) / (24 * 3600 * 1000L);

        BaseEventsActivity activity = (BaseEventsActivity) getActivity();
        activity.reportActionToAnalytics("filterByDate", Long.toString(numDaysAhead) + "days later");
        activity.showSearchView(DATE_FORMAT.format(selectedDate));
    }
}
