package com.eventshigh.nearme.app.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.utils.DateTimeUtils;

import java.util.Calendar;
import java.util.Locale;

/**
 * Fragment to show this weeks events by date.
 */
public class ThisWeekFragment extends BaseEventsFragment {
    public static final String NUM_DAYS_PARAM = ThisWeekFragment.class.getName() + "_num_days";

    public static ThisWeekFragment getInstance(EventsContext eventsContext,  boolean showCategories,
            int numDays) {
        ThisWeekFragment fragment = new ThisWeekFragment();

        Bundle args = getArgs(eventsContext, false, showCategories);
        args.putInt(NUM_DAYS_PARAM, numDays);
        fragment.setArguments(args);
        return fragment;
    }

    private ViewPager viewPager;
    private ThisWeekPagerAdapter adapter;
    private TabLayout tabsView;

    boolean isDestroyed;

    private final Calendar today = DateTimeUtils.toMidnight(Calendar.getInstance(), null);
    private int numDays;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        numDays = getArguments().getInt(NUM_DAYS_PARAM);
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

        tabsView = (TabLayout) view.findViewById(R.id.date_filter);
        int selectedPosition = populateTabs();
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabsView));
        tabsView.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));
        select(selectedPosition);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        viewPager.post(new Runnable() {
            @Override
            public void run() {
                if (!isDestroyed) {

                    viewPager.setAdapter(adapter);
                    tabsView.removeAllTabs();
                    select(populateTabs());
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        isDestroyed = true;
        super.onDestroy();
    }

    @SuppressLint("SetTextI18n")
    private int populateTabs() {
        int selectedPosition = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getPageTitle(i).equals(eventsContext.dateFilter)) {
                selectedPosition = i;
            }

            Calendar calendar = getDate(i);
            @SuppressLint("InflateParams")
            DateTabView dateTabView = new DateTabView(
                    activity.getLayoutInflater().inflate(R.layout.view_tab_date, null));
            dateTabView.weekDayView.setText(
                    calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US));
            dateTabView.dayOfMonthView.setText(
                    Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)));
            tabsView.addTab(tabsView.newTab().setCustomView(dateTabView.root));
        }
        return selectedPosition;
    }

    private void select(int position) {
        Tab tab = tabsView.getTabAt(position);
        if (tab != null) {
            tab.select();
        }
    }

    private Calendar getDate(int position) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today.getTime());
        calendar.add(Calendar.DAY_OF_MONTH, position);
        return calendar;
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

    private class ThisWeekPagerAdapter extends FragmentStatePagerAdapter {
        public ThisWeekPagerAdapter() {
            super(activity.getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            EventsContext dateContext = new EventsContext(eventsContext.city, "");
            dateContext.setDateFilter(getDate(position));

            return EventsFragment.getInstance(dateContext, false, showCategories, null);

        }

        @Override
        public CharSequence getPageTitle(int position) {
            return EventsContext.formatDateFilter(getDate(position));
        }

        @Override
        public int getCount() {
            return numDays;
        }
    }
}
