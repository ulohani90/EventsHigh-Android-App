package com.eventshigh.nearme.app.activity;

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
import android.widget.LinearLayout;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

/**
 * Created by umesh on 21/04/16.
 */
public class MeFragment extends Fragment {

    Account account;
    public BaseContextActivity activity;

    private ViewPager viewPager;

    private TabLayout tabsView;

    MePagerAdapter adapter;

    public static String TAB_PARAM = "tab_param";

    public static final String MY_FAV_EVENTS="my favourite events";

    public static final String MY_INTEREST_EVENTS = "my interest events";

    public static final String MY_FRIENDS = "my friends";

    public static MeFragment getInstance(EventsContext eventsContext,String tabParam){
        MeFragment fragment = new MeFragment();
        Bundle args = new Bundle();
        args.putParcelable(BaseEventsFragment.EVENT_CONTEXT_PARAM, eventsContext);
        args.putString(TAB_PARAM,tabParam);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        eventsContext = getArguments().getParcelable(BaseEventsFragment.EVENT_CONTEXT_PARAM);
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

        adapter = new MePagerAdapter(getChildFragmentManager());

        viewPager = (ViewPager) view.findViewById(R.id.view_pager_date);
        viewPager.setAdapter(adapter);

        tabsView = (TabLayout) view.findViewById(R.id.date_filter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                ((BaseActivity) getActivity()).reportActionToAnalytics("metabchange", (position == 0 ? "MyFavourites" : ((position == 1)?"MyInterestEvents":"MyFriends")));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        tabsView.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));
        tabsView.setupWithViewPager(viewPager);

        account = new Account(getActivity());

        String tabParam = getArguments().getString(TAB_PARAM,"");
        if(tabParam.equalsIgnoreCase(MY_INTEREST_EVENTS)){
            viewPager.setCurrentItem(1);
        }else{
            viewPager.setCurrentItem(0);
        }

    }


    EventsContext eventsContext;

    EventsFragment myFavouritesFragment;

    EventsFragment myInterestEventsFragment;

    public class MePagerAdapter extends FragmentStatePagerAdapter {

        String []TABS = {"My Favourites","My Events"};



        Fragment fragment;

        public MePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if(position == 0){
                EventsContext myEventsContext = new EventsContext(eventsContext.city,
                        EventsHighEndpoints.QUERY_MY_EVENT);
                myFavouritesFragment = EventsFragment.getInstance(myEventsContext, false, false,  null);
                return myFavouritesFragment;
            }else {
                EventsContext myEventsContext = new EventsContext(eventsContext.city,
                        EventsHighEndpoints.QUERY_MY_INTEREST_EVENTS);
                myInterestEventsFragment = EventsFragment.getInstance(myEventsContext, false, true,  null);
                return myInterestEventsFragment;
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
