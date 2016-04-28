package com.eventshigh.nearme.app.activity;

import android.Manifest;
import android.Manifest.permission;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.Tab;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.res.TypedArrayUtils;
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

import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.CitySelectDialog;
import com.eventshigh.nearme.app.ui.ReferEarnDialog;
import com.eventshigh.nearme.app.ui.adapter.CityListAdapter;
import com.eventshigh.nearme.app.ui.adapter.CityListAdapter.OnCitySelectionListener;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.plus.PlusOneButton;
import com.google.android.gms.plus.PlusOneButton.OnPlusOneClickListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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

    // Client to Google api so that we can fetch the user location if
    // its not passed in intent.
    private GoogleApiClient client;

    // GCM registration helper.
    private Account account;

    // Tabs.
    private int defaultTab = 1;
    public static final String MY_EVENTS_TAB = EventsHighEndpoints.QUERY_MY_EVENT;
    public static final String EXPLORE_TAB = "explore";
    public static final String NOTIFICATIONS_TAB = "Notifications";
    public static final String THIS_WEEK_TAB = "this week";
    public static final String OFFERS_TAB = "Offers";
    public ArrayList<String> TABS = new ArrayList<>();

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
        /*String tabName = getIntent().getStringExtra(DEFAULT_TAB_PARAM);
        if (tabName != null) {
            for (int i = 0; i < TABS.size(); i++) {
                if (TABS.get(i).equalsIgnoreCase(tabName)) {
                    defaultTab = i;
                    break;
                }
            }
        }
*/
        getIntent().getAction();

    }

    @Override
    public View getViewForSnackbar() {
        return null;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        this.setIntent(intent);
        if (intent != null && intent.getAction() != null && intent.getAction().equalsIgnoreCase(ReferralActivity.REDEEM_ACTION)) {

            drawer.closeDrawers();
            showExploreScreen();
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (toolbar != null) {
            setLightToolbarIcons();
        }
        if (getIntent() != null && getIntent().getAction() != null && getIntent().getAction().equalsIgnoreCase(ReferralActivity.REDEEM_ACTION)) {
            getIntent().setAction(null);
        }

        //invalidateOptionsMenu();
        // We show the onboarding If this is first activity and there was no
        // location/query passed through intent.
        if (eventsContext.location == null && eventsContext.query.isEmpty() &&
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


        //Show next screen.
        showNextScreen();

        // Setup the Google+ Button.
        PlusOneButton plusOneButton = (PlusOneButton) findViewById(R.id.plus_one_button);
        plusOneButton.initialize("https://play.google.com/store/apps/details?id=" + getPackageName(),
                PLUS_ONE_REQUEST_CODE);
        plusOneButton.setOnPlusOneClickListener(new OnPlusOneClickListener() {
            @Override
            public void onPlusOneClick(Intent intent) {
                if (intent != null) {
                    reportActionToAnalytics("plusOne");
                    startActivityForResult(intent, PLUS_ONE_REQUEST_CODE);
                }
            }
        });

        currentCity = (TextView) findViewById(R.id.current_city);
        if (account.getLastCity() != null) {
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

        // new InitiateBranchAsyncTask(getIntent().getData()).execute();
        loadBranchInstance();
        // ReferEarnDialog.showDialog(this);

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

        // Set visibility.
        menu.findItem(R.id.action_show_map).setVisible(isPlayServicesPresent);

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

            if (isPagerSwipeBlocked) {
                isPagerSwipeBlocked = false;
                if (exploreFragment != null) {
                    exploreFragment.animateLocalityViewOut();
                }
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission granted.
                if (eventsContext.city == null && client != null) {
                    client.connect();
                }
            }
            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void cityChanged(City city) {
        drawer.closeDrawer(GravityCompat.START);
        reportActionToAnalytics("cityChanged");
        eventsContext.changeLocation(city.cityBounds.getCenter());
        showExploreScreen();
    }

    public static String[] removeElements(String[] input, String deleteMe) {
        List result = new LinkedList();

        for (String item : input)
            if (!deleteMe.equals(item))
                result.add(item);

        return (String[]) result.toArray(input);
    }

    // ***********************
    // Callbacks
    // ***********************

    // Callback for GoogleClientApi. This is called when googleClientApi is ready to accept
    // requests. We set the user location if needed and start next activity.
    private ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            if (eventsContext.city == null &&
                    ActivityCompat.checkSelfPermission(LaunchActivity.this, permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(LaunchActivity.this, permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location location = LocationServices.FusedLocationApi.getLastLocation(client);
                if (location != null) {
                    LatLng latLng = LocationUtils.locationToLatLng(location);
                    eventsContext.changeLocation(latLng);
                    if (eventsContext.city != null) {
                        account.setLastCity(eventsContext.city);
                    } else {
                        reportActionToAnalytics("unsupportedCity");
                    }
                }
            }

            // Start the next activity if possible or ask user for city.
            client.disconnect();
            //noinspection ConstantConditions
            mOnConnectionFailedListener.onConnectionFailed(null);
        }

        @Override
        public void onConnectionSuspended(int i) {
            // do nothing.
        }
    };

    private OnConnectionFailedListener mOnConnectionFailedListener = new OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@Nullable ConnectionResult connectionResult) {
            // Set the location from lastCity if needed.
            if (eventsContext.city == null) {
                City lastCity = account.getLastCity();
                if (lastCity != null) {
                    reportActionToAnalytics("usedLastCity");
                    eventsContext.changeLocation(lastCity.cityBounds.getCenter());
                }
            }

            // If we have user location, start next activity.
            if (eventsContext.city != null) {
                if (eventsContext.location != null) {
                    try {
                        int appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                        Uri reportUri = UpdateAccountInfoService.getBaseUri(LaunchActivity.this, "reportUser")
                                .appendQueryParameter("city", eventsContext.city.toString())
                                .appendQueryParameter("lat", Double.toString(eventsContext.location.latitude))
                                .appendQueryParameter("lon", Double.toString(eventsContext.location.longitude))
                                .appendQueryParameter("version", Integer.toString(appVersion))
                                .build();
                        RequestFuture<String> future = RequestFuture.newFuture();
                        VolleyHelper.addToRequestQueue(LaunchActivity.this,
                                new StringRequest(reportUri.toString(), future, future) {
                                    @Override
                                    public Priority getPriority() {
                                        return Priority.LOW;
                                    }
                                }
                        );
                    } catch (NameNotFoundException e) {
                        Crashlytics.getInstance().core.logException(e);
                    }
                }

                citySelector.setVisibility(View.GONE);
                tabsView.setVisibility(View.VISIBLE);
                viewPager.setVisibility(View.VISIBLE);
                if (currentCity != null)
                    currentCity.setText(eventsContext.city.name());
                showNextScreen();
                return;
            }

            // We do not have user location. Lets populate the City chooser and let user
            // select the city.
            if (citySelector.getVisibility() != View.VISIBLE) {
                reportActionToAnalytics("locationFailed");
                tabsView.setVisibility(View.GONE);
                viewPager.setVisibility(View.GONE);
                citySelector.setVisibility(View.VISIBLE);
                citySelector.setAdapter(new CityListAdapter(LaunchActivity.this, mCitySelectionListener));
                if (connectionResult != null) {
                    showMessage(R.string.failed_location);
                }
                if (ActivityCompat.checkSelfPermission(LaunchActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // Request missing location permission.
                    ActivityCompat.requestPermissions(LaunchActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                            PERMISSIONS_REQUEST_LOCATION);
                }
            }
        }
    };


    // ***********************
    // Helper methods
    // ***********************


    @Override
    protected void onPause() {
        super.onPause();
        if (drawer != null)
            drawer.closeDrawers();
    }

    private void refreshIfOldData() {
        City userCity = account.getLastCity();
        if (eventsContext.city != null && userCity != null &&
                eventsContext.city != userCity) {
            cityChanged(userCity);
            return;
        }

        if (viewPager.getAdapter() == null || (getIntent() != null && getIntent().getAction() != null && getIntent().getAction().equalsIgnoreCase(ReferralActivity.REDEEM_ACTION))) {
            showExploreScreen();
        }
    }

    private void showExploreScreen() {
/*
        if (!(account.getLastCity() == City.BANGALORE)) {
            TABS = new ArrayList<>();
            TABS.add(MY_EVENTS_TAB);
            TABS.add(EXPLORE_TAB);
            TABS.add(THIS_WEEK_TAB);
            TABS.add(NOTIFICATIONS_TAB);
        } else {
            TABS = new ArrayList<>();
            TABS.add(MY_EVENTS_TAB);
            TABS.add(EXPLORE_TAB);
            TABS.add(OFFERS_TAB);
            TABS.add(THIS_WEEK_TAB);
            TABS.add(NOTIFICATIONS_TAB);
        }*/

        TABS = new ArrayList<>();
        TABS.add(MY_EVENTS_TAB);
        TABS.add(EXPLORE_TAB);
        TABS.add(OFFERS_TAB);
        TABS.add(THIS_WEEK_TAB);
        TABS.add(NOTIFICATIONS_TAB);

        String tabName = getIntent().getStringExtra(DEFAULT_TAB_PARAM);
        if (tabName != null) {
            for (int i = 0; i < TABS.size(); i++) {
                if (TABS.get(i).equalsIgnoreCase(tabName)) {
                    defaultTab = i;
                    break;
                }
            }
        }

        ExploreScreenPagerAdapter adapter = new ExploreScreenPagerAdapter();
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(defaultTab, false);
        viewPager.addOnPageChangeListener(listener);
        tabsView.setTabMode(TabLayout.MODE_FIXED);
        tabsView.setTabGravity(TabLayout.GRAVITY_FILL);
        tabsView.setupWithViewPager(viewPager);
        tabsView.setScrollPosition(defaultTab, 0, true);
        setupTabIconsWithOffer();
       /* if (account.getLastCity() == City.BANGALORE) {
            setupTabIconsWithOffer();
        } else {
            setupTabIcons();
        }*/
        tabsView.invalidate();

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
        Tab tab = tabsView.getTabAt(defaultTab);
        if (tab != null) {
            tab.select();
        }
    }

    private void setupTabIconsWithOffer() {

        TextView tabOne = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabOne.setText("Me");
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
        tabThree.setText("Offers");
        tabThree.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_offer, 0, 0);
        tabThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(2);
            }
        });
        tabsView.getTabAt(2).setCustomView(tabThree);

        TextView tabFour = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabFour.setText("Week");
        tabFour.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_week, 0, 0);
        tabFour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(3);
            }
        });
        tabsView.getTabAt(3).setCustomView(tabFour);

        TextView tabFive = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabFive.setText("Alerts");
        tabFive.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_notification, 0, 0);
        tabFive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(4);
            }
        });
        tabsView.getTabAt(4).setCustomView(tabFive);
    }

    private void setupTabIcons() {

        TextView tabOne = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabOne.setText("Me");
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
        tabThree.setText("Week");
        tabThree.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_week, 0, 0);
        tabThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(2);
            }
        });
        tabsView.getTabAt(2).setCustomView(tabThree);

        TextView tabFour = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabFour.setText("Alerts");
        tabFour.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_notification, 0, 0);
        tabFour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(3);
            }
        });
        tabsView.getTabAt(3).setCustomView(tabFour);
    }

    private void showNextScreen() {
        // If we do not have user city, use GoogleLocation api to get user location.
        if (eventsContext.city == null) {
            client = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mOnConnectionFailedListener)
                    .build();
            if (ActivityCompat.checkSelfPermission(LaunchActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request missing location permission.
                //noinspection ConstantConditions
                mOnConnectionFailedListener.onConnectionFailed(null);
            } else {
                client.connect();
            }
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
            if (special != null)
                outIntent.putExtra("special_obj", special);
            outIntent.putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, eventsContext);
            startActivity(outIntent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Checking if the previous activity is launched on branch Auto deep link.
        if (requestCode == getResources().getInteger(R.integer.EventsDetailDeepLink_code)) {
            //Decide here where  to navigate  when an auto deep linked activity finishes.
            //For e.g. Go to HomeActivity or a  SignUp Activity.
            showNextScreen();
        }
    }

    private final OnCitySelectionListener mCitySelectionListener = new OnCitySelectionListener() {
        @Override
        public void onCitySelection(City city) {
            eventsContext.changeLocation(city.cityBounds.getCenter());
            account.setLastCity(city);
            if (currentCity != null) {
            }
            currentCity.setText(city.name());
            citySelector.setVisibility(View.GONE);
            tabsView.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);
            showNextScreen();
        }
    };


    ViewPager.OnPageChangeListener listener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            reportActionToAnalytics("tabchange", TABS.get(position));
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };


    public void setisPagerSwipeBlocked(boolean isPagerSwipeBlocked) {
        this.isPagerSwipeBlocked = isPagerSwipeBlocked;
    }


    ExploreFragment exploreFragment;
    private EventsFragment myEventsFragment;

    /**
     * An SlidingTabPagerAdapter which populates tabs and content for LaunchActivity.
     */
    private class ExploreScreenPagerAdapter extends FragmentPagerAdapter
            implements TabLayout.OnTabSelectedListener, ViewPager.OnPageChangeListener {


        public ExploreScreenPagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public long getItemId(int position) {
            return (eventsContext.toString() + TABS.get(position)).hashCode();
        }

        @Override
        public Fragment getItem(int position) {
            if (TABS.get(position).equals(MY_EVENTS_TAB)) {
                /*EventsContext myEventsContext = new EventsContext(eventsContext.location,
                        EventsHighEndpoints.QUERY_MY_EVENT);
                myEventsFragment = EventsFragment.getInstance(myEventsContext, false, true, false, null);*/
                String tabParam = "";
                if (getIntent().hasExtra(MeFragment.TAB_PARAM)) {
                    tabParam = getIntent().getStringExtra(MeFragment.TAB_PARAM);
                }
                EventsContext myEventsContext = new EventsContext(eventsContext.location,
                        EventsHighEndpoints.QUERY_MY_EVENT);
                Log.i("ME_TAB_PARAMS", tabParam);
                MeFragment fragment = MeFragment.getInstance(myEventsContext, tabParam);
                return fragment;
            }

            if (TABS.get(position).equals(EXPLORE_TAB)) {
                exploreFragment = ExploreFragment.getInstance(eventsContext);
                return exploreFragment;

            }
            if (TABS.get(position).equals(OFFERS_TAB)) {
                return new OffersFragment();
            }

            if (TABS.get(position).equals(NOTIFICATIONS_TAB)) {
                return new StreamFragment();
            }

            return ThisWeekFragment.getInstance(eventsContext, true, 7);
        }

        @Override
        public int getCount() {
            return TABS.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TABS.get(position);
        }

        @Override
        public void onTabSelected(TabLayout.Tab tab) {

            showActionBar();

            int position = tab.getPosition();
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

   /* public class InitiateBranchAsyncTask extends AsyncTask<Void, Void, JSONObject> {
        Uri data;


        public InitiateBranchAsyncTask(@NonNull Uri data) {
            this.data = data;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            Branch branch = Branch.getInstance();
            if (branch != null)
                branch.initSession(new Branch.BranchReferralInitListener() {
                    @Override
                    public void onInitFinished(JSONObject referringParams, BranchError error) {
                        if (error == null) {
                            //showEventDetails();
                            if (referringParams.length() == 0) {
                                // showNextScreen();
                                Log.i("Event_detail_missed", "refering params empty");
                            } else {
                                try {
                                    if (!referringParams.getBoolean("+is_first_session") && !referringParams.getBoolean("+clicked_branch_link")) {
                                        //showNextScreen();
                                        Log.i("Event_detail_missed", referringParams.getBoolean("+is_first_session") ? "False" : "true");
                                    } else {
                                        if (referringParams.has("event_id")) {
                                            showEventDetails(
                                                    EventsHighEndpoints.getEventDetailsURI(City.BANGALORE, referringParams.getString("event_id")), null);
                                        } else if (referringParams.has("event_uri")) {
                                            Uri uri = Uri.parse(referringParams.getString("event_uri"));
                                            showSearchView(uri.getLastPathSegment());
                                        } else if (referringParams.has("offer_id")) {
                                            if (account.getLastCity() == City.BANGALORE && viewPager != null) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        viewPager.setCurrentItem(2);
                                                    }
                                                });

                                            }
                                        }

                                        if (referringParams.has("referrer2")) {

                                            String referrer = referringParams.getString("referrer2");
                                            Log.i("referrer2",referrer);
                                            new Account(LaunchActivity.this).recordReferrer(referrer,true);

                                        }
                                        //showEventDetails((Event)( obj.get("event")), eventsContext.getLabel(), null);
                                    }
                                } catch (JSONException e) {
                                    // showNextScreen();
                                    e.printStackTrace();
                                }
                                System.out.println("JsonObject received" + referringParams);
                            }
                        } else {
                            //  showNextScreen();
                            Log.i("MyApp", error.getMessage());
                        }
                    }
                }, data, LaunchActivity.this);
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {

        }
    }*/

    public void loadBranchInstance() {
        Branch branch = Branch.getInstance();
        if (branch != null)
            branch.initSession(new Branch.BranchReferralInitListener() {
                @Override
                public void onInitFinished(JSONObject referringParams, BranchError error) {
                    if (error == null) {
                        //showEventDetails();
                        if (referringParams.length() == 0) {
                            // showNextScreen();
                            Log.i("Event_detail_missed", "refering params empty");
                        } else {
                            try {
                                if (!referringParams.getBoolean("+is_first_session") && !referringParams.getBoolean("+clicked_branch_link")) {
                                    //showNextScreen();
                                    Log.i("Event_detail_missed", referringParams.getBoolean("+is_first_session") ? "False" : "true");
                                } else {
                                    if (referringParams.has("event_id")) {
                                        showEventDetails(
                                                EventsHighEndpoints.getEventDetailsURI(City.BANGALORE, referringParams.getString("event_id")), null);
                                    } else if (referringParams.has("event_uri")) {
                                        Uri uri = Uri.parse(referringParams.getString("event_uri"));
                                        showSearchView(uri.getLastPathSegment());
                                    } else if (referringParams.has("offer_id")) {
                                        if (account.getLastCity() == City.BANGALORE && viewPager != null) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    viewPager.setCurrentItem(2);
                                                }
                                            });

                                        }
                                    }

                                    if (referringParams.has("referrer2")) {
                                        String referrer = referringParams.getString("referrer2");
                                        Log.i("referrer2", referrer);
                                        new Account(LaunchActivity.this).recordReferrer(referrer, true);
                                    }
                                    //showEventDetails((Event)( obj.get("event")), eventsContext.getLabel(), null);
                                }
                            } catch (JSONException e) {
                                // showNextScreen();
                                e.printStackTrace();
                            }
                            System.out.println("JsonObject received" + referringParams);
                        }
                    } else {
                        //  showNextScreen();
                        Log.i("MyApp", error.getMessage());
                    }
                }
            }, getIntent().getData(), LaunchActivity.this);
    }
}
