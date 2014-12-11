package com.eventshigh.nearme.app.utils;

import android.app.Activity;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * This is an Widget which is used to show user day selection for upcoming week.
 */
public class DaySelector {

    private static final int NUM_DAYS = 14;
    private static final SimpleDateFormat DAY = new SimpleDateFormat("EE");
    private static final SimpleDateFormat DATE = new SimpleDateFormat("d MMM");

    public interface DaySelectionListener {
        public void onDaySelection(int dayNo);
    }

    public static LinearLayout getDaySelectorItem(Activity activity, ViewGroup parent, Date date,
                                                  @Nullable TimeZone timeZone) {
        LinearLayout daySelectorItem =
                (LinearLayout) activity.getLayoutInflater().inflate(
                        R.layout.day_selector_item, parent, false);
        ((TextView)daySelectorItem.findViewById(R.id.day_of_week)).setText(DAY.format(date));
        ((TextView)daySelectorItem.findViewById(R.id.date)).setText(DATE.format(date));

        TextView timeView = ((TextView)daySelectorItem.findViewById(R.id.event_time));
        String time = timeZone == null ? null : Utils.getTimeString(date, timeZone);
        if (time == null) {
            timeView.setVisibility(View.GONE);
        } else {
            timeView.setText(time);
        }

        daySelectorItem.setClickable(true);
        return daySelectorItem;
    }

    private final Activity activity;
    private final ViewGroup parent;
    private DaySelectionListener daySelectionListener = null;
    private LinearLayout[] daySelectorItems = new LinearLayout[NUM_DAYS];
    private int selectedDay = -1;

    public DaySelector(Activity activity, ViewGroup parent) {
        this.activity = activity;
        this.parent = parent;
    }

    public void populate() {
        parent.removeAllViews();

        int minWidth = getMinWidth();
        for (int i = 0; i < NUM_DAYS; i++) {
            Date date = Utils.getDate(i);
            LinearLayout daySelectorItem = getDaySelectorItem(activity, parent, date, null);
            daySelectorItem.setMinimumWidth(minWidth);
            daySelectorItem.setOnClickListener(new DayItemOnClickListener(i));
            parent.addView(daySelectorItem);
            daySelectorItems[i] = daySelectorItem;
        }

        setSelected(0);
    }

    public void setDaySelectionListener(DaySelectionListener daySelectionListener) {
        this.daySelectionListener = daySelectionListener;
    }

    public int getSelectedDay() {
        return selectedDay;
    }

    public void setSelected(int selectedDayNo) {
        if (selectedDay >= 0) {
            daySelectorItems[selectedDay].setBackgroundColor(
                    activity.getResources().getColor(R.color.app_color));
            ((TextView) daySelectorItems[selectedDay].findViewById(R.id.day_of_week)).setTextColor(
                    activity.getResources().getColor(R.color.day_selector_day_week_text));
        }

        daySelectorItems[selectedDayNo].setBackgroundColor(
                activity.getResources().getColor(R.color.day_selector_pressed_color));
        ((TextView)daySelectorItems[selectedDayNo].findViewById(R.id.day_of_week)).setTextColor(
                activity.getResources().getColor(android.R.color.white));

        selectedDay = selectedDayNo;
    }

    private class DayItemOnClickListener implements  OnClickListener {
        private final int dayItemNo;

        private DayItemOnClickListener(int dayItemNo) {
            this.dayItemNo = dayItemNo;
        }

        @Override
        public void onClick(View view) {
            setSelected(dayItemNo);
            if (daySelectionListener != null) {
                daySelectionListener.onDaySelection(dayItemNo);
            }
        }
    }

    // Get MinWidth for daySelector item.
    private int getMinWidth() {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x / 7;
    }
}
