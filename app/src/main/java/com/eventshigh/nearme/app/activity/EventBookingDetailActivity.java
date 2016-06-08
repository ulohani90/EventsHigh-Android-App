package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.stream.EhPrices;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import pl.snowdog.material.ui.ToolbarColorizeHelper;

/**
 * Created by umesh on 19/05/16.
 */
public class EventBookingDetailActivity extends BaseActivity implements View.OnClickListener {

    public static final String EVENT_TOTAL_TICKETS = "event_total_tickets";
    public static final String EVENT_DATE_SELECTED = "event_date_selected";
    public static final String EVENT_TIME_SELECTED = "event_time_selected";
    public static final String EVENT_TOTAL_PRICE = "total_price";
    public static final String EVENT_TICKETS_DESCRIPTION = "tickets_description";

    Event event;
    Toolbar toolbar;
    double total = 0;
    double noOfTickets = 0;
    TextView totalPrice;
    TextView numberOfTickets;
    String currency;
    TextView tvNextEventBooking;

    String dateString;
    EventTime eventTime;
    ArrayList<EhPrices> prices;
    ArrayList<EventTime> timings;

    TextView validOnExtraDays;

    FrameLayout ticketContainerLayout, comboContainerLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_booking_detail);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ScrollView scrollView = (ScrollView) findViewById(R.id.booking_scrollview);
        if (getIntent().hasExtra("event")) {
            event = getIntent().getParcelableExtra("event");
        }

        totalPrice = (TextView) findViewById(R.id.total_price);
        numberOfTickets = (TextView) findViewById(R.id.total_tickets);

        validOnExtraDays = (TextView) findViewById(R.id.valid_on_extras_days);
        ticketContainerLayout = (FrameLayout) findViewById(R.id.ticket_container_layout);
        comboContainerLayout = (FrameLayout) findViewById(R.id.combo_ticket_container);
        getTimingSlots();

        if (eventTimes.size() == 0) {
            findViewById(R.id.ll_sold_out_screen).setVisibility(View.VISIBLE);
            if (!Utils.checkIfStringEmpty(event.organizerPhone))
                ((TextView) findViewById(R.id.tv_sold_out_text)).setText("They may be available at venue. Please check with organizers at this " + event.organizerPhone + " .");
            getSupportActionBar().setTitle("Sold out");
        } else {
            addDateContainerData();
            addTimeContainerData(0);
            addEventTickets(0, 0);
            addEventComboTickets(0, 0);
            updateTotalPrice();

            tvNextEventBooking = (TextView) findViewById(R.id.tv_next_event_booking);
            tvNextEventBooking.setOnClickListener(this);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_next_event_booking:
                if (noOfTickets > 0) {
                    Intent iNext = new Intent(this, GuestDetailActivity.class);
                    Bundle bundleNext = new Bundle();
                    bundleNext.putParcelable(EventDetailActivity.EVENT_OBJECT, event);
                    bundleNext.putDouble(EVENT_TOTAL_TICKETS, noOfTickets);
                    bundleNext.putDouble(EVENT_TOTAL_PRICE, total);
                    bundleNext.putString(EVENT_DATE_SELECTED, dateString);
                    bundleNext.putParcelable(EVENT_TIME_SELECTED, eventTime);
                    bundleNext.putParcelableArrayList(EVENT_TICKETS_DESCRIPTION, prices);

                    iNext.putExtras(bundleNext);
                    startActivity(iNext);
                } else {
                    Toast.makeText(this, "Please select your ticket(s)", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (toolbar != null)
            setLightToolbarIcons();
    }

    private void setLightToolbarIcons() {
        toolbar.post(new Runnable() {
            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                ToolbarColorizeHelper.colorizeToolbar(toolbar,
                        getResources().getColor(android.R.color.white), EventBookingDetailActivity.this);
            }
        });
    }

    HashMap<String, ArrayList<EventTime>> eventTimes;

    TextView dateLayoutSelectedLast;

    TextView timeLayoutLastSelected;

    public void addDateContainerData(){
        LinearLayout dateContainer = (LinearLayout) findViewById(R.id.date_container);
        for (int i = 0; i < dates.size(); i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.ticket_date_time_count_container, dateContainer, false);

            final TextView dayText = (TextView) view.findViewById(R.id.event_day);
            dayText.setVisibility(View.VISIBLE);

            if (i == 0) {
                dayText.setSelected(true);
                dateLayoutSelectedLast = dayText;
                dateString = dates.get(0);
            }

            TextView timeLayout = (TextView) view.findViewById(R.id.time_textview);
            timeLayout.setVisibility(View.GONE);
            SpannableString date = new SpannableString(eventTimes.get(dates.get(i)).get(0).day + "\n" + eventTimes.get(dates.get(i)).get(0).date);
            date.setSpan(new StyleSpan(Typeface.BOLD), 0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            date.setSpan(new RelativeSizeSpan(0.8f), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            dayText.setText(date);
            view.setTag(i);
            dateContainer.addView(view);
            dateContainer.setTag(i + "");
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (Integer) v.getTag();
                    if (dateLayoutSelectedLast != null) {
                        dateLayoutSelectedLast.setSelected(false);
                    }
                    dateString = dates.get(Integer.parseInt(v.getTag().toString()));
                    dayText.setSelected(true);
                    dateLayoutSelectedLast = dayText;

                    addTimeContainerData(position);
                    addEventTickets(position, 0);
                    addEventComboTickets(position, 0);
                    total = noOfTickets = 0;
                    updateTotalPrice();
                }
            });
        }

    }

    public void addTimeContainerData(final int position) {
        LinearLayout timeContainer = (LinearLayout) findViewById(R.id.time_container);
        timings = eventTimes.get(dates.get(position));
        timeContainer.removeAllViews();
        for (int i = 0; i < timings.size(); i++) {

            View view = LayoutInflater.from(this).inflate(R.layout.ticket_date_time_count_container, timeContainer, false);
            TextView dayText = (TextView) view.findViewById(R.id.event_day);
            dayText.setVisibility(View.GONE);
            final TextView timeLayout = (TextView) view.findViewById(R.id.time_textview);
            timeLayout.setVisibility(View.VISIBLE);
            timeLayout.setText(timings.get(i).time);
            if (i == 0) {
                timeLayout.setSelected(true);
                timeLayoutLastSelected = timeLayout;
                eventTime = timings.get(0);

            }
            view.setTag(i);
            timeContainer.addView(view);
            timeContainer.setTag(i + "");
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int timeIndex = (Integer) v.getTag();
                    if (timeLayoutLastSelected != null) {
                        timeLayoutLastSelected.setSelected(false);
                    }
                    eventTime = timings.get(Integer.parseInt(v.getTag().toString()));
                    timeLayout.setSelected(true);
                    timeLayoutLastSelected = timeLayout;
                    addEventTickets(position, timeIndex);
                    addEventComboTickets(position, timeIndex);
                    total = noOfTickets = 0;
                    updateTotalPrice();
                }
            });
        }
    }

    List<String> dates;

    public void getTimingSlots() {
        dates = new ArrayList<>();
        eventTimes = new HashMap<>();

        for (int i = 0; i < event.eventTimings.length; i++) {
            EventTime time = DateTimeUtils.getEventTime(event, i);
            if (getEhPricesSize(time) > 0) {
                if (eventTimes.containsKey(time.date)) {
                    eventTimes.get(time.date).add(time);
                } else {
                    ArrayList<EventTime> timings = new ArrayList<>();
                    timings.add(time);
                    eventTimes.put(time.date, timings);
                    dates.add(time.date);
                }
            }
        }

    }

    public void addEventTickets(int dateIndex, int timeIndex) {
        prices = getEhPrices(dateIndex, timeIndex);
        LinearLayout ticketTypes = (LinearLayout) findViewById(R.id.options_container);
        ticketTypes.removeAllViews();
        for (int i = 0; i < prices.size(); i++) {

            final EhPrices price = prices.get(i);
            if (!price.isMulti) {

                View view = LayoutInflater.from(this).inflate(R.layout.eh_ticket_type_layout, ticketTypes, false);
                TextView ticketType = (TextView) view.findViewById(R.id.ticket_name);
                TextView ticketDesc = (TextView) view.findViewById(R.id.ticket_desc);
                TextView ticketPrice = (TextView) view.findViewById(R.id.ticket_price);
                TextView ticketDiscountedPrice = (TextView) view.findViewById(R.id.ticket_discounted_price);
                final TextView ticketCount = (TextView) view.findViewById(R.id.ticket_count);
                TextView ticketCountIncrement = (TextView) view.findViewById(R.id.ticket_count_increment);
                TextView ticketCountDecrement = (TextView) view.findViewById(R.id.ticket_count_decrement);

                ticketType.setText(price.name);

                if (price.note != null && price.note.length() > 0) {
                    ticketDesc.setText(price.note);
                    ticketDesc.setVisibility(View.VISIBLE);
                } else {
                    ticketDesc.setVisibility(View.GONE);
                }
                currency = price.currency;
                ticketCount.setText(0 + "");
                ticketCount.setTag(i + "");
                ticketCountIncrement.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int ticketNo = Integer.parseInt(ticketCount.getText().toString());
                        if (ticketNo < 10) {

                            ticketNo++;
                            if (ticketNo > 0) {
                                if (comboContainerLayout.isShown()) {
                                    findViewById(R.id.combo_disable_view).setVisibility(View.VISIBLE);
                                }
                            }
                            prices.get(Integer.parseInt(ticketCount.getTag().toString())).count = ticketNo;
                            ticketCount.setText(ticketNo + "");
                            noOfTickets += 1;
                            if (price.discountValue < 0.01) {
                                total += price.value;
                            } else {
                                total += price.discountValue;
                            }
                            updateTotalPrice();
                        } else {
                            Toast.makeText(getApplicationContext(), "Maximum of 10 tickets can be booked. Please contact for cooperate bookings.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                ticketCountDecrement.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int ticketNo = Integer.parseInt(ticketCount.getText().toString());
                        if (ticketNo != 0) {
                            ticketNo--;
                            if (ticketNo == 0) {
                                if (comboContainerLayout.isShown()) {
                                    findViewById(R.id.combo_disable_view).setVisibility(View.GONE);
                                }
                            }
                            prices.get(Integer.parseInt(ticketCount.getTag().toString())).count = ticketNo;
                            ticketCount.setText(ticketNo + "");
                            noOfTickets -= 1;
                            if (price.discountValue < 0.01) {
                                total -= price.value;
                            } else {
                                total -= price.discountValue;
                            }
                            updateTotalPrice();
                        }
                    }
                });
                if (price.discountValue > 0) {
                    ticketPrice.setTextColor(Color.parseColor("#C0C0C0"));
                    ticketPrice.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
                    ticketPrice.setText(price.currency + " " + price.value);
                    ticketDiscountedPrice.setText(price.currency + " " + price.discountValue);
                    ticketPrice.setPaintFlags(ticketPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    ticketPrice.setTextColor(Color.parseColor("#353535"));
                    ticketPrice.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                    ticketPrice.setText(price.currency + " " + price.value);
                    ticketPrice.setPaintFlags(ticketPrice.getPaintFlags());
                }
                ticketTypes.addView(view);
            }
        }

    }


    public void addEventComboTickets(int dateIndex, int timeIndex) {
        prices = getEhPrices(dateIndex, timeIndex);
        TextView comboTicketTitle = (TextView) findViewById(R.id.combo_tickets_title);
        LinearLayout ticketTypes = (LinearLayout) findViewById(R.id.combo_options_container);
        ticketTypes.removeAllViews();
        boolean showView = false;
        for (int i = 0; i < prices.size(); i++) {

            final EhPrices price = prices.get(i);
            if ((price.isMulti && isFirstOccurence(dateIndex, timeIndex, price))){
                showView = true;
                View view = LayoutInflater.from(this).inflate(R.layout.eh_ticket_type_layout, ticketTypes, false);
                TextView ticketType = (TextView) view.findViewById(R.id.ticket_name);
                TextView ticketDesc = (TextView) view.findViewById(R.id.ticket_desc);
                TextView ticketPrice = (TextView) view.findViewById(R.id.ticket_price);
                TextView ticketDiscountedPrice = (TextView) view.findViewById(R.id.ticket_discounted_price);
                final TextView ticketCount = (TextView) view.findViewById(R.id.ticket_count);
                TextView ticketCountIncrement = (TextView) view.findViewById(R.id.ticket_count_increment);
                TextView ticketCountDecrement = (TextView) view.findViewById(R.id.ticket_count_decrement);
                comboTicketTitle.setText(getExtraString(price));
                ticketType.setText(price.name);


                if (price.note != null && price.note.length() > 0) {
                    ticketDesc.setText(price.note);
                    ticketDesc.setVisibility(View.VISIBLE);
                } else {
                    ticketDesc.setVisibility(View.GONE);
                }
                currency = price.currency;
                ticketCount.setText(0 + "");
                ticketCount.setTag(i + "");
                ticketCountIncrement.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int ticketNo = Integer.parseInt(ticketCount.getText().toString());
                        if (ticketNo < 10) {
                            ticketNo++;
                            if (ticketNo > 0){
                                findViewById(R.id.ticket_disable_view).setVisibility(View.VISIBLE);
                            }
                            prices.get(Integer.parseInt(ticketCount.getTag().toString())).count = ticketNo;
                            ticketCount.setText(ticketNo + "");
                            noOfTickets += 1;
                            if (price.discountValue < 0.01){
                                total += price.value;
                            } else {
                                total += price.discountValue;
                            }
                            updateTotalPrice();
                        } else {
                            Toast.makeText(getApplicationContext(), "Maximum of 10 tickets can be booked. Please contact for cooperate bookings.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                ticketCountDecrement.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int ticketNo = Integer.parseInt(ticketCount.getText().toString());
                        if (ticketNo != 0) {
                            ticketNo--;
                            if (ticketNo == 0) {
                                findViewById(R.id.ticket_disable_view).setVisibility(View.GONE);
                            }
                            prices.get(Integer.parseInt(ticketCount.getTag().toString())).count = ticketNo;
                            ticketCount.setText(ticketNo + "");
                            noOfTickets -= 1;
                            if (price.discountValue < 0.01) {
                                total -= price.value;
                            }else{
                                total -= price.discountValue;
                            }
                            updateTotalPrice();
                        }
                    }
                });

                if (price.discountValue > 0){
                    ticketPrice.setTextColor(Color.parseColor("#C0C0C0"));
                    ticketPrice.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
                    ticketPrice.setText(price.currency + " " + price.value);
                    ticketDiscountedPrice.setText(price.currency + " " + price.discountValue);
                    ticketPrice.setPaintFlags(ticketPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    ticketPrice.setTextColor(Color.parseColor("#353535"));
                    ticketPrice.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                    ticketPrice.setText(price.currency + " " + price.value);
                    ticketPrice.setPaintFlags(ticketPrice.getPaintFlags());
                }
                ticketTypes.addView(view);
            }
        }
        if (showView) {
            comboContainerLayout.setVisibility(View.VISIBLE);
        } else {
            comboContainerLayout.setVisibility(View.GONE);
        }

    }

    public String getExtraString(EhPrices price) {
        StringBuilder builder = new StringBuilder();

        if (price.occurences.size() >= 2) {
            if (DateTimeUtils.dateToEventTime(new Date(price.occurences.get(0)), TimeZone.getTimeZone(event.city.timeZone)).date.equalsIgnoreCase(DateTimeUtils.dateToEventTime(new Date(price.occurences.get(1)), TimeZone.getTimeZone(event.city.timeZone)).date)) {

                builder.append("For " + DateTimeUtils.dateToEventTime(new Date(price.occurences.get(0)), TimeZone.getTimeZone(event.city.timeZone)).date + ", ");
                for (int i = 0; i < price.occurences.size(); i++) {
                    if (i != 0) {
                        builder.append(" & ");
                    }
                    builder.append(DateTimeUtils.dateToEventTime(new Date(price.occurences.get(i)), TimeZone.getTimeZone(event.city.timeZone)).time);
                }

            } else {
                builder.append("For ");
                EventTime time = null;
                for (int i = 0; i < price.occurences.size(); i++) {
                    if (i != 0) {
                        builder.append(" - ");
                    }
                    time = DateTimeUtils.dateToEventTime(new Date(price.occurences.get(i)), TimeZone.getTimeZone(event.city.timeZone));
                    builder.append(time.day + " " + time.date);
                }

                builder.append(", " + time.time);
            }
        }
        SpannableString string = new SpannableString(builder.toString());
        string.setSpan(new StyleSpan(Typeface.BOLD), 0, 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return string.toString();
    }

    public boolean isFirstOccurence(int dateIndex, int timeIndex, EhPrices price) {
        EventTime time = eventTimes.get(dates.get(dateIndex)).get(timeIndex);
        if (DateTimeUtils.dateToEventTime(new Date(price.occurences.get(0)), TimeZone.getTimeZone(event.city.timeZone)).equals(time)) {
            return true;
        }
        return false;
    }


    public ArrayList<EhPrices> getEhPrices(int dateIndex, int timeIndex) {
        ArrayList<EhPrices> results = new ArrayList<>();
        EventTime eventTime = eventTimes.get(dates.get(dateIndex)).get(timeIndex);
        for (int i = 0; i < event.ehPrices.size(); i++) {
            EhPrices ehPrices = event.ehPrices.get(i);
            long eventTimeLong = System.currentTimeMillis();
            if (ehPrices.validityStart <= eventTimeLong && ehPrices.validityStop > eventTimeLong) {
                for (int j = 0; j < event.ehPrices.get(i).occurences.size(); j++) {
                    EventTime time = DateTimeUtils.dateToEventTime(new Date(ehPrices.occurences.get(j)), TimeZone.getTimeZone(event.city.timeZone));
                    if (eventTime.equals(time)) {
                        results.add(ehPrices);
                    }
                }
            }
        }
        return results;
    }

    public int getEhPricesSize(EventTime eventTime) {
        ArrayList<EhPrices> results = new ArrayList<>();
        for (int i = 0; i < event.ehPrices.size(); i++) {
            EhPrices ehPrices = event.ehPrices.get(i);
            long eventTimeLong = System.currentTimeMillis();
            if (ehPrices.validityStart <= eventTimeLong && ehPrices.validityStop > eventTimeLong) {
                for (int j = 0; j < event.ehPrices.get(i).occurences.size(); j++) {
                    EventTime time = DateTimeUtils.dateToEventTime(new Date(ehPrices.occurences.get(j)), TimeZone.getTimeZone(event.city.timeZone));
                    if (eventTime.equals(time)) {
                        results.add(ehPrices);
                    }
                }
            }
        }
        return results.size();
    }


    public void updateTotalPrice() {
        totalPrice.setText(currency + " " + Math.round(total));
        numberOfTickets.setText(Math.round(noOfTickets) + "");
    }


}
