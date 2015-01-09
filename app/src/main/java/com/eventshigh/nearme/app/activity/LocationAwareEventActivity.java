package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.DatePicker;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.task.EventsFetcherTask;
import com.eventshigh.nearme.app.task.EventsFetcherTask.EventsFetcherCallBack;
import com.eventshigh.nearme.app.ui.EventSearchSuggestionsProvider;
import com.eventshigh.nearme.app.ui.LocationPickerDialog;
import com.eventshigh.nearme.app.ui.LocationPickerDialog.OnLocationSelection;
import com.eventshigh.nearme.app.ui.OnBoardingHelper;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventFetcherParam;
import com.eventshigh.nearme.app.utils.EventsCollection;
import com.eventshigh.nearme.app.utils.EventsCollection.Builder;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import fr.nicolaspomepuy.discreetapprate.AppRate;
import fr.nicolaspomepuy.discreetapprate.AppRate.OnShowListener;
import fr.nicolaspomepuy.discreetapprate.RetryPolicy;

/**
 * Base activity for location aware events listing. This class implements
 * common methods to get user location and fetch listings when needed.
 * This is an abstract activity and the event UI is left to implementing
 * class.
 * <p/>
 *
 * The parent activity view should have {@link android.widget.LinearLayout}
 * to hold day picker.
 */
public abstract class LocationAwareEventActivity extends BaseActivity {

    // ***********************
    // CONSTANTS
    // ***********************
    public static final String EXTRA_EVENT_FETCHER_PARAM = EventFetcherParam.class.getSimpleName();
    public static final String EXTRA_TAG_NAME_PARAM = "extra.event.tag.name";
    public static final int NUM_MAX_TABS = 10;
    public static final int SECONDS_FOR_REFRESH = 600;


    // ***********************
    // MEMBERS
    // ***********************

    // Last city,day for which events are shown.
    protected EventFetcherParam lastEventFetcherParam;
    // Last fetched events collection.
    private EventsCollection events;
    // Tag selected from tab bar for which events are shown.
    private String lastSelectedTag;
    // last fetcher used to fetch events.
    private EventsFetcherTask fetcher;
    // On boarding helper.
    private OnBoardingHelper onBoardingHelper;
    // when was this activity last started on.
    private long lastStartedAt;
    // override the cache?
    private boolean shouldOverrideCache = false;


    // ***********************
    // Activity lifecycle  Methods
    // See http://developer.android.com/training/basics/activity-lifecycle/starting.html
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lastStartedAt = 0;

        // Set the context in term of lastEventFetcherParam. Use Inent
        // to restore the context.
        lastEventFetcherParam = new EventFetcherParam(null, "");

        // See if we have context passed to us within intent.
        Intent intent = getIntent();
        EventFetcherParam param = intent.getParcelableExtra(EXTRA_EVENT_FETCHER_PARAM);
        if (param != null) {
            lastEventFetcherParam = param;
        }
        lastSelectedTag = intent.getStringExtra(EXTRA_TAG_NAME_PARAM);

        // Show query as title.
        ActionBar actionBar = getActionBar();
        if (actionBar != null && !lastEventFetcherParam.query.isEmpty()) {
            if (EventsHighEndpoints.isDateQuery(lastEventFetcherParam.query)) {
                actionBar.setTitle(DateTimeUtils.dateQueryToTitle(lastEventFetcherParam.query));
            } else {
                reportActionToAnalytics("search", lastEventFetcherParam.query);
                EventSearchSuggestionsProvider.saveRecentQuery(this, lastEventFetcherParam.query);
                actionBar.setTitle(lastEventFetcherParam.query);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // The activity could have started either for first time or when user
        // launches the sleeping app or when he returns from details pane.
        // We do not refresh the app if user is in same session or has returned
        // within {@code SECONDS_FOR_REFRESH} seconds.
        if (lastStartedAt < System.currentTimeMillis() - SECONDS_FOR_REFRESH * 1000) {
            // If location is passed in param, use it.
            LatLng location = lastEventFetcherParam.location;
            if (location == null) {
                City lastCity = GcmRegistration.getInstance(getApplicationContext()).getLastCity();
                if (lastCity != null) {
                    location = lastCity.cityBounds.getCenter();
                }
            }

            if (location == null) {
                askUserForLocation();
            } else {
                lastEventFetcherParam.changeLocation(null);
                updateUserLocation(location);
            }
        }

        lastStartedAt = System.currentTimeMillis();

        // Show the rate this app in non intrusive way.
        AppRate.with(this)
                .delay(3000).initialLaunchCount(5).retryPolicy(RetryPolicy.EXPONENTIAL)
                .text(R.string.action_share_app).listener(mOnShowListener)
                .checkAndShow();
    }

    @Override
    protected void onDestroy() {
        // See http://stackoverflow.com/questions/22924825/view-not-attached-to-window-manager-crash.
        if (fetcher != null) {
            fetcher.destroy();
        }

        super.onDestroy();
    }

    @Override
    public boolean onSearchRequested() {
        reportActionToAnalytics("onSearchRequested");
        Bundle appData = new Bundle();
        appData.putParcelable(EXTRA_EVENT_FETCHER_PARAM, lastEventFetcherParam);
        startSearch(null, false, appData, false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }

        if (id == R.id.action_list) {
            switchTo(EventGridActivity.class);
            return true;
        }

        if (id == R.id.action_map) {
            switchTo(MapsActivity.class);
            return true;
        }

        if (id == R.id.action_change_location) {
            askUserForLocation();
            return true;
        }

        if (id == R.id.action_filter) {
            DialogFragment selectDateFragment = new DatePickerFragment();
            if (lastEventFetcherParam.city != null) {
                Bundle args = new Bundle();
                args.putString(City.class.getName(), lastEventFetcherParam.city.toString());
                selectDateFragment.setArguments(args);
            }
            selectDateFragment.show(getFragmentManager(), "selectDate");
            return true;
        }

        if (id == R.id.debug_cache_override) {
            shouldOverrideCache = !item.isChecked();
            item.setChecked(shouldOverrideCache);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // ***********************
    // Delegated methods
    // ***********************

    /**
     * Sets new events data. This is called when we get new events from
     * {@link com.eventshigh.nearme.app.task.EventsFetcherTask}.
     *
     * EventFetcher could be asked to fetch events when user changes the city or day.
     *
     * @param events a list of events as returned by {@link com.eventshigh.nearme.app.task.EventsFetcherTask}
     */
    protected abstract void updateNewEvents(List<Event> events);

    /**
     * Updates the user location as reported by LocationClient.
     *
     * @param userLocation user location as reported by location client.
     */
    protected abstract void updateUserLocation(LatLng userLocation);

    /**
     * @return true if location should be shown in action bar as subtitle.
     */
    protected abstract boolean showLocationInActionBar();


    // ***********************
    // Helper methods
    // ***********************

    private void updateListingAndShowHelpIfNeeded(List<Event> events) {
        updateNewEvents(events);

        if (!events.isEmpty()) {
            if (onBoardingHelper == null) {
                onBoardingHelper = new OnBoardingHelper(this);
            }

            onBoardingHelper.next();
        }
    }

    private void fetchNewListing() {
        reportActionToAnalytics("fetchNewListing");
        fetcher = new EventsFetcherTask(
                LocationAwareEventActivity.this, shouldOverrideCache, mEventsFetcherCallBack);
        fetcher.execute(lastEventFetcherParam);
    }

    protected void askUserForLocation() {
        reportActionToAnalytics("askUserForLocation");
        String countryCode = lastEventFetcherParam.city == null ?
                null : lastEventFetcherParam.city.countryCode;
        new LocationPickerDialog().show(this, countryCode, new OnLocationSelection() {
            @Override
            public void onLocationSelection(String locationString, LatLng locationPoint) {
                if (showLocationInActionBar()) {
                    ActionBar actionBar = getActionBar();
                    if (actionBar != null) {
                        actionBar.setSubtitle(locationString);
                    }
                }
                updateUserLocation(locationPoint);
            }
        });
    }

    /**
     * Refresh the event listings if user city has changed as per new location.
     * Parent activity can pass {@code NULL} to cleanup any state like {@code lastCity}.
     *
     * @param userLocation location of user.
     * @return true if city was updated as per new location and request for
     * fetching new events was submitted.
     */
    protected boolean refreshListingsIfNeeded(@Nullable LatLng userLocation) {
        City userCity = City.getCity(userLocation);
        if (userCity == null) {
            if (userLocation != null) {
                reportActionToAnalytics("unsupportedCity");
                Toast.makeText(this, R.string.no_event, Toast.LENGTH_SHORT).show();
            }
            lastEventFetcherParam.changeLocation(null);
            basicTabs();
            return true;
        }

        if (!lastEventFetcherParam.changeLocation(userLocation)) {
            basicTabs();
            fetchNewListing();
            return true;
        }

        return false;
    }

    protected void switchTo(Class<?> cls) {
        reportActionToAnalytics("switchView");
        Intent intent = new Intent(this, cls)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_EVENT_FETCHER_PARAM, lastEventFetcherParam);
        if (lastSelectedTag != null) {
            intent.putExtra(EXTRA_TAG_NAME_PARAM, lastSelectedTag);
        }
        startActivity(intent);
    }

    private void basicTabs() {
        events = new Builder(City.BANGALORE, Collections.EMPTY_SET).build();

        ActionBar actionBar = getActionBar();
        if (actionBar != null && actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS) {
            String lastSelectedTagSave = lastSelectedTag;
            actionBar.removeAllTabs();
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(EventsCollection.ALL_EVENTS_CATEGORY + " (" + 0 + " )")
                            .setTag(EventsCollection.ALL_EVENTS_CATEGORY)
                            .setTabListener(mTabListener));
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(EventsCollection.RECOMMENDED_EVENTS_CATEGORY + " (" + 0 + " )")
                            .setTag(EventsCollection.RECOMMENDED_EVENTS_CATEGORY)
                            .setTabListener(mTabListener));

            if (lastSelectedTagSave != null) {
                lastSelectedTag = lastSelectedTagSave;
            }
        }
    }


    // ***********************
    // Callbacks
    // ***********************

    // This callback is called by EventsFetcher when new set of events are available. We build the
    // markers for all events and then call method to show selected markers.
    private EventsFetcherCallBack mEventsFetcherCallBack = new EventsFetcherCallBack() {
        @Override
        public void OnEventsAvailable(EventFetcherParam param, EventsCollection events) {
            lastEventFetcherParam = param;
            LocationAwareEventActivity.this.events = events;

            // Update tabs if needed.
            ActionBar actionBar = getActionBar();
            if (actionBar == null) {
                return;
            }

            List<Pair<String, Integer>> tags = events.getTags();
            if (tags.size() > NUM_MAX_TABS) {
                tags = tags.subList(0, NUM_MAX_TABS);
            }

            if (actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS || tags.size() > 1) {
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
                actionBar.removeAllTabs();

                int selectedItem = 0;
                if (lastSelectedTag != null) {
                    for (int i = 0; i < tags.size(); i++) {
                        if (tags.get(i).first.equalsIgnoreCase(lastSelectedTag)) {
                            selectedItem = i;
                            break;
                        }
                    }
                }

                for (Pair<String, Integer> tag : tags) {
                    Tab tab = actionBar.newTab()
                            .setText("  " + tag.first + "  \n  (" + tag.second + ")  ")
                            .setTag(tag.first)
                            .setTabListener(mTabListener);
                    EventCategory category = Event.getCategoryFromTag(tag.first);
                    if (category != null) {
                        int iconRes = category.getIconResourceId();
                        if (iconRes != R.drawable.icon_other) {
                            tab.setIcon(iconRes);
                        }
                    }
                    actionBar.addTab(tab, false);
                }

                actionBar.setSelectedNavigationItem(selectedItem);
            } else {
                if (!param.query.isEmpty()) {
                    int numEvents = tags.isEmpty() ? 0 : events.getEvents(0).size();
                    actionBar.setTitle(DateTimeUtils.dateQueryToTitle(param.query) + " (" + numEvents + ")");
                }

                updateListingAndShowHelpIfNeeded(events.getEvents(0));
            }
        }
    };

    private TabListener mTabListener = new TabListener() {
        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (events != null) {
                lastSelectedTag = tab.getTag().toString();
                List<Event> eventsForTag = events.getEvents(tab.getPosition());
                if (!eventsForTag.isEmpty() && getActionBar().getNavigationItemCount() > 1) {
                    reportActionToAnalytics("filterByCategory", lastSelectedTag);
                }
                updateListingAndShowHelpIfNeeded(eventsForTag);
            }
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    };

    private OnShowListener mOnShowListener = new OnShowListener() {
        @Override
        public void onRateAppShowing(AppRate appRate, final View view) {
            reportActionToAnalytics("shareAppShown");
            view.findViewById(R.id.dar_rate_element).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareApp();
                    view.setVisibility(View.GONE);
                }
            });
        }

        @Override
        public void onRateAppDismissed() {
            reportActionToAnalytics("shareAppDismissed");
        }

        @Override
        public void onRateAppClicked() {
        }
    };

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener, DialogInterface.OnClickListener {
        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

        private Date selectedDate = null;
        private boolean filterRequested = false;
        private long numDaysAhead = -1;
        private Date today;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            City city = null;
            Bundle args = getArguments();
            if (args != null) {
                String cityStr = args.getString(City.class.getName());
                if (cityStr != null) {
                    city = City.valueOf(cityStr);
                }
            }

            Calendar cal = Calendar.getInstance();
            if (city != null) {
                cal.setTimeZone(TimeZone.getTimeZone(city.timeZone));
            }

            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            today = new Date(year - 1900, month, day);

            DatePickerDialog datePicker = new DatePickerDialog(getActivity(), this, year, month, day);
            datePicker.setCancelable(true);
            datePicker.setCanceledOnTouchOutside(true);
            datePicker.getDatePicker().setMinDate(today.getTime());
            datePicker.getDatePicker().setMaxDate(today.getTime() + 7 * 24 * 3600 * 1000L);

            datePicker.setButton(DialogInterface.BUTTON_POSITIVE,
                    getActivity().getString(R.string.action_filter), this);
            return datePicker;
        }

        public synchronized void onDateSet(DatePicker view, int year, int month, int day) {
            selectedDate = new Date(year - 1900, month, day);
            numDaysAhead = (selectedDate.getTime() - today.getTime()) / (24*3600*1000L);
            if (filterRequested) {
                filterByDate();
            }
        }

        @Override
        public synchronized void onClick(DialogInterface dialog, int which) {
            filterRequested = true;
            if (selectedDate != null) {
                filterByDate();
            }
        }

        private void filterByDate() {
            LocationAwareEventActivity activity = (LocationAwareEventActivity) getActivity();
            activity.reportActionToAnalytics("filterByDate", Long.toString(numDaysAhead) + "days later");
            EventFetcherParam param = new EventFetcherParam(activity.lastEventFetcherParam.location,
                    DATE_FORMAT.format(selectedDate));
            Intent intent = new Intent(getActivity(), activity.getClass())
                    .putExtra(EXTRA_EVENT_FETCHER_PARAM, param);
            startActivity(intent);
        }
    }
}
