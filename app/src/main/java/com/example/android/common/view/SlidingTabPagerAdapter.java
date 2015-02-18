package com.example.android.common.view;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.View;
import android.view.ViewGroup;

public abstract class SlidingTabPagerAdapter extends FragmentStatePagerAdapter {
    public SlidingTabPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public abstract View getView(int position, ViewGroup parent);
}
