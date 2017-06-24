package com.eventshigh.nearme.app.ui.adapter;

import android.app.Activity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;

public class EventPagerCard extends ViewHolder {
    private final ViewPager viewPager;
    private final LinearLayout dotsView;

    public static EventPagerCard newInstance(Activity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_event_pager, parent, false);
        return new EventPagerCard(view);
    }

    public EventPagerCard(View cardView) {
        super(cardView);
        viewPager = (ViewPager) cardView.findViewById(R.id.events_pager);
        dotsView = (LinearLayout) cardView.findViewById(R.id.dots_parent);
    }

    public void bindHeaderView(EventPagerData eventPagerData) {
        FeaturedEventsAdapter adapter = new FeaturedEventsAdapter(eventPagerData);
        viewPager.setAdapter(adapter);

        dotsView.removeAllViews();
        if (adapter.getCount() > 1) {
            dotsView.setVisibility(View.VISIBLE);
            for (int i = 0; i < adapter.getCount(); i++) {
                View view = eventPagerData.activity.getLayoutInflater().inflate(
                        R.layout.view_dot_featured, dotsView, false);
                view.setSelected(i == 0);
                dotsView.addView(view);
            }
        } else {
            dotsView.setVisibility(View.GONE);
        }
        viewPager.clearOnPageChangeListeners();
        viewPager.addOnPageChangeListener(new DotsSelector(eventPagerData.activity));
    }

    private class DotsSelector implements OnPageChangeListener {
        private final BaseContextActivity activity;

        private DotsSelector(BaseContextActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // do nothing.
        }

        @Override
        public void onPageSelected(int position) {
            if (position != 0) {
                activity.reportActionToAnalytics("featuredSwipe");
            }
            for (int i = 0; i < dotsView.getChildCount(); i++) {
                dotsView.getChildAt(i).setSelected(i == position);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // do nothing.
        }
    }
}
