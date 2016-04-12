package com.eventshigh.nearme.app.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventsContext;

import com.eventshigh.nearme.app.network.SocialInvitationsRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.CitySelectDialog;


import com.eventshigh.nearme.app.ui.adapter.CityListAdapter;
import com.eventshigh.nearme.app.ui.adapter.CityListAdapter.OnCitySelectionListener;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import pl.snowdog.material.ui.ToolbarColorizeHelper;

/**
 * Application Main or launch activity.
 */
public class LaunchActivity extends BaseContextActivity {
    // Constants
    public static final String DEFAULT_TAB_PARAM = LaunchActivity.class.getName() + "_default_tab";

    // UI Elements for this activity.
    private DrawerLayout drawer;
    private TabLayout tabsView;
    private ViewPager viewPager;
    private ListView citySelector;
    private ActionBarDrawerToggle drawerToggle;

    // GCM registration helper.
    private Account account;

    // Tabs.
    private int defaultTab = 1;
    public static final String MY_EVENTS_TAB = EventsHighEndpoints.QUERY_MY_EVENT;
    public static final String EXPLORE_TAB = "explore";
    public static final String THIS_WEEK_TAB = "this week";
    public final String[] TABS = {
            MY_EVENTS_TAB,
            EXPLORE_TAB,
            THIS_WEEK_TAB,
    };

    //Calculate no of time user resumes on to Home
    int screenViewCount;

    boolean isPagerSwipeBlocked;

    TextView currentCity;

    public final int MIN_SCREEN_VIEWS = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout(R.layout.activity_launch);

        // Set View.
        drawer = (DrawerLayout) findViewById(R.id.nav_drawer);
        tabsView = (TabLayout) findViewById(R.id.tabs);
        viewPager = (ViewPager) findViewById(R.id.view_pager);
        citySelector = (ListView) findViewById(R.id.city_selector);

        // Setup the Drawer Layout.
        drawerToggle = new ActionBarDrawerToggle(this, drawer, R.string.app_name, R.string.title_activity_settings);
        drawer.addDrawerListener(drawerToggle);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        if (isFinishing()) {
            return;
        }

        // Set defaults for preferences.
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        // Read account status.
        account = new Account(this);

        // Process the incoming intent.
        String tabName = getIntent().getStringExtra(DEFAULT_TAB_PARAM);
        if (tabName != null) {
            for (int i = 0; i < TABS.length; i++) {
                if (TABS[i].equalsIgnoreCase(tabName)) {
                    defaultTab = i;
                    break;
                }
            }
        }


    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        this.setIntent(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(toolbar != null) {
            setLightToolbarIcons();
        }

        //invalidateOptionsMenu();
        // We show the onboarding If this is first activity and there was no
        // location/query passed through intent.
        if (eventsContext.city == null && eventsContext.query.isEmpty() &&
            eventsContext.dateFilter.isEmpty()) {
            if (Preferences.getInstance(this).shouldShowOnBoarding()) {
                startActivity(new Intent(this, OnBoardingActivity.class));
                return;
            }

            String action = getIntent().getAction();
            if (!isTaskRoot() && (action == null || !action.startsWith(NOTIFICATION_ACTION))
                && (getIntent().getData() == null || !getIntent().getData().getHost().equalsIgnoreCase("branch.eventshigh.com"))) {
                finish();
                return;
            }
        }
        Branch branch = Branch.getInstance();
        if(branch!=null)
        branch.initSession(new Branch.BranchReferralInitListener() {
            @Override
            public void onInitFinished(JSONObject referringParams, BranchError error) {
                if (error == null) {
                    //showEventDetails();
                    if(referringParams.length() == 0){
                        showNextScreen();

                    }else {
                        try {
                            if(!referringParams.getBoolean("+is_first_session") && !referringParams.getBoolean("+clicked_branch_link")){
                                showNextScreen();
                            }else {
                                if(referringParams.has("event_id") ) {
                                    showEventDetails(
                                            EventsHighEndpoints.getEventDetailsURI(City.BANGALORE, referringParams.getString("event_id")), null);
                                }else if(referringParams.has("event_uri")){
                                    Uri uri = Uri.parse(referringParams.getString("event_uri"));
                                    showSearchView(uri.getLastPathSegment());
                                }
                                //showEventDetails((Event)( obj.get("event")), eventsContext.getLabel(), null);
                            }
                        } catch (JSONException e) {
                            showNextScreen();
                            e.printStackTrace();
                        }
                        System.out.println("JsonObject received" + referringParams);
                    }
                } else {
                    showNextScreen();
                    Log.i("MyApp", error.getMessage());
                }
            }
        }, this.getIntent().getData(), this);

        // Show next screen.
      //  showNextScreen();


        currentCity = (TextView)findViewById(R.id.current_city);
        if(account.getLastCity()!=null) {
            currentCity.setText(account.getLastCity().name());
            currentCity.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CitySelectDialog.show(LaunchActivity.this, account, new CitySelectDialog.CitySelectionCallback() {
                        @Override
                        public void onCityChanged(City city) {
                            currentCity.setText(city.name());
                            cityChanged(city);
                        }
                    });
                }
            });
        }


    //    ReferEarnDialog.showDialog(this);

    }

    private void setLightToolbarIcons() {
        toolbar.post(new Runnable() {
            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                ToolbarColorizeHelper.colorizeToolbar(toolbar,
                        getResources().getColor(android.R.color.white), LaunchActivity.this);
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_launch, menu);

        // Search View.
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {

            if(isPagerSwipeBlocked){
                isPagerSwipeBlocked=false;
                if(exploreFragment!=null){
                    exploreFragment.animateLocalityViewOut();
                }
            }else {
                super.onBackPressed();
            }
        }
    }

    public void cityChanged(City city) {
        drawer.closeDrawer(GravityCompat.START);
        reportActionToAnalytics("cityChanged");

        eventsContext.changeLocation(city);
        showExploreScreen();
    }


    // ***********************
    // Helper methods
    // ***********************
    private void setCity() {
        // Set the location from lastCity if needed.
        if (eventsContext.city == null) {
            City lastCity = account.getLastCity();
            if (lastCity != null) {
                reportActionToAnalytics("usedLastCity");
                eventsContext.changeLocation(lastCity);
            }
        }

        // If we have user location, start next activity.
        if (eventsContext.city != null) {
            showNextScreen();
            return;
        }

        // We do not have user location. Lets populate the City chooser and let user
        // select the city.
        reportActionToAnalytics("locationFailed");
        tabsView.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);
        citySelector.setVisibility(View.VISIBLE);
        citySelector.setAdapter(new CityListAdapter(LaunchActivity.this, mCitySelectionListener));
    }

    private void refreshIfOldData() {
        City userCity = account.getLastCity();
        if (eventsContext.city != null && userCity != null &&
                eventsContext.city != userCity) {
            cityChanged(userCity);
            return;
        }

        if (viewPager.getAdapter() == null) {
            showExploreScreen();
        }
    }

    private void showExploreScreen() {


        ExploreScreenPagerAdapter adapter = new ExploreScreenPagerAdapter();
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(defaultTab, false);
        viewPager.addOnPageChangeListener(listener);
        tabsView.setTabMode(TabLayout.MODE_FIXED);
        tabsView.setTabGravity(TabLayout.GRAVITY_FILL);
        tabsView.setupWithViewPager(viewPager);
        tabsView.setScrollPosition(defaultTab, 0, true);


        setupTabIcons();
        tabsView.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        TabLayout.Tab tab = tabsView.getTabAt(defaultTab);
        if (tab != null) {
            tab.select();
        }

    }

    private void setupTabIcons() {

        TextView tabOne = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabOne.setText("My Events");
        tabOne.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_events, 0, 0);
        tabOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(0);
            }
        });
        tabsView.getTabAt(0).setCustomView(tabOne);

        TextView tabTwo = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabTwo.setText("Explore");
        tabTwo.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_explore, 0, 0);
        tabTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(1);
            }
        });
        tabsView.getTabAt(1).setCustomView(tabTwo);

        TextView tabThree = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabThree.setText("This Week");
        tabThree.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_week, 0, 0);
        tabThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(2);
            }
        });
        tabsView.getTabAt(2).setCustomView(tabThree);

    }
    private void showNextScreen() {
        // If we do not have user city, use GoogleLocation api to get user location.
        if (eventsContext.city == null) {
            setCity();
            return;
        }

        // If we do not have query, show explore screen.
        if (eventsContext.query.isEmpty() && eventsContext.dateFilter.isEmpty()) {
            refreshIfOldData();
        } else {
            String action = Intent.ACTION_VIEW;
            SocialInvitationsRequest.SpecialCoupons special = null;
            Intent inIntent = getIntent();
            if (inIntent != null) {
                String inAction = inIntent.getAction();
                if (inAction != null && inAction.startsWith(NOTIFICATION_ACTION)) {
                    action = inAction;
                }
                special = inIntent.getParcelableExtra("special_obj");
            }

            Intent outIntent = new Intent(this, EventsGridActivity.class);
            outIntent.setAction(action);
            if(special!=null)
            outIntent.putExtra("special_obj",special);
            outIntent.putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, eventsContext);
            startActivity(outIntent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Checking if the previous activity is launched on branch Auto deep link.
        if(requestCode == getResources().getInteger(R.integer.EventsDetailDeepLink_code)){
            //Decide here where  to navigate  when an auto deep linked activity finishes.
            //For e.g. Go to HomeActivity or a  SignUp Activity.
            showNextScreen();
        }
    }
    private final OnCitySelectionListener mCitySelectionListener = new OnCitySelectionListener() {
        @Override
        public void onCitySelection(City city) {
            eventsContext.changeLocation(city);
            account.setLastCity(city);

            citySelector.setVisibility(View.GONE);
            tabsView.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);
            showNextScreen();
        }
    };


    ViewPager.OnPageChangeListener listener= new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            reportActionToAnalytics("tabchange",TABS[position]);

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };



    public void setisPagerSwipeBlocked(boolean isPagerSwipeBlocked){
        this.isPagerSwipeBlocked = isPagerSwipeBlocked;
    }


    ExploreFragment exploreFragment;
    private EventsFragment myEventsFragment;
    /**
     * An SlidingTabPagerAdapter which populates tabs and content for LaunchActivity.
     */
    private class ExploreScreenPagerAdapter extends FragmentPagerAdapter
            implements TabLayout.OnTabSelectedListener,ViewPager.OnPageChangeListener {


        public ExploreScreenPagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public long getItemId(int position) {
            return (eventsContext.toString() + TABS[position]).hashCode();
        }

        @Override
        public Fragment getItem(int position) {
            if (TABS[position].equals(MY_EVENTS_TAB)) {
                EventsContext myEventsContext = new EventsContext(eventsContext.city,
                    EventsHighEndpoints.QUERY_MY_EVENT);
                myEventsFragment = EventsFragment.getInstance(myEventsContext, false, true, null);

                return myEventsFragment;
            }

            if (TABS[position].equals(EXPLORE_TAB)) {
                 exploreFragment = ExploreFragment.getInstance(eventsContext);
                 return exploreFragment;

            }

            return ThisWeekFragment.getInstance(eventsContext, true, 7);
        }

        @Override
        public int getCount() {
            return TABS.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TABS[position];
        }

        @Override
        public void onTabSelected(TabLayout.Tab tab) {

                showActionBar();

                int position = tab.getPosition();
                if (TABS[position].equals(MY_EVENTS_TAB) && myEventsFragment != null) {
                    myEventsFragment.onResume();
                }

                viewPager.setCurrentItem(position);

        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            // do nothing.
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            // do nothing.
        }


        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }
}
