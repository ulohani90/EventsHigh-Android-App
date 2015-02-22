package com.eventshigh.nearme.app.ui;

import android.app.Activity;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;

/**
* A {@link android.support.v4.view.PagerAdapter} which shows only one page which asks user to retry.
*/
public class FailedRetryAdapter extends PagerAdapter {
    private final Activity activity;

    public FailedRetryAdapter(Activity activity) {
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = activity.getLayoutInflater().inflate(R.layout.view_retry, container, false);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
}
