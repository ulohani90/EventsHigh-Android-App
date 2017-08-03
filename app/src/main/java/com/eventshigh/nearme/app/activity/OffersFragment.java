package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.LinearLayout;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.user.Account;

/**
 * Created by umesh on 15/04/16.
 */
public class OffersFragment extends Fragment {

    public BaseContextActivity activity;

    private ViewPager viewPager;

    private TabLayout tabsView;

    private OffersPagerAdapter adapter;

    private LinearLayout verifyPhnLayout;


    Account account;


    public OffersFragment newInstance() {
        OffersFragment fragment = new OffersFragment();
        return fragment;

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseContextActivity) activity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_this_week, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new OffersPagerAdapter(getChildFragmentManager());

        viewPager = (ViewPager) view.findViewById(R.id.view_pager_date);
        viewPager.setAdapter(adapter);

        tabsView = (TabLayout) view.findViewById(R.id.date_filter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                ((BaseActivity) getActivity()).reportActionToAnalytics("offertabchange", (position == 0 ? "OfferList" : "PointsList"));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        tabsView.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));
        tabsView.setupWithViewPager(viewPager);

        verifyPhnLayout = (LinearLayout) view.findViewById(R.id.verify_phn_layout);
        account = new Account(getActivity());

        (view.findViewById(R.id.verify_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyClicked();
            }
        });

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (account != null && !(account.getUserInfo().isVerified)) {
            verifyPhnLayout.setClickable(true);
            verifyPhnLayout.setVisibility(View.VISIBLE);
        } else {
            verifyPhnLayout.setVisibility(View.GONE);
        }
    }

    public void verifyClicked() {
        activity.startActivity(new Intent(activity, PhoneLoginActivity.class));
    }

    public class OffersPagerAdapter extends FragmentStatePagerAdapter {

        String[] TABS = {"Offers", "How to Earn"};

        Fragment fragment;

        public OffersPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {

                return MyOffersListFragment.newInstance(null);
            } else {

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
