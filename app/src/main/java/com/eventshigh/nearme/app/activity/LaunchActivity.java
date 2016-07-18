package com.eventshigh.nearme.app.activity;

import android.Manifest;
import android.Manifest.permission;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
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
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.broadcast.GeofenceStartService;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.LocalityLatLong;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.data.MyTicketObject;
import com.eventshigh.nearme.app.network.EventRequest;
import com.eventshigh.nearme.app.network.MovieDetailRequest;
import com.eventshigh.nearme.app.network.MyTicketsRequest;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.CitySelectDialog;
import com.eventshigh.nearme.app.ui.adapter.CityListAdapter;
import com.eventshigh.nearme.app.ui.adapter.CityListAdapter.OnCitySelectionListener;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.plus.PlusOneButton;
import com.google.android.gms.plus.PlusOneButton.OnPlusOneClickListener;
import com.zendesk.sdk.model.helpcenter.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import pl.snowdog.material.ui.ToolbarColorizeHelper;


/**
 * Application Main or launch activity.
 */

public class LaunchActivity extends BaseContextActivity {
    // Constants
    public static final String DEFAULT_TAB_PARAM = LaunchActivity.class.getName() + "_default_tab";
    public static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 0x001;
    public static final int SUBMIT_REVIEW_REQUEST_CODE = 900;


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
    public static final String MOVIES_TAB = "movies";
    public ArrayList<String> TABS = new ArrayList<>();

    ProgressDialog progress;


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
        startService(new Intent(this, GeofenceStartService.class));

        //fetchEventAttendedDetails();

        if (isFinishing()) {
            return;
        }

        // Set defaults for preferences.
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
        setUserCityHeader();
        getIntent().getAction();


    }

    public void fetchEventAttendedDetails() {
        MyTicketsRequest.submit(this, Request.Priority.IMMEDIATE, this, true, mTicketsListener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("LaunchActivity", "Not able to fetch ticketed events attended");
            }
        }, true);
    }

    private Response.Listener<List<MyTicketObject>> mTicketsListener = new Response.Listener<List<MyTicketObject>>() {
        @Override
        public void onResponse(List<MyTicketObject> tickets, boolean isIntermediate) {
            try {
                if (tickets != null) {
                    MyTicketObject reqMyTickets = null;
                    for (MyTicketObject myTicketObject : tickets) {
                        Date date = new SimpleDateFormat("EEE MMM dd, hh:mm aa", Locale.ENGLISH).parse(myTicketObject.getEventTime());
                        long milliseconds = date.getTime();
                        long millisecondsFromNow = milliseconds - date.getTime();
                        if (millisecondsFromNow < 86400000) {
                            reqMyTickets = myTicketObject;
                            break;
                        }
                    }
                    if (reqMyTickets != null) {
                        showDialog(reqMyTickets);
                        reportActionToAnalytics("reviewModelShow");
                    }
                }
            } catch (ParseException pe) {
                Log.e("Launch Activity", pe.toString());
            }
        }
    };

    public void showDialog(MyTicketObject myTicketObject) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ReviewEventAttendedDialog newFragment = new ReviewEventAttendedDialog();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ReviewEventAttendedDialog.MY_TICKET, myTicketObject);
        newFragment.setArguments(bundle);
        boolean mIsLargeLayout = false;
        if (mIsLargeLayout) {
            newFragment.show(fragmentManager, "dialog");
        } else {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.add(android.R.id.content, newFragment)
                    .addToBackStack(null).commit();
        }
    }


    TextView toolbarTitleText;

    public void setUserCityHeader() {
        View view = LayoutInflater.from(this).inflate(R.layout.home_toolbar_city_layout, toolbar, false);
        toolbarTitleText = (TextView) view.findViewById(R.id.location_text);
        view.findViewById(R.id.parent_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (account.getLastCity() != null) {

                    Intent intent = new Intent(LaunchActivity.this, PlacesAutocompleteBoundedActivity.class);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);

                } else {
                    Toast.makeText(LaunchActivity.this, "Please select the city first", Toast.LENGTH_LONG).show();
                }
            }
        });
        toolbar.addView(view);
    }


    @Override
    public View getViewForSnackbar() {
        return null;
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
                startActivity(new Intent(this, NewOnboardingActivity.class));
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
            if (account.getLastLocality() != null && account.getLastLocality().getName().length() > 0) {
                toolbarTitleText.setText(account.getLastLocality().getName());
            } else {
                toolbarTitleText.setText(account.getLastCity().name());
            }

            currentCity.setText(account.getLastCity().name());
            currentCity.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CitySelectDialog.show(LaunchActivity.this, account, new CitySelectDialog.CitySelectionCallback() {
                        @Override
                        public void onCityChanged(City city) {
                            toolbarTitleText.setText(city.name());
                            currentCity.setText(city.name());
                            cityChanged(city);
                        }
                    });
                }
            });
        }

        // new InitiateBranchAsyncTask(getIntent().getData()).execute();
        loadBranchInstance();
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

                    // got the location, client is not needed.
                    client.disconnect();


                } else {
                    drawer.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mConnectionCallbacks.onConnected(null);
                        }
                    }, 100);

                   /* if (progress == null) {
                        progress = ProgressDialog.show(LaunchActivity.this, null, "Finding Location..Please wait");
                        startLocationUpdates();
                    }*/
                }
            }
            mOnConnectionFailedListener.onConnectionFailed(null);
            //noinspection ConstantConditions


        }

        @Override
        public void onConnectionSuspended(int i) {
            // do nothing.
        }
    };

    protected void startLocationUpdates() {
        // Create the location request
        LocationRequest mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)
                .setFastestInterval(2000);
        // Request location updates
        /*if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            progress.dismiss();
            Toast.makeText(LaunchActivity.this, "Could not connect. Permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }*/
        try {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new android.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        progress.dismiss();
                        LatLng latLng = LocationUtils.locationToLatLng(location);
                        eventsContext.changeLocation(latLng);
                        if (eventsContext.city != null) {
                            account.setLastCity(eventsContext.city);
                        } else {
                            reportActionToAnalytics("unsupportedCity");
                        }
                    } else {
                        progress.dismiss();
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
           /* locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0L, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        progress.dismiss();
                        LatLng latLng = LocationUtils.locationToLatLng(location);
                        eventsContext.changeLocation(latLng);
                        if (eventsContext.city != null) {
                            account.setLastCity(eventsContext.city);
                        } else {
                            reportActionToAnalytics("unsupportedCity");
                        }
                    } else {
                        progress.dismiss();
                    }
                }
            });*/
           /* LocationServices.FusedLocationApi.requestLocationUpdates(client,
                    mLocationRequest, new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            if (location != null) {
                                progress.dismiss();
                                LatLng latLng = LocationUtils.locationToLatLng(location);
                                eventsContext.changeLocation(latLng);
                                if (eventsContext.city != null) {
                                    account.setLastCity(eventsContext.city);
                                } else {
                                    reportActionToAnalytics("unsupportedCity");
                                }
                            } else {
                                progress.dismiss();
                            }
                        }
                    });*/
        } catch (SecurityException e) {
            progress.dismiss();
        }
    }

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
            toolbarTitleText.setText(userCity.name() + "  \u22C0");
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
        //   TABS.add(MOVIES_TAB);
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
                int position = tab.getPosition();
                if (position == 0) {

                } else {
                    viewPager.setCurrentItem(tab.getPosition());
                }
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
        if (account.getLastCity() != null) {
            if (account.getLastLocality() != null && account.getLastLocality().getName().length() > 0) {
                toolbarTitleText.setText(account.getLastLocality().getName());
            } else {
                toolbarTitleText.setText(account.getLastCity().name());
            }

            if (currentCity != null)
                currentCity.setText(account.getLastCity().name());
        }
    }

    private void setupTabIconsWithOffer() {

        TextView tabOne = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabOne.setText("Me");
        tabOne.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_events, 0, 0);
        tabOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LaunchActivity.this, UserProfileActivity.class);
                startActivity(intent);
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

        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                String placeName = data.getStringExtra("place_name");
                LatLng latLng = data.getParcelableExtra("place_lat_lng");
                if (latLng != null && placeName != null) {
                    toolbarTitleText.setText(placeName);
                    LocalityLatLong locality = new LocalityLatLong(placeName, latLng);
                    account.setLastLocality(locality);
                    if (weekEventsFragment != null)
                        weekEventsFragment.setSelectedLocalityDistanceText();
                    Log.i("TestActivity", "Place: " + placeName);
                } else {
                    Toast.makeText(this, "Selected locality not found ", Toast.LENGTH_LONG).show();
                }
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.i("TestActivity", status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }

        //Checking if the previous activity is launched on branch Auto deep link.
        if (requestCode == getResources().getInteger(R.integer.EventsDetailDeepLink_code)) {
            //Decide here where  to navigate  when an auto deep linked activity finishes.
            //For e.g. Go to HomeActivity or a  SignUp Activity.
            showNextScreen();
        }

        if (requestCode == SUBMIT_REVIEW_REQUEST_CODE) {
            if (resultCode == RESULT_OK)
                super.onBackPressed();
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

    NewWeekEventsFragment weekEventsFragment;

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

                return EmptyFragment.newInstance(null);
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

           /* if(TABS.get(position).equalsIgnoreCase(MOVIES_TAB)){
                return new MoviesListFragment();
            }*/
            weekEventsFragment = NewWeekEventsFragment.getInstance(eventsContext, false);
            return weekEventsFragment;

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
            if (position == 0) {
                Intent intent = new Intent(LaunchActivity.this, UserProfileActivity.class);
                startActivity(intent);
            } else {
                viewPager.setCurrentItem(position);
            }

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
                        try {
                            if (referringParams.length() == 0) {
                                // showNextScreen();
                                Log.i("Event_detail_missed", "refering params empty");
                            } else if (referringParams.has("profile_id")) {
                                String profileId = referringParams.getString("profile_id");
                                Intent intent = new Intent(LaunchActivity.this, UserProfileActivity.class);
                                intent.putExtra(UserProfileActivity.PROFILE_ID, profileId);
                                startActivity(intent);
                            } else if (!referringParams.optBoolean("review", false)) {
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
                                    } else if (referringParams.has("movie_id")) {
                                        int id = referringParams.getInt("movie_id");
                                        Intent intent = new Intent(LaunchActivity.this, MovieDetailActivity.class);
                                        intent.putExtra(MovieDetailActivity.MOVIE_ID, id);
                                        startActivity(intent);
                                    }

                                    /*if (referringParams.has("referrer2")) {
                                        String referrer = referringParams.getString("referrer2");
                                        Log.i("referrer2", referrer);
                                        new Account(LaunchActivity.this).recordReferrer(referrer, true);
                                    }*/
                                    //showEventDetails((Event)( obj.get("event")), eventsContext.getLabel(), null);
                                }

                                System.out.println("JsonObject received" + referringParams);
                            } else if (referringParams.optBoolean("review", false)) {

                                if (!referringParams.getBoolean("+is_first_session") && !referringParams.getBoolean("+clicked_branch_link")) {
                                    //showNextScreen();
                                    Log.i("Event_detail_missed", referringParams.getBoolean("+is_first_session") ? "False" : "true");
                                } else {
                                    if (referringParams.has("event_id")) {
                                        writeEventReview(
                                                EventsHighEndpoints.getEventDetailsURI(City.BANGALORE, referringParams.getString("event_id")), null);
                                    } else if (referringParams.has("event_uri")) {
                                        Uri uri = Uri.parse(referringParams.getString("event_uri"));
                                        showSearchView(uri.getLastPathSegment());
                                    } else if (referringParams.has("movie_id")) {
                                        int id = referringParams.getInt("movie_id");
                                        writeMovieReview(id, null);
                                    }

                                }
                            }
                        } catch (JSONException e) {
                            // showNextScreen();
                            e.printStackTrace();
                        }
                    } else {
                        //  showNextScreen();
                        Log.i("MyApp", error.getMessage());
                    }
                }
            }, getIntent().getData(), LaunchActivity.this);
    }

    public void writeMovieReview(int movieId, @Nullable String label) {
        //reportActionToAnalytics("showEventDetails", label);
        if (movieId != -1)
            MovieDetailRequest.submit(this, movieId, Request.Priority.IMMEDIATE,
                    new Response.Listener<MovieDetailObject>() {
                        @Override
                        public void onResponse(MovieDetailObject movieDetailObject, boolean b) {
                            Intent detailIntent = new Intent(LaunchActivity.this, MovieDetailActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putParcelable(MovieDetailActivity.MOVIE_DETAIL_OBJECT, movieDetailObject);
                            bundle.putString(MovieDetailActivity.OBJECT_TYPE, "movie");
                            detailIntent.putExtras(bundle);
                            startActivity(detailIntent);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            Toast.makeText(LaunchActivity.this, R.string.failed_load,
                                    Toast.LENGTH_SHORT).show();
                            VolleyHelper.log(LaunchActivity.this, volleyError);
                            finish();
                        }
                    });

    }

    public void writeEventReview(Uri eventDetailsURI, @Nullable String label) {

        EventRequest.submit(this, eventDetailsURI, Request.Priority.IMMEDIATE, new Response.Listener<Event>() {
                    @Override
                    public void onResponse(Event event, boolean b) {
                        //reportActionToAnalytics("showEventDetails", label);
                        Intent detailIntent = new Intent(LaunchActivity.this, WriteReviewActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(NewEventDetailActivity.EVENT_OBJECT, event);
                        bundle.putString(MovieDetailActivity.OBJECT_TYPE, "event");
                        detailIntent.putExtras(bundle);
                        startActivity(detailIntent);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Toast.makeText(LaunchActivity.this, R.string.failed_load,
                                Toast.LENGTH_SHORT).show();
                        VolleyHelper.log(LaunchActivity.this, volleyError);
                        finish();
                    }
                });

    }


}
