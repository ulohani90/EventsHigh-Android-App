package com.eventshigh.nearme.app.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.stream.EhPrices;
import com.eventshigh.nearme.app.network.EventCollectionRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.ui.animation.ResizeAnimation;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;
import com.squareup.timessquare.CalendarPickerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by umesh on 29/05/16.
 */
public class NewWeekEventsFragment extends BaseEventsFragment {


    public static NewWeekEventsFragment getInstance(EventsContext eventsContext, boolean showEhInviteForNotification) {
        NewWeekEventsFragment fragment = new NewWeekEventsFragment();
        Bundle args = getArgs(eventsContext, false, false);
        args.putBoolean(SHOW_EH_INVITE_NOTIFICATION_PARAM, showEhInviteForNotification);
        fragment.setArguments(args);
        return fragment;
    }

    private static final String SHOW_EH_INVITE_NOTIFICATION_PARAM =
            EventsFragment.class.getName() + "_show_eh_invite_param";
    private AutofitRecyclerView eventGridView;
    private View topProgressBar;

    private final Calendar todayDate = DateTimeUtils.toMidnight(Calendar.getInstance(), null);

    EventsAdapter eventsAdapter;
    EventCollectionRequest.EventsCollection eventsCollection;
    View noMyEventsView;
    View retryView;

    private boolean showEhInviteForNotification;
    View view;

    LinearLayout filtersContainer;
    HorizontalScrollView dateFilter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_by_week, container, false);
        eventsAdapter = new EventsAdapter(activity);
        eventGridView = (AutofitRecyclerView) view.findViewById(R.id.event_grid);
        eventGridView.setAdapter(eventsAdapter);
        filtersContainer = (LinearLayout) view.findViewById(R.id.filters_container);
        noMyEventsView = view.findViewById(R.id.view_no_my_event);

        dateFilter = (HorizontalScrollView) view.findViewById(R.id.date_filter);
        retryView = view.findViewById(R.id.view_retry);
        retryView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("retry");
                makeServerRequest(false);
            }
        });
        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setEnabled(false);

        // Setup the actionbar hide/show on scroll.
        /*((CoordinatorLayout)view.findViewById(R.id.main_content)).setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {

            }
        });*/
        eventGridView.addOnScrollListener(onScrollListener);

        topProgressBar = view.findViewById(R.id.top_progress_bar);
        return view;
    }


    private RecyclerView.OnScrollListener onScrollListener;

    public void onAttach(Context context) {
        super.onAttach(context);

        showEhInviteForNotification = getArguments().getBoolean(SHOW_EH_INVITE_NOTIFICATION_PARAM);
        if (onScrollListener == null) {
            onScrollListener = new HideActionBarOnScroll(this.activity) {
                int y;

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        y = 0;
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    y += dy;
                    if (y > 250) {
                        //collapseAnimation();
                    }
                }
            };
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addFiltersData();
    }

    @Override
    public void onStart() {
        super.onStart();


        makeServerRequest(false);
    }

    public void makeServerRequest(boolean shouldBypassCache) {
        Calendar endDate = Calendar.getInstance();
        endDate.setFirstDayOfWeek(Calendar.MONDAY);
        endDate.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        endDate.add(Calendar.DAY_OF_WEEK, 8);
        eventsContext.dateFilter = EventsContext.formatDateFilter(Calendar.getInstance()) + "," + EventsContext.formatDateFilter(endDate);
        EventCollectionRequest.submit(activity, eventsContext, Request.Priority.IMMEDIATE, this,
                shouldBypassCache, true, mEventsFetcherCallBack, mErrorListener);
    }

    private Response.Listener<EventCollectionRequest.EventsCollection> mEventsFetcherCallBack = new Response.Listener<EventCollectionRequest.EventsCollection>() {
        @Override
        public void onResponse(EventCollectionRequest.EventsCollection eventsCollection, boolean isIntermediate) {
            if (isDetached()) {
                return;
            }

            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (eventsCollection.events.isEmpty()) {
                    // Failed. Show toast and return empty list.
                    noMyEventsView.setVisibility(View.VISIBLE);
                    Toast.makeText(getActivity(), R.string.no_events, Toast.LENGTH_SHORT).show();

                }
            }

            if (!isIntermediate || !eventsCollection.events.isEmpty()) {
                noMyEventsView.setVisibility(View.GONE);
                String seeAllQuery = eventsContext.query.isEmpty() ||
                        eventsContext.dateFilter.isEmpty() ? null : eventsContext.query;
                if (getActivity() != null && (getActivity()) instanceof EventsGridActivity) {
                    if (eventsCollection.events.size() > 0)
                        ((EventsGridActivity) getActivity()).setShareImageUrl(eventsCollection.events.get(0).imgUrl);
                }
                NewWeekEventsFragment.this.eventsCollection = eventsCollection;
                List<Event> filteredEvents = eventsCollection.events;
                filteredEvents = filterEventsWithCategory(null, filteredEvents);
                filteredEvents = filterEventsWithDate(filteredEvents, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime());
                filteredEvents = filterEventsWithPrice(filteredEvents, -1);
                eventsAdapter.setEvents(filteredEvents, seeAllQuery, false);
                eventsContext.removeDateFilter();
                /*if (showFollowCard) {
                    eventsAdapter.addFollowCard(eventsContext.query, eventsCollection.events.size(),
                            eventsCollection.numFollowers, special);
                }*/
                //     eventGridView.scrollToPosition(scrollPosition);
            }
        }
    };

    private Response.ErrorListener mErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            if (isDetached()) {
                return;
            }

            topProgressBar.setVisibility(View.GONE);
            if (eventsAdapter.getItemCount() > 0) {
                activity.showMessage(R.string.failed_refresh);
            } else {
                retryView.setVisibility(View.VISIBLE);
            }
            eventsContext.removeDateFilter();

            VolleyHelper.log(activity, volleyError);

        }
    };

    ArrayList<String> filterCategoryName;

    public List<Event> filterEventsWithCategory(String category, List<Event> totalEvents) {
        List<Event> allEvents;

        if (totalEvents == null) {
            allEvents = eventsCollection.events;
        } else {
            allEvents = totalEvents;
        }

        if (filterCategoryName == null)
            filterCategoryName = new ArrayList<>();
        if (category != null) {
            if (filterCategoryName.contains(category)) {
                filterCategoryName.remove(category);
            } else {
                filterCategoryName.add(category);
            }
        }
        List<Event> filteredEvents = new ArrayList<>();
        if (filterCategoryName.size() > 0) {
            for (int i = 0; i < allEvents.size(); i++) {
                for (int j = 0; j < filterCategoryName.size(); j++) {
                    if (allEvents.get(i).tags.contains(filterCategoryName.get(j))) {
                        filteredEvents.add(allEvents.get(i));
                        break;
                    }
                }
            }
        } else {
            filteredEvents = allEvents;
        }

        if (totalEvents == null) {
            filteredEvents = filterEventsWithDate(filteredEvents, -1);
            filteredEvents = filterEventsWithPrice(filteredEvents, -1);
            eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
            if (filteredEvents.size() > 0) {

                noMyEventsView.setVisibility(View.GONE);
            } else {
                noMyEventsView.setVisibility(View.VISIBLE);
                // Failed. Show toast and return empty list.
                Toast.makeText(getActivity(), R.string.no_events, Toast.LENGTH_SHORT).show();


            }
        }
        return filteredEvents;

    }


    ArrayList<Long> filterEventTimes;

    public List<Event> filterEventsWithDate(List<Event> totalEvents, long... times) {
        List<Event> allEvents;
        if (totalEvents == null) {
            allEvents = eventsCollection.events;
        } else {
            allEvents = totalEvents;
        }
        if (filterEventTimes == null) {
            filterEventTimes = new ArrayList<>();
        }
        for (long time : times) {
            if (time != -1) {
                if (filterEventTimes.contains(time)) {
                    filterEventTimes.remove(time);
                } else {
                    filterEventTimes.add(time);
                }
            }
        }
        List<Event> filteredEvents = new ArrayList<>();
        if (filterEventTimes.size() > 0) {
            for (int i = 0; i < allEvents.size(); i++) {
                for (int j = 0; j < filterEventTimes.size(); j++) {
                    for (int k = 0; k < allEvents.get(i).eventTimings.length; k++) {
                        if (filterEventTimes.get(j) == DateTimeUtils.getEventDate(allEvents.get(i), k).getTime()) {
                            filteredEvents.add(allEvents.get(i));
                            break;
                        }
                    }
                }
            }
        } else {
            filteredEvents = allEvents;
        }

        if (totalEvents == null) {
            filteredEvents = filterEventsWithCategory(null, filteredEvents);
            filteredEvents = filterEventsWithPrice(filteredEvents, -1);
            eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
            if (filteredEvents.size() > 0) {

                noMyEventsView.setVisibility(View.GONE);
            } else {
                noMyEventsView.setVisibility(View.VISIBLE);
                // Failed. Show toast and return empty list.
                Toast.makeText(getActivity(), R.string.no_events, Toast.LENGTH_SHORT).show();

            }
        }
        return filteredEvents;

    }

    List<Integer> filterEventPrices;

    public List<Event> filterEventsWithPrice(List<Event> totalEvents, int priceValue) {
        List<Event> allEvents;
        if (totalEvents == null) {
            allEvents = eventsCollection.events;
        } else {
            allEvents = totalEvents;
        }
        if (filterEventPrices == null) {
            filterEventPrices = new ArrayList<>();
        }

        if (priceValue != -1) {
            if (filterEventPrices.contains(priceValue)) {
                filterEventPrices.remove((Integer) priceValue);
            } else {
                filterEventPrices.add((Integer) priceValue);
            }
        }


        List<Event> filteredEvents = new ArrayList<>();
        if (filterEventPrices.size() > 0) {
            for (int i = 0; i < allEvents.size(); i++) {
                Event event = allEvents.get(i);

                secondLoop:
                for (int j = 0; j < filterEventPrices.size(); j++) {
                    int priceMin = -1;
                    int priceMax = -1;
                    if (filterEventPrices.get(j) == EventsGridActivity.FREE) {
                        priceMax = 0;
                        priceMin = 0;
                    } else if (filterEventPrices.get(j) == EventsGridActivity.UPTO_250) {
                        priceMin = 1;
                        priceMax = 100;
                    } else if (filterEventPrices.get(j) == EventsGridActivity.PRICE_250_TO_750) {
                        priceMin = 100;
                        priceMax = 200;
                    } else {
                        priceMin = 200;
                        priceMax = 50000;
                    }
                    if (event.ehPrices.size() > 0) {
                        for (EhPrices price : event.ehPrices) {
                            if (price.discountValue > 0) {
                                if (price.discountValue >= priceMin && price.discountValue <= priceMax) {
                                    filteredEvents.add(event);
                                    break secondLoop;
                                }
                            } else {
                                if (price.value >= priceMin && price.value <= priceMax) {
                                    filteredEvents.add(event);
                                    break secondLoop;
                                }
                            }
                        }
                    } else {
                        if (event.minPrice >= priceMin && event.maxPrice <= priceMax) {
                            filteredEvents.add(event);
                            break secondLoop;
                        }
                    }
                }

            }
        } else {
            filteredEvents = allEvents;
        }

        if (totalEvents == null) {
            filteredEvents = filterEventsWithCategory(null, filteredEvents);
            filteredEvents = filterEventsWithDate(filteredEvents, -1);
            eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
            if (filteredEvents.size() > 0) {

                noMyEventsView.setVisibility(View.GONE);
            } else {
                noMyEventsView.setVisibility(View.VISIBLE);
                // Failed. Show toast and return empty list.
                Toast.makeText(getActivity(), R.string.no_events, Toast.LENGTH_SHORT).show();

            }
        }
        return filteredEvents;
    }

    View showFiltersView;

    TextView showMoreFilterText;

    TextView selectCustomDates, today, tomorrow, weekend;

    public void addFiltersData() {
        final LinearLayout horizontalCategories = (LinearLayout) view.findViewById(R.id.category_container);
        final EventCategory[] categories = EventCategory.values();
        for (int i = 0; i < categories.length; i++) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.filter_tags_layout, horizontalCategories, false);
            final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
            filterText.setText(categories[i].categoryName);
            horizontalCategories.addView(view);
            filterText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (filterText.isSelected()) {
                        filterText.setSelected(false);
                    } else {
                        filterText.setSelected(true);
                    }

                    filterEventsWithCategory(filterText.getText().toString(), null);
                }
            });
        }
        LinearLayout horizontalprice = (LinearLayout) view.findViewById(R.id.price_container);
        String[] priceRanges = {"Free", " \u20B9 ", "\u20B9 \u20B9", "\u20B9 \u20B9 \u20B9"};
        for (int i = 0; i < priceRanges.length; i++) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.filter_tags_layout, horizontalCategories, false);
            final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
            filterText.setText(priceRanges[i]);
            horizontalprice.addView(view);
            filterText.setTag(i);
            filterText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (Integer) filterText.getTag();
                    if (position == 0) {
                        filterEventsWithPrice(null, EventsGridActivity.FREE);
                    } else if (position == 1) {
                        filterEventsWithPrice(null, EventsGridActivity.UPTO_250);
                    } else if (position == 2) {
                        filterEventsWithPrice(null, EventsGridActivity.PRICE_250_TO_750);
                    } else {
                        filterEventsWithPrice(null, EventsGridActivity.MORE_THAN_750);
                    }
                    if (filterText.isSelected()) {
                        filterText.setSelected(false);
                    } else {
                        filterText.setSelected(true);
                    }
                }
            });
        }

        LinearLayout horizontalDate = (LinearLayout) view.findViewById(R.id.date_container);
        String[] dateRanges = {"Today", "Tomorrow", "Weekend", "Dates", "• • •"};
        for (int i = 0; i < dateRanges.length; i++) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.filter_tags_layout, horizontalCategories, false);
            final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
            filterText.setText(dateRanges[i]);

            if (i == dateRanges.length - 1) {
                showFiltersView = view;
                showMoreFilterText = filterText;
                showMoreFilterText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
                showMoreFilterText.setTypeface(null, Typeface.BOLD);
            } else if (i == dateRanges.length - 2) {
                selectCustomDates = filterText;
                selectCustomDates.setVisibility(View.GONE);
            } else if (i == dateRanges.length - 3) {
                weekend = filterText;
                weekend.setVisibility(View.GONE);
            } else if (i == dateRanges.length - 4) {
                tomorrow = filterText;
            } else {
                today = filterText;
                filterText.setSelected(true);
            }

            horizontalDate.addView(view);
            filterText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (filterText.getText().toString().equalsIgnoreCase("Today")) {
                        checkIfCustomDateSelected();
                        filterEventsWithDate(null, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime());
                    } else if (filterText.getText().toString().equalsIgnoreCase("Tomorrow")) {
                        checkIfCustomDateSelected();
                        filterEventsWithDate(null, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime() + DateTimeUtils.MILLISECONDS_IN_A_DAY);
                    } else if (filterText.getText().toString().equalsIgnoreCase("Weekend")) {
                        checkIfCustomDateSelected();
                        filterEventsWithDate(null, DateTimeUtils.getWeekEndDates());
                    } else if (filterText.getText().toString().equalsIgnoreCase("Dates")) {
                        showDateDialog();
                        Calendar currentYear = Calendar.getInstance();
                        Calendar nextYear = Calendar.getInstance();
                        nextYear.setFirstDayOfWeek(Calendar.MONDAY);
                        nextYear.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                        nextYear.add(Calendar.DAY_OF_WEEK, 8);

                        if (selectedDates != null) {
                            dialogView.init(currentYear.getTime(), nextYear.getTime())//
                                    .inMode(CalendarPickerView.SelectionMode.MULTIPLE).withSelectedDates(selectedDates);
                        } else {
                            dialogView.init(currentYear.getTime(), nextYear.getTime()) //
                                    .inMode(CalendarPickerView.SelectionMode.MULTIPLE);
                        }
                    } else if (filterText.getText().toString().equalsIgnoreCase("• • •")) {
                        expandAnimation();
                        return;
                    } else {
                        collapseAnimation();
                        return;
                    }
                    if (!(filterText.getText().toString().equalsIgnoreCase("Dates"))) {
                        if (filterText.isSelected()) {
                            filterText.setSelected(false);
                        } else {
                            filterText.setSelected(true);
                        }
                    }
                }
            });
        }

        collapseAnimation();
    }

    CalendarPickerView dialogView;

    public void showDateDialog() {
        dialogView = (CalendarPickerView) LayoutInflater.from(getActivity()).inflate(R.layout.date_picker_dialog_layout, null, false);
        AlertDialog theDialog = new AlertDialog.Builder(getActivity()) //
                .setTitle(null)
                .setView(dialogView)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getSelectedDates();
                        dialog.dismiss();
                    }
                })
                .create();
        theDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {

                dialogView.fixDialogDimens();
            }
        });
        theDialog.show();

    }

    List<Date> selectedDates;

    public void getSelectedDates() {
        selectedDates = dialogView.getSelectedDates();
        if (selectedDates != null && selectedDates.size() > 0) {
            removeAllSelectedDateFilters();
            selectCustomDates.setSelected(true);
            long[] dates = new long[selectedDates.size()];
            for (int i = 0; i < selectedDates.size(); i++) {
                dates[i] = selectedDates.get(i).getTime();
            }
            filterEventsWithDate(null, dates);
        }
    }

    public void removeAllSelectedDateFilters() {
        if (today.isSelected()) {
            today.setSelected(false);
        }
        if (tomorrow.isSelected()) {
            tomorrow.setSelected(false);
        }
        if (weekend.isSelected()) {
            weekend.setSelected(false);
        }

        if (filterEventTimes != null)
            filterEventTimes.clear();
    }

    public void checkIfCustomDateSelected() {
        if (selectCustomDates.isSelected()) {
            selectCustomDates.setSelected(false);
            filterEventTimes.clear();
        }
    }

    public void expandAnimation() {
        int targetHeight;
        targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (56 * 3), getResources().getDisplayMetrics());
        ResizeAnimation resizeAnimation = new ResizeAnimation(filtersContainer, targetHeight);
        resizeAnimation.setDuration(100);
        resizeAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                showMoreFilterText.setText("");
                if (!weekend.isShown())
                    weekend.setVisibility(View.VISIBLE);
                if (!selectCustomDates.isShown()) {
                    selectCustomDates.setVisibility(View.VISIBLE);
                    dateFilter.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                    dateFilter.postDelayed(new Runnable() {
                        public void run() {
                            dateFilter.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                        }
                    }, 100L);
                }
                addDrawable(R.drawable.ic_action_highlight_remove);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        filtersContainer.startAnimation(resizeAnimation);
    }

    public void collapseAnimation() {
        ResizeAnimation resizeAnimation = new ResizeAnimation(filtersContainer, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (56), getResources().getDisplayMetrics()));
        resizeAnimation.setDuration(100);
        resizeAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                showMoreFilterText.setText("• • •");
                addDrawable(-1);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        filtersContainer.startAnimation(resizeAnimation);
    }

    public void addDrawable(int resId) {
        Drawable drawableLeft = null;
        if (resId != -1) {
            drawableLeft = getResources().getDrawable(resId);
            drawableLeft.setBounds(0, 0, drawableLeft.getIntrinsicWidth(), drawableLeft.getIntrinsicHeight());

        }
        showMoreFilterText.setCompoundDrawables(drawableLeft, null, null, null);
    }
}
