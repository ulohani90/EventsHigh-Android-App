package com.eventshigh.nearme.app.ui;


import android.content.Context;

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
