package com.example.android.common.view;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public abstract class SlidingTabPagerAdapter extends FragmentStatePagerAdapter {
    public SlidingTabPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public abstract String getNumEvents(int position);
}
