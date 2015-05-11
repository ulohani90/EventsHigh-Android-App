package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.example.android.common.view.SlidingTabLayout;
import com.example.android.common.view.SlidingTabLayout.TabColorizer;
import com.example.android.common.view.TabViewAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Fragment to show this weeks events by date.
 */
public class ThisWeekFragment extends Fragment {
    private final int NUM_DAYS = 14;
    private static final String EVENT_CONTEXT_PARAM = EventsFragment.class.getName() + "_event_context";

    public static ThisWeekFragment getInstance(EventsContext eventsContext, boolean showOffer) {
        ThisWeekFragment fragment = new ThisWeekFragment();

        Bundle args = new Bundle();
        args.putParcelable(EVENT_CONTEXT_PARAM, eventsContext);
        args.putBoolean(EventsFragment.SHOW_OFFER_PARAM, showOffer);
        fragment.setArguments(args);

        return fragment;
    }

    private BaseContextActivity activity;
    private EventsContext eventsContext;
    private boolean showOffer;

    private ViewPager viewPager;
    private ThisWeekPagerAdapter adapter;
    private SlidingTabLayout tabsView;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseContextActivity) activity;

        eventsContext = getArguments().getParcelable(EVENT_CONTEXT_PARAM);
        showOffer = getArguments().getBoolean(EventsFragment.SHOW_OFFER_PARAM);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_this_week, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        adapter = new ThisWeekPagerAdapter();

        viewPager = (ViewPager) view.findViewById(R.id.view_pager_date);
        viewPager.setAdapter(adapter);

        tabsView = (SlidingTabLayout) view.findViewById(R.id.date_filter);
        tabsView.setTabViewAdapter(adapter);
        tabsView.setViewPager(viewPager);
        tabsView.setOnPageChangeListener(adapter);
        tabsView.setCustomTabColorizer(adapter);
        tabsView.scrollTo(eventsContext.dateFilter);
    }

    public void refresh() {
        viewPager.setAdapter(adapter);
        tabsView.scrollTo(0);
    }

    private static class DateTabView {
        private final View root;
        public final TextView weekDayView;
        public final TextView dayOfMonthView;

        public DateTabView(View root) {
            this.root  = root;
            weekDayView = (TextView) root.findViewById(R.id.weekday);
            dayOfMonthView = (TextView) root.findViewById(R.id.dayofmonth);
        }
    }

    private class ThisWeekPagerAdapter extends FragmentStatePagerAdapter
            implements TabViewAdapter, OnPageChangeListener, TabColorizer {
        private final List<DateTabView> dateTabViews = new ArrayList<>(NUM_DAYS);

        private int lastPosition = -1;

        public ThisWeekPagerAdapter() {
            super(activity.getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            EventsContext dateContext = new EventsContext(eventsContext.location, "");
            dateContext.setDateFilter(getDate(position));
            return EventsFragment.getInstance(dateContext, false, showOffer);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return EventsContext.formatDateFilter(getDate(position));
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // do nothing.
        }

        @Override
        public void onPageSelected(int position) {
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null && !actionBar.isShowing()) {
                actionBar.show();
            }

            if (lastPosition >= 0) {
                TextView last = dateTabViews.get(lastPosition).dayOfMonthView;
                last.setTypeface(null, Typeface.NORMAL);
            }

            TextView selected = dateTabViews.get(position).dayOfMonthView;
            selected.setTypeface(null, Typeface.BOLD);
            lastPosition = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // do nothing.
        }

        @Override
        public View getView(int position, ViewGroup parent) {
            Calendar calendar = getDate(position);
            DateTabView dateTabView = new DateTabView(
                    activity.getLayoutInflater().inflate(R.layout.view_tab_date, parent, false));
            dateTabView.weekDayView.setText(
                    calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US));
            dateTabView.dayOfMonthView.setText(
                    Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)));

            dateTabViews.add(position, dateTabView);
            return dateTabView.root;
        }

        @Override
        public int getCount() {
            return NUM_DAYS;
        }

        @Override
        public int getIndicatorColor(int position) {
            return getResources().getColor(android.R.color.white);
        }

        @Override
        public int getDividerColor(int position) {
            return 0x26000000;
        }

        private final Calendar today = DateTimeUtils.toMidnight(Calendar.getInstance(), null);
        private Calendar getDate(int position) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(today.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, position);
            return calendar;
        }
    }
}
