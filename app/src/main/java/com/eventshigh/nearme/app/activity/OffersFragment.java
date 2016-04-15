package com.eventshigh.nearme.app.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;

import java.util.Calendar;
import java.util.Locale;

/**
 * Created by umesh on 15/04/16.
 */
public class OffersFragment extends Fragment{

    public BaseContextActivity activity;

    private ViewPager viewPager;

    private TabLayout tabsView;

    private OffersPagerAdapter adapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseContextActivity) activity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_this_week,container,false);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter=new OffersPagerAdapter(getChildFragmentManager());

        viewPager = (ViewPager) view.findViewById(R.id.view_pager_date);
        viewPager.setAdapter(adapter);

        tabsView = (TabLayout) view.findViewById(R.id.date_filter);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabsView));
        tabsView.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }
    public class OffersPagerAdapter extends FragmentStatePagerAdapter{

        String []TABS = {"Offers","Points"};

        Fragment fragment;

        public OffersPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if(position == 0){

                return MyOffersListFragment.newInstance(null);
            }else{

                return MyPointsListFragment.newInstance(null);
            }

        }

        @Override
        public int getCount() {
            return TABS.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TABS[position];
        }
    }
}
