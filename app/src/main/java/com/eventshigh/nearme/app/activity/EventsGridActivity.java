package com.eventshigh.nearme.app.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.BrowseFilterAttributes;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventFilterAttribute;
import com.eventshigh.nearme.app.data.LocalityLatLong;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest;
import com.eventshigh.nearme.app.ui.FBSigninDialog;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.ui.animation.ResizeAnimation;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;
import com.google.android.gms.maps.model.LatLng;
import com.squareup.timessquare.CalendarPickerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import pl.snowdog.material.ui.ToolbarColorizeHelper;

/**
 * Shows the events in Grid layout.
 */
public class EventsGridActivity extends BaseContextActivity {

    private boolean showFollowCard;

    String shareImageUrl;

    private boolean isFromNotification;

    EventsFragment eventsFragment;

    HorizontalScrollView categoryFilter, priceFilter, dateFilter, sortFilter, zoneFilter, localitiesFilter;

    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 0x002;

    boolean isCategoryFilterVisible, isZoneFilterVisible, isDateFilterVisible, isSortFilterVisible;

    boolean isTodaySelected;

    public static final int SORT_STATE_TRENDING = 1;
    public static final int SORT_STATE_PRICE = 2;
    public static final int SORT_STATE_DISTANCE = 3;
    public static final int SORT_STATE_TIME = 4;


    Account account;


    public static final int CATEGORY_FILTER = 1;
    public static final int DATE_FILTER = 2;
    public static final int PRICE_FILTER = 3;
    public static final int ZONE_FILTER = 4;
    public static final int SPECIAL_FILTER = 5;
    public static final int TICKETED_FILTER = 6;
    public static final int LOCALITY_FILTER = 7;


    public static final int FREE = 1;
    public static final int UPTO_250 = 2;
    public static final int PRICE_250_TO_750 = 3;
    public static final int PRICE_750_TO_1500 = 4;
    public static final int MORE_THAN_1500 = 5;

    LinearLayout filtersContainer, filtersHeaderContainer;

    ImageView expandBtn;

    boolean isMapVisible;

    public View fabBrowseMap, fabSpecialFilter;

    public static final int SHOW_EVENT_LIST_STATE = 1;
    public static final int SHOW_FILTERS_STATE = 2;


    boolean isFabFilterVisible;

    boolean isNearMeQuery;

    boolean isNewYearQuery;


    ArrayList<String> specialFilters, previousSelectedFilters;
    boolean isShowLocalitiesFilter;

    public static final String[] EXPLORE_TAGS = {
            EventCategory.NIGHTLIFE.categoryName,
            EventCategory.LIVE_PERFORMANCES.categoryName,
            EventCategory.OUTDOORS.categoryName,
            EventCategory.HEALTH_WELLNESS.categoryName,
            EventCategory.KIDS_ENTERTAINMENT.categoryName,
            EventCategory.SPORTS.categoryName,
            EventCategory.WORKSHOPS.categoryName,
            EventCategory.TECH.categoryName,
            EventCategory.ART.categoryName,
            EventCategory.FOOD.categoryName

    };

    AutofitRecyclerView specialFilterGrid;

    LinearLayout specialFilterLayout;


    ArrayList<String> selectedLocalities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout(R.layout.activity_events_grid);
        account = new Account(this);
        if (eventsContext.location == null) {
            City lastCity = new Account(this).getLastCity();
            if (lastCity != null) {
                reportActionToAnalytics("usedLastCity");
                eventsContext.changeLocation(lastCity.cityBounds.getCenter());
            }
        }

        filtersHeaderContainer = (LinearLayout) findViewById(R.id.filters_header_container);
        filtersContainer = (LinearLayout) findViewById(R.id.filters_container);
        expandBtn = (ImageView) findViewById(R.id.expand_btn);
        expandBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFiltersShown) {
                    collapseAnimation(-1);
                    reportActionToAnalytics("filters", eventsContext.query + "-Collapse");
                } else {
                    expandAnimation();
                    reportActionToAnalytics("filters", eventsContext.query + "-Expand");
                }
            }
        });
        categoryFilter = (HorizontalScrollView) findViewById(R.id.category_filter);
        priceFilter = (HorizontalScrollView) findViewById(R.id.price_filter);
        sortFilter = (HorizontalScrollView) findViewById(R.id.sort_container);
        dateFilter = (HorizontalScrollView) findViewById(R.id.date_filter);
        zoneFilter = (HorizontalScrollView) findViewById(R.id.zone_filter);
        localitiesFilter = (HorizontalScrollView) findViewById(R.id.localities_filter);

        // Show query as title.
        if (!eventsContext.query.isEmpty()) {
            setTitle();
        }

        // Fab Share.
        fabBrowseMap = findViewById(R.id.fab_browse_map);
        fabBrowseMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (eventsFragment != null) {
                    if (eventsFragment.isMapShown) {
                        if (eventsFragment.isMapListShown) {
                            eventsFragment.hideMapEvents(SHOW_EVENT_LIST_STATE);
                        } else {
                            eventsFragment.showListView();
                        }
                        reportActionToAnalytics("hideMapFabClick");
                        ((FloatingActionButton) fabBrowseMap).setImageResource(R.drawable.ic_browse_map);
                        collapseAnimation(SHOW_SORT);
                    } else {
                        reportActionToAnalytics("showMapFabClick");
                        if (ActivityCompat.checkSelfPermission(EventsGridActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            // Request missing location permission.
                            ActivityCompat.requestPermissions(EventsGridActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
                        } else {
                            if (isFiltersShown) {
                                collapseAnimation(SHOW_MAP_VIEW);
                            } else {
                                eventsFragment.hideListView();
                                //collapseAnimation(HIDE_SORT);
                            }
                            ((FloatingActionButton) fabBrowseMap).setImageResource(R.drawable.ic_list_menu);
                        }

                    }

                }

            }
        });


        fabSpecialFilter = findViewById(R.id.fab_special_filter);
        fabSpecialFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyArrayListContents(previousSelectedFilters, specialFilters);
                translateSpecialFilterUp();
            }
        });
        // Should we show follow widget?
        showFollowCard = !eventsContext.query.isEmpty() &&
                eventsContext.dateFilter.isEmpty() &&
                !EventsHighEndpoints.isDateQuery(eventsContext.query) &&
                !EventsHighEndpoints.isMyEventQuery(eventsContext.query) &&
                !EventsHighEndpoints.isFeaturedEventQuery(eventsContext.query);

        addToolbarView();

        if (false) {
            // Hide the regular toolbar and show the follow toolbar
            toolbar.setVisibility(View.GONE);
            toolbar = (Toolbar) findViewById(R.id.follow_toolbar);
            toolbar.setVisibility(View.VISIBLE);


            setSupportActionBar(toolbar);

            //updateToolbar(0);
            setTitle();
        }
        if (getIntent() != null &&
                getIntent().getAction() != null && getIntent().getAction().startsWith(NOTIFICATION_ACTION)) {
            isFromNotification = true;
        }

        // Add Events Fragment.
        Fragment eventFragment;
        if (!eventsContext.query.isEmpty()) {

            if (eventsContext.query.equalsIgnoreCase("this week") || eventsContext.query.equalsIgnoreCase("today") || eventsContext.query.contains(EventsHighEndpoints.QUERY_NEARME)) {
                if (eventsContext.query.equalsIgnoreCase("today"))
                    isTodaySelected = true;

                if (eventsContext.query.contains(EventsHighEndpoints.QUERY_NEARME)) {
                    isNearMeQuery = true;
                    showFollowCard = false;
                } else {
                    isNearMeQuery = false;
                }

                isCategoryFilterVisible = true;
                isSortFilterVisible = true;
                categoryFilter.setVisibility(View.VISIBLE);
                findViewById(R.id.category_separator).setVisibility(View.VISIBLE);
                findViewById(R.id.sort_separator).setVisibility(View.VISIBLE);

            } else {
                isCategoryFilterVisible = false;
                isSortFilterVisible = true;
                categoryFilter.setVisibility(View.GONE);
                findViewById(R.id.category_separator).setVisibility(View.GONE);
                findViewById(R.id.sort_separator).setVisibility(View.VISIBLE);

            }

            if (eventsContext.query.equalsIgnoreCase("New Year Parties") || eventsContext.query.equalsIgnoreCase("Parties And Nightlife")) {

                isNewYearQuery = true;
                isDateFilterVisible = false;
                isSortFilterVisible = false;
                isCategoryFilterVisible = true;
                categoryFilter.setVisibility(View.VISIBLE);
                findViewById(R.id.category_separator).setVisibility(View.VISIBLE);
                sortFilter.setVisibility(View.GONE);
                findViewById(R.id.sort_separator).setVisibility(View.GONE);
                dateFilter.setVisibility(View.GONE);
                findViewById(R.id.date_separator).setVisibility(View.GONE);
                isShowLocalitiesFilter = true;
                isZoneFilterVisible = true;
                zoneFilter.setVisibility(View.VISIBLE);

            } else {
                isShowLocalitiesFilter = false;
                isZoneFilterVisible = false;
                isDateFilterVisible = true;
                dateFilter.setVisibility(View.VISIBLE);
                findViewById(R.id.date_separator).setVisibility(View.VISIBLE);
                zoneFilter.setVisibility(View.GONE);

            }

            boolean showEhInviteForNotification = getIntent() != null &&
                    getIntent().getAction() != null && getIntent().getAction().startsWith(NOTIFICATION_ACTION);
            EventsFragment eventFragment1 = EventsFragment.getInstance(
                    eventsContext, showFollowCard, false, showEhInviteForNotification, (SocialInvitationsRequest.SpecialCoupons) getIntent().getParcelableExtra("special_obj"), isTodaySelected, null, true);
            eventFragment1.setOnScrollListener(
                    false ? followCardScrollListener : doNothingScrollListener);

            this.eventsFragment = eventFragment1;
            FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
            tr.replace(R.id.event_container, eventsFragment);
            //
            tr.commit();
        } else {
            NewWeekEventsFragment thisWeekFragment = NewWeekEventsFragment.getInstance(eventsContext, false);
            FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
            //tr.setCustomAnimations(R.anim.animate_slide_up, R.anim.stay);
            // tr.replace(R.id.event_container, thisWeekFragment);
            tr.commit();
        }
        //  addFiltersData();
        specialFilterLayout = (LinearLayout) findViewById(R.id.special_filter_layout);
        specialFilterGrid = (AutofitRecyclerView) findViewById(R.id.special_filter_grid);
        findViewById(R.id.close_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyArrayListContents(previousSelectedFilters, specialFilters);
                translateSpecialFilterDown(false);
                specialFilterAdapter.notifyDataSetChanged();
            }
        });
        findViewById(R.id.apply_filters).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!equalLists(specialFilters, previousSelectedFilters)) {

                    copyArrayListContents(specialFilters, previousSelectedFilters);
                    translateSpecialFilterDown(true);
                } else {
                    translateSpecialFilterDown(false);
                }
                if (specialFilters != null && specialFilters.size() > 0) {
                    ((FloatingActionButton) fabSpecialFilter).setImageResource(R.drawable.ic_filter_tick);
                } else {
                    ((FloatingActionButton) fabSpecialFilter).setImageResource(R.drawable.ic_filter_white);
                }

            }
        });
    }

    public void copyArrayListContents(ArrayList<String> copyFrom, ArrayList<String> copyTo) {
        if (copyFrom == null && copyTo == null) {
            return;
        }
        if (copyFrom == null) {
            copyTo = null;
            return;
        }
        if (copyTo == null) {
            copyTo = new ArrayList<>();
        }
        copyTo.clear();
        for (String text : copyFrom) {
            copyTo.add(text);
        }
    }

    public boolean equalLists(List<String> one, List<String> two) {
        if (one == null && two == null) {
            return true;
        }

        if ((one == null && two != null)
                || one != null && two == null
                || one.size() != two.size()) {
            return false;
        }

        //to avoid messing the order of the lists we will use a copy
        //as noted in comments by A. R. S.
        one = new ArrayList<String>(one);
        two = new ArrayList<String>(two);

        Collections.sort(one);
        Collections.sort(two);
        return one.equals(two);
    }

    EventsAdapter specialFilterAdapter;

    public void addSpecialFilterGrid(ArrayList<EventFilterAttribute> attributes) {
        fabSpecialFilter.setVisibility(View.GONE);
        /*if (attributes != null && attributes.size() > 0) {
            if (isFabFilterVisible) {
                fabSpecialFilter.setVisibility(View.GONE);
                fabBrowseMap.setVisibility(View.GONE);
            } else {
                fabSpecialFilter.setVisibility(View.VISIBLE);
                fabBrowseMap.setVisibility(View.VISIBLE);
            }
            if (specialFilters == null) {
                specialFilters = new ArrayList<>();
            }
            if (previousSelectedFilters == null) {
                previousSelectedFilters = new ArrayList<>();
            }
            specialFilters = new ArrayList<>(previousSelectedFilters);
            specialFilterAdapter = new EventsAdapter(this);
            specialFilterGrid.setLayoutManager(new LinearLayoutManager(this));
            specialFilterAdapter.setSpecialFilters(attributes, specialFilters);
            specialFilterGrid.setAdapter(specialFilterAdapter);
        } else {
            fabSpecialFilter.setVisibility(View.GONE);
        }*/
    }

    EventsMapFragment mapFragment;


    TextView followBtn;

    public void addToolbarView() {
        if (toolbar != null) {
            View view = LayoutInflater.from(this).inflate(R.layout.event_grid_toolbar_layout, toolbar, false);
            TextView title = (TextView) view.findViewById(R.id.title);
            title.setText(Utils.capitalize(eventsContext.query));
            toolbar.addView(view);
        }
    }

    public void unSelectFollowBtn() {
        reportActionToAnalytics("removeFollowing", eventsContext.query);
        account.setIsFollowing(eventsContext.query, false);
        changeFollowBtnState(false);
    }

    public void selectFollowBtn() {
        reportActionToAnalytics("addFollowing", eventsContext.query);
        if (!account.getUserInfo().isSignedIn) {
            FBSigninDialog.show(this, R.string.ui_signin_via_fb, R.string.ui_signin_fb_plan_more, 1);
        }
        account.setIsFollowing(eventsContext.query, true);
        changeFollowBtnState(true);
    }

    public void changeFollowBtnState(boolean isSelected) {
        if (followBtn != null) {
            if (isSelected) {
                followBtn.setSelected(true);
                followBtn.setText("Following");
            } else {
                followBtn.setSelected(false);
                followBtn.setText("+ Follow");
            }
        }
    }

    boolean isFiltersShown;

    View showFiltersView;


    TextView selectCustomDates, today, tomorrow, weekend;

    List<BrowseFilterAttributes> filters;
    List<String> browseZones;

    public void setCompoundDrawable(TextView textView, int resId) {
        Drawable drawable = getDrawable(resId);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        textView.setCompoundDrawables(null, drawable, null, null);
    }


    public void addFiltersData() {
        int iterateCount = 0;

        final LinearLayout horizontalCategories = (LinearLayout) findViewById(R.id.category_container);
        horizontalCategories.removeAllViews();
        if (isNewYearQuery && filters != null && filters.size() > 0) {
            if (filters.size() > 6) {
                iterateCount = 6;
            } else {
                iterateCount = filters.size();
            }
            for (int i = 0; i < iterateCount; i++) {
                final BrowseFilterAttributes attribute = filters.get(i);
                View view = LayoutInflater.from(this).inflate(R.layout.special_filter_tags_layout, horizontalCategories, false);
                final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
                int lastSpaceIndex = attribute.getName().lastIndexOf(" ");
                String filterName = attribute.getName().substring(0, lastSpaceIndex) + "\n" + attribute.getName().substring(lastSpaceIndex + 1, attribute.getName().length());
                filterText.setText(attribute.getName());
                horizontalCategories.addView(view);
                setCompoundDrawable(filterText, attribute.getIconResourceId());
                filterText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (filterText.isSelected()) {
                            filterText.setSelected(false);
                            setCompoundDrawable(filterText, attribute.getIconResourceId());
                        } else {
                            filterText.setSelected(true);
                            setCompoundDrawable(filterText, attribute.getSelectedIconResourceId());
                        }
                        eventsFragment.startFilterAsyncTask(SPECIAL_FILTER, null, null, -1, null, attribute.getKey(), false, null, -1);
                        reportActionToAnalytics("filters", eventsContext.query + "-" + filterText);
                    }
                });
            }
        } else {
            final String[] categories = EXPLORE_TAGS;
            for (int i = 0; i < categories.length; i++) {
                View view = LayoutInflater.from(this).inflate(R.layout.filter_tags_layout, horizontalCategories, false);
                final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
                filterText.setText(categories[i]);
                horizontalCategories.addView(view);
                filterText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (filterText.isSelected()) {
                            filterText.setSelected(false);
                        } else {
                            filterText.setSelected(true);
                        }
                        if (filterText.getText().toString().equalsIgnoreCase(EventsHighEndpoints.QUERY_FEATURED)) {

                            eventsFragment.startFilterAsyncTask(CATEGORY_FILTER, null, "Featured", -1, null, null, false, null);
                            //eventsFragment.filterEventsWithCategory("Featured", null);
                        } else {
                            eventsFragment.startFilterAsyncTask(CATEGORY_FILTER, null, filterText.getText().toString(), -1, null, null, false, null);
                            //eventsFragment.filterEventsWithCategory(filterText.getText().toString(), null);
                        }
                        reportActionToAnalytics("filters", eventsContext.query + "-" + filterText);
                    }
                });
            }
        }
        LinearLayout horizontalprice = (LinearLayout) findViewById(R.id.price_container);
        horizontalprice.removeAllViews();
        if (isNewYearQuery) {

            if (iterateCount < filters.size()) {
                horizontalprice.setVisibility(View.VISIBLE);
                iterateCount = filters.size();
                for (int i = 6; i < iterateCount; i++) {
                    final BrowseFilterAttributes attribute = filters.get(i);
                    View view = LayoutInflater.from(this).inflate(R.layout.special_filter_tags_layout, horizontalprice, false);
                    final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
                    int lastSpaceIndex = attribute.getName().lastIndexOf(" ");
                    String filterName = attribute.getName().substring(0, lastSpaceIndex) + "\n" + attribute.getName().substring(lastSpaceIndex + 1, attribute.getName().length());
                    filterText.setText(attribute.getName());
                    horizontalprice.addView(view);
                    setCompoundDrawable(filterText, attribute.getIconResourceId());
                    filterText.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            if (filterText.isSelected()) {
                                filterText.setSelected(false);
                                setCompoundDrawable(filterText, attribute.getIconResourceId());
                            } else {
                                filterText.setSelected(true);
                                setCompoundDrawable(filterText, attribute.getSelectedIconResourceId());
                            }
                            eventsFragment.startFilterAsyncTask(SPECIAL_FILTER, null, null, -1, null, attribute.getKey(), false, null, -1);
                            reportActionToAnalytics("filters", eventsContext.query + "-" + filterText);
                        }
                    });
                }
            } else {
                horizontalprice.setVisibility(View.GONE);
            }
        } else {
            String[] priceRanges = {"Free", " \u20B9 ", "\u20B9 \u20B9", "\u20B9 \u20B9 \u20B9", "\u20B9 \u20B9 \u20B9 \u20B9"};
            for (int i = 0; i < priceRanges.length; i++) {
                View view = LayoutInflater.from(this).inflate(R.layout.filter_tags_layout, horizontalprice, false);
                final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
                filterText.setText(priceRanges[i]);
                horizontalprice.addView(view);
                filterText.setTag(i);
                filterText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = (Integer) filterText.getTag();
                        if (position == 0) {
                            eventsFragment.startFilterAsyncTask(PRICE_FILTER, null, null, FREE, null, null, false, null);
                            reportActionToAnalytics("filters", eventsContext.query + "-FREE");
                            // eventsFragment.filterEventsWithPrice(null, FREE);
                        } else if (position == 1) {
                            eventsFragment.startFilterAsyncTask(PRICE_FILTER, null, null, UPTO_250, null, null, false, null);
                            //eventsFragment.filterEventsWithPrice(null, UPTO_250);
                            reportActionToAnalytics("filters", eventsContext.query + "uptp250");
                        } else if (position == 2) {
                            eventsFragment.startFilterAsyncTask(PRICE_FILTER, null, null, PRICE_250_TO_750, null, null, false, null);
                            reportActionToAnalytics("filters", eventsContext.query + "price250to750");
                            //eventsFragment.filterEventsWithPrice(null, PRICE_250_TO_750);
                        } else if (position == 3) {
                            eventsFragment.startFilterAsyncTask(PRICE_FILTER, null, null, PRICE_750_TO_1500, null, null, false, null);
                            //eventsFragment.filterEventsWithPrice(null, PRICE_750_TO_1500);
                            reportActionToAnalytics("filters", eventsContext.query + "price750to1500");
                        } else {
                            eventsFragment.startFilterAsyncTask(PRICE_FILTER, null, null, MORE_THAN_1500, null, null, false, null);
                            reportActionToAnalytics("filters", eventsContext.query + "moreThan1500");
                            //eventsFragment.filterEventsWithPrice(null, MORE_THAN_1500);
                        }

                        if (filterText.isSelected()) {
                            filterText.setSelected(false);
                        } else {
                            filterText.setSelected(true);
                        }
                    }
                });
            }
        }

        LinearLayout zoneContainer = (LinearLayout) findViewById(R.id.zone_container);
        List<String> zones = null;
        if (browseZones != null) {
            zones = browseZones;
        } else {
            zones = getZonesAccordingToCity();
        }

        if (zones != null) {
            for (int i = 0; i < zones.size() + 1; i++) {

                String zone = null;
                if (i != 0) {
                    zone = zones.get(i - 1);
                }
                View view = LayoutInflater.from(this).inflate(R.layout.filter_tags_layout, zoneContainer, false);
                final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
                if (i == 0) {
                    filterText.setText("Zone:");
                } else {
                    filterText.setText(zones.get(i - 1));
                }

                zoneContainer.addView(view);
                filterText.setTag(i);
                final String finalZone = zone;
                filterText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        int position = (Integer) filterText.getTag();
                        if (position != 0) {
                            position = position - 1;
                            eventsFragment.startFilterAsyncTask(ZONE_FILTER, null, null, -1, finalZone, null, false, null);
                            reportActionToAnalytics("filters", eventsContext.query + "-" + finalZone);
                            // eventsFragment.filterEventsWithPrice(null, FREE);

                            if (filterText.isSelected()) {
                                filterText.setSelected(false);
                            } else {
                                filterText.setSelected(true);
                            }
                        }
                    }
                });
            }
        }

        collapseAnimation(-1);
        LinearLayout horizontalDate = (LinearLayout) findViewById(R.id.date_container);
        String[] dateRanges = {"Today", "Tomorrow", "Weekend", "Custom Dates"};
        for (int i = 0; i < dateRanges.length; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.filter_tags_layout, horizontalCategories, false);
            final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
            filterText.setText(dateRanges[i]);
            if (i == dateRanges.length - 1) {
                selectCustomDates = filterText;
                // selectCustomDates.setVisibility(View.GONE);
            } else if (i == dateRanges.length - 2) {
                weekend = filterText;
                //weekend.setVisibility(View.GONE);
            } else if (i == dateRanges.length - 3) {
                tomorrow = filterText;
            } else {
                today = filterText;
                if (isTodaySelected) {
                    today.setSelected(true);
                } else {
                    today.setSelected(false);
                }
            }


            horizontalDate.addView(view);
            filterText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (filterText.getText().toString().equalsIgnoreCase("Today")) {
                        checkIfCustomDateSelected();
                        eventsFragment.startFilterAsyncTask(DATE_FILTER, null, null, -1, null, null, false, null, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime() + 1);
                        reportActionToAnalytics("filters", eventsContext.query + "-Today");

                        //eventsFragment.filterEventsWithDate(null, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime());
                    } else if (filterText.getText().toString().equalsIgnoreCase("Tomorrow")) {
                        checkIfCustomDateSelected();
                        eventsFragment.startFilterAsyncTask(DATE_FILTER, null, null, -1, null, null, false, null, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime() + DateTimeUtils.MILLISECONDS_IN_A_DAY + 1);
                        reportActionToAnalytics("filters", eventsContext.query + "-Tomorrow");
                        // eventsFragment.filterEventsWithDate(null, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime() + DateTimeUtils.MILLISECONDS_IN_A_DAY);
                    } else if (filterText.getText().toString().equalsIgnoreCase("Weekend")) {
                        checkIfCustomDateSelected();
                        eventsFragment.startFilterAsyncTask(DATE_FILTER, null, null, -1, null, null, false, null, DateTimeUtils.getWeekEndDates());
                        reportActionToAnalytics("filters", eventsContext.query + "-Weekend");
                        //eventsFragment.filterEventsWithDate(null, DateTimeUtils.getWeekEndDates());
                    } else if (filterText.getText().toString().equalsIgnoreCase("Custom Dates")) {

                        showDateDialog();
                        Calendar currentYear = Calendar.getInstance();
                        Calendar nextYear = Calendar.getInstance();
                        nextYear.add(Calendar.DAY_OF_MONTH, 45);

                        if (selectedDates != null) {
                            dialogView.init(currentYear.getTime(), nextYear.getTime()).inMode(CalendarPickerView.SelectionMode.MULTIPLE).withSelectedDates(selectedDates);

                        } else {
                            dialogView.init(currentYear.getTime(), nextYear.getTime()) //
                                    .inMode(CalendarPickerView.SelectionMode.MULTIPLE);
                        }
                    }
                    if (!(filterText.getText().toString().equalsIgnoreCase("Custom Dates"))) {
                        if (filterText.isSelected()) {
                            filterText.setSelected(false);
                        } else {
                            filterText.setSelected(true);
                        }
                    }
                }
            });
        }

        trending = (TextView) findViewById(R.id.sort_trending);
        price = (TextView) findViewById(R.id.sort_price);
        distance = (TextView) findViewById(R.id.sort_distance);
        time = (TextView) findViewById(R.id.sort_time);
        if (EventsHighEndpoints.isNearMeQuery(eventsContext.query) && (new Account(this).getLastLocality() != null || getUserLocation() != null)) {
            distance.setSelected(true);
        } else {
            trending.setSelected(true);
        }
        trending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(eventsFragment.sortState == SORT_STATE_TRENDING)) {
                    trending.setSelected(true);
                    price.setSelected(false);
                    distance.setSelected(false);
                    time.setSelected(false);
                    eventsFragment.sortAccToSortState(SORT_STATE_TRENDING);
                    reportActionToAnalytics("filters", eventsContext.query + "-sortTrending");
                }
            }
        });


        price.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(eventsFragment.sortState == SORT_STATE_PRICE)) {
                    trending.setSelected(false);
                    price.setSelected(true);
                    distance.setSelected(false);
                    time.setSelected(false);
                    eventsFragment.sortAccToSortState(SORT_STATE_PRICE);
                    reportActionToAnalytics("filters", eventsContext.query + "-sortPrice");
                }
            }
        });

        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(eventsFragment.sortState == SORT_STATE_TIME)) {
                    trending.setSelected(false);
                    price.setSelected(false);
                    distance.setSelected(false);
                    time.setSelected(true);
                    eventsFragment.sortAccToSortState(SORT_STATE_TIME);
                    reportActionToAnalytics("filters", eventsContext.query + "-sortTime");
                }
            }
        });


        if (account.getLastLocality() != null) {
            distance.setText("Distance from " + Utils.capitalize(account.getLastLocality().getName()));
        }
        distance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (account.getLastLocality() != null) {
                    if (!(eventsFragment.sortState == SORT_STATE_DISTANCE)) {
                        trending.setSelected(false);
                        price.setSelected(false);
                        distance.setSelected(true);
                        time.setSelected(false);
                        eventsFragment.sortAccToSortState(SORT_STATE_DISTANCE);
                        reportActionToAnalytics("filters", eventsContext.query + "-sortDistance");
                    }
                } else {
                    Intent intent = new Intent(EventsGridActivity.this, PlacesAutocompleteBoundedActivity.class);
                    intent.putExtra("show_special_text", true);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                }
            }
        });
        ticketedEventsSwitch = (SwitchCompat) findViewById(R.id.ticketed_events);
        if (isShowLocalitiesFilter) {
            localitiesFilter.setVisibility(View.VISIBLE);
            ticketedEventsSwitch.setVisibility(View.GONE);
            addLocalitiesDataToContainer(selectedLocalities);

        } else {
            localitiesFilter.setVisibility(View.GONE);
            ticketedEventsSwitch.setVisibility(View.VISIBLE);
            ticketedEventsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        eventsFragment.startFilterAsyncTask(TICKETED_FILTER, null, null, -1, null, null, true, null);
                        reportActionToAnalytics("filters", eventsContext.query + "-showTicketedEvents");
                    } else {
                        eventsFragment.startFilterAsyncTask(TICKETED_FILTER, null, null, -1, null, null, false, null);
                        reportActionToAnalytics("filters", eventsContext.query + "-showAllEvents");
                    }
                }
            });
        }
    }

    public void addLocalitiesDataToContainer(final List<String> selectedLocalities) {
        final LinearLayout localitiesContainer = (LinearLayout) findViewById(R.id.localities_container);
        localitiesContainer.removeAllViews();

        if (selectedLocalities != null) {
            for (int i = 0; i < selectedLocalities.size() + 1; i++) {
                final int position = i;

                final View view = LayoutInflater.from(this).inflate(R.layout.locality_filter_tag_layout, localitiesContainer, false);
                LinearLayout parentLayout = (LinearLayout) view.findViewById(R.id.parent_layout);
                ImageView cancelImg = (ImageView) view.findViewById(R.id.cancel);
                final TextView filterText = (TextView) view.findViewById(R.id.locality_name);
                View separator = view.findViewById(R.id.separator);
                if (i == 0) {
                    filterText.setText("Select Locality");
                    filterText.setTextColor(Color.parseColor("#3e3e3e"));
                    cancelImg.setVisibility(View.GONE);
                    parentLayout.setBackground(getDrawable(R.drawable.white_bg));
                    separator.setVisibility(View.GONE);
                } else {
                    filterText.setText(selectedLocalities.get(i - 1));
                    filterText.setTextColor(getResources().getColor(android.R.color.white));
                    parentLayout.setBackground(getDrawable(R.drawable.locality_tag_bg));
                    cancelImg.setVisibility(View.VISIBLE);
                    separator.setVisibility(View.VISIBLE);
                }
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (position == 0) {
                            showLocalitiesList();
                        }
                    }
                });


                cancelImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View image) {

                        if (EventsGridActivity.this.selectedLocalities.contains(selectedLocalities.get(position - 1))) {
                            EventsGridActivity.this.selectedLocalities.remove(selectedLocalities.get(position - 1));
                        }
                        addLocalitiesDataToContainer(EventsGridActivity.this.selectedLocalities);
                        eventsFragment.startFilterAsyncTask(LOCALITY_FILTER, null, null, -1, null, null, false, EventsGridActivity.this.selectedLocalities);
                    }
                });
                localitiesContainer.addView(view);

            }
        } else {
            View view = LayoutInflater.from(this).inflate(R.layout.locality_filter_tag_layout, localitiesContainer, false);
            final TextView filterText = (TextView) view.findViewById(R.id.locality_name);
            view.findViewById(R.id.separator).setVisibility(View.GONE);
            filterText.setText("Select Locality");
            filterText.setTextColor(Color.parseColor("#3e3e3e"));
            view.findViewById(R.id.cancel).setVisibility(View.GONE);
            LinearLayout parentLayout = (LinearLayout) view.findViewById(R.id.parent_layout);
            parentLayout.setBackground(getDrawable(R.drawable.white_bg));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showLocalitiesList();
                }
            });
            localitiesContainer.addView(view);
        }
    }

    List<String> localities;

    public boolean[] getBooleanList(CharSequence[] dialogList) {
        boolean[] isCheckedList = new boolean[dialogList.length];
        if (selectedLocalities != null) {
            for (int i = 0; i < dialogList.length; i++) {

                if (selectedLocalities.contains(dialogList[i].toString())) {
                    isCheckedList[i] = true;
                } else {
                    isCheckedList[i] = false;
                }
            }
        }
        return isCheckedList;
    }

    public void showLocalitiesList() {

        final CharSequence[] dialogList = localities.toArray(new CharSequence[localities.size()]);

        boolean[] is_checked = getBooleanList(dialogList);
        new AlertDialog.Builder(this).setTitle("Select Locality").setMultiChoiceItems(dialogList, is_checked, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean isChecked) {
                if (selectedLocalities != null) {
                    if (selectedLocalities.contains(dialogList[i].toString())) {
                        selectedLocalities.remove(dialogList[i].toString());
                    } else {
                        selectedLocalities.add(dialogList[i].toString());
                    }
                } else {
                    selectedLocalities = new ArrayList<>();
                    selectedLocalities.add(dialogList[i].toString());
                }
            }
        }).setPositiveButton("Apply", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                addLocalitiesDataToContainer(selectedLocalities);
                eventsFragment.startFilterAsyncTask(LOCALITY_FILTER, null, null, -1, null, null, false, selectedLocalities);

            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        }).show();
    }

    public List<String> getZonesAccordingToCity() {
        if (account.getLastCity() == City.BANGALORE) {
            return Arrays.asList(new String[]{"Central Bangalore", "East Bangalore", "North Bangalore", "South Bangalore", "West Bangalore", "Outside " + Utils.capitalize(City.BANGALORE.name())});
        } else if (account.getLastCity() == City.CHENNAI) {
            return Arrays.asList(new String[]{"Central Chennai", "North Chennai", "South Chennai", "Outside " + Utils.capitalize(City.CHENNAI.name())});
        } else if (account.getLastCity() == City.DELHI) {
            return Arrays.asList(new String[]{"Central Delhi", "East Delhi", "Ghaziabad", "Greater Noida", "Gurgaon", "Noida", "South Delhi", "South West Delhi", "West Delhi", "Outside " + Utils.capitalize(City.DELHI.name())});
        } else if (account.getLastCity() == City.MUMBAI) {
            return Arrays.asList(new String[]{"Mumbai Central Suburbs", "Mumbai Harbour", "Mumbai Mira Road & Beyond", "Mumbai South", "Mumbai Western Suburbs", "Outside " + Utils.capitalize(City.MUMBAI.name())});
        } else if (account.getLastCity() == City.HYDERABAD) {
            return Arrays.asList(new String[]{"Central Hyderabad", "East Hyderabad", "North Hyderabad", "South Hyderabad", "West Hyderabad", "Hyderabad Outer"});
        } else if (account.getLastCity() == City.KOLKATA) {
            return Arrays.asList(new String[]{"Kolkata Central", "Kolkata East", "Kolkata North", "Kolkata South"});
        } else if (account.getLastCity() == City.PUNE) {
            return Arrays.asList(new String[]{"Central Pune", "East Pune", "North Pune", "South Pune", "West Pune", "Pune Outer"});
        } else {
            return null;
        }
    }

    TextView trending, price, distance, time;

    SwitchCompat ticketedEventsSwitch;

    public void removeAllSelectedDateFilters() {
        if (today.isSelected()) {
            today.setSelected(false);
        }
        if (tomorrow.isSelected()) {
            tomorrow.setSelected(false);
        }
        if (weekend.isSelected()) {
            weekend.setSelected(false);
        }

        if (eventsFragment.filterEventTimes != null)
            eventsFragment.filterEventTimes.clear();
    }

    public void checkIfCustomDateSelected() {
        if (selectCustomDates.isSelected()) {
            selectCustomDates.setSelected(false);
            eventsFragment.filterEventTimes.clear();
        }
    }

    CalendarPickerView dialogView;

    public void showDateDialog() {
        dialogView = (CalendarPickerView) getLayoutInflater().inflate(R.layout.date_picker_dialog_layout, null, false);
        AlertDialog theDialog = new AlertDialog.Builder(this) //
                .setTitle(null)
                .setView(dialogView)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getSelectedDates();
                        dialog.dismiss();
                    }
                })
                .create();
        theDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {

                dialogView.fixDialogDimens();
            }
        });
        theDialog.show();

    }

    List<Date> selectedDates;

    public void getSelectedDates() {
        selectedDates = dialogView.getSelectedDates();
        if (selectedDates != null && selectedDates.size() > 0) {
            removeAllSelectedDateFilters();
            selectCustomDates.setSelected(true);
            long[] dates = new long[selectedDates.size()];

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < selectedDates.size(); i++) {
                dates[i] = selectedDates.get(i).getTime();
                if (i != 0) {
                    builder.append("/");
                }
                builder.append(DateTimeUtils.getDateFromMillisTime(selectedDates.get(i).getTime()));
            }
            eventsFragment.startFilterAsyncTask(DATE_FILTER, null, null, -1, null, null, false, null, dates);
            reportActionToAnalytics("filters", eventsContext.query + "-" + builder.toString());
            //eventsFragment.filterEventsWithDate(null, dates);
        }
    }

    /*@Override
    protected void onStart() {
        super.onStart();
        String action = getIntent().getAction();
        if (BaseActivity.NOTIFICATION_ACTION.equals(action)) {
            reportActionToAnalytics("openNotification",eventsContext.query);
        }
    }*/

    public void setShareImageUrl(String shareImageUrl) {
        this.shareImageUrl = shareImageUrl;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_event_menu, menu);

        menu.findItem(R.id.action_share);

        menu.findItem(R.id.action_follow).setVisible(showFollowCard);
        // Set visibility.
        // menu.findItem(R.id.action_show_map).setVisible(isPlayServicesPresent);

        return true;
    }

    TextView followText;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem followBtn = menu.findItem(R.id.action_follow);
        LinearLayout followActionLayout = (LinearLayout) followBtn.getActionView();
        followActionLayout.setOnClickListener(followLayoutClickListener);
        followText = (TextView) followActionLayout.findViewById(R.id.follow_text);
        if (account != null)
            changeToolbarFollowBtnText(account.isFollowing(eventsContext.query));
        return super.onPrepareOptionsMenu(menu);
    }


    View.OnClickListener followLayoutClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (account != null) {
                if (account.isFollowing(eventsContext.query)) {
                    reportActionToAnalytics("removeFollowing", eventsContext.query);
                    account.setIsFollowing(eventsContext.query, false);
                    changeToolbarFollowBtnText(false);
                } else {
                    reportActionToAnalytics("addFollowing", eventsContext.query);
                    if (!account.getUserInfo().isSignedIn) {
                        FBSigninDialog.show(EventsGridActivity.this, R.string.ui_signin_via_fb, R.string.ui_signin_fb_plan_more, 1);
                    }
                    account.setIsFollowing(eventsContext.query, true);
                    changeToolbarFollowBtnText(true);
                }
            }
        }
    };

    public void changeToolbarFollowBtnText(boolean isFollowing) {
        followText.setBackgroundResource(isFollowing ? R.drawable.follow_btn_round_corner_filled_bg : R.drawable.follow_btn_round_corner_bg);
        followText.setTextColor(isFollowing ? getResources().getColor(R.color.primary) : getResources().getColor(android.R.color.white));
        followText.setText(isFollowing ? R.string.ui_following : R.string.ui_follow);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            shareEvents(eventsContext);
        }
        return super.onOptionsItemSelected(item);
    }


    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_show_map) {
            showMapActivity();
        }
        return super.onOptionsItemSelected(item);
    }*/

    @Override
    public void showMapActivity() {
        Intent intent = new Intent(this, EventsMapsActivity.class);
        intent.putExtra(EventsMapsActivity.IS_CATEGORY_FILTER_VISIBLE, isCategoryFilterVisible);
        intent.putExtra(EventsMapsActivity.IS_TODAY_SELECTED, isTodaySelected);
        intent.putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, eventsContext);
        startActivity(intent);
    }

    private OnScrollListener doNothingScrollListener = new OnScrollListener() {
        private int y;


        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                y = 0;
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            y += dy;
            if (y > 250) {
                /*if (isCategoryFilterVisible)
                    categoryFilter.setVisibility(View.GONE);
                priceFilter.setVisibility(View.GONE);*/
                //collapseAnimation();

            }

        }
    };

    private OnScrollListener followCardScrollListener = new OnScrollListener() {
        private int y;

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            // do nothings.
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            y += dy;

            // updateToolbar(Math.min(y, 255));
        }
    };

    private int currentToolBarAlpha = 255;

    private void updateToolbar(int toolbarAlpha) {
        if (toolbarAlpha == currentToolBarAlpha) {
            // do nothing
            return;
        }

        // Change the color of toolbar icons and text if needed.
        if (toolbarAlpha < 100) {
            if (currentToolBarAlpha >= 100) {
                setDarkToolbarIcons();
            }
        } else {
            if (currentToolBarAlpha < 100) {
                setLightToolbarIcons();
            }
        }

        currentToolBarAlpha = toolbarAlpha;
        toolbar.setBackgroundColor(Color.argb(toolbarAlpha, 0xCE, 0x4A, 0x46));
        toolbar.setTitleTextColor(Color.argb(toolbarAlpha, 255, 255, 255));
        toolbar.setSubtitleTextColor(Color.argb(toolbarAlpha, 255, 255, 255));
    }

    @Override
    public View getViewForSnackbar() {
        return toolbar;
    }

    private void setDarkToolbarIcons() {
        toolbar.post(new Runnable() {
            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                ToolbarColorizeHelper.colorizeToolbar(toolbar,
                        getResources().getColor(android.R.color.black), EventsGridActivity.this);
            }
        });
    }

    private void setLightToolbarIcons() {
        toolbar.post(new Runnable() {
            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                ToolbarColorizeHelper.colorizeToolbar(toolbar,
                        getResources().getColor(android.R.color.white), EventsGridActivity.this);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (isFabFilterVisible) {
            translateSpecialFilterDown(false);
        } else {
            if (isFromNotification) {
                Intent intent = new Intent(this, LaunchActivity.class);
                startActivity(intent);
            }

            super.onBackPressed();
        }
    }

    public void expandAnimation() {
        int targetHeight;
        if (isCategoryFilterVisible) {
            if (eventsFragment.isMapShown) {
                if (isZoneFilterVisible) {
                    if (isDateFilterVisible) {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 5), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 4), getResources().getDisplayMetrics());
                        }

                    } else {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 4), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 3), getResources().getDisplayMetrics());
                        }

                    }
                } else {
                    if (isDateFilterVisible) {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 4), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 3), getResources().getDisplayMetrics());
                        }
                    } else {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 3), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 2), getResources().getDisplayMetrics());
                        }
                    }
                }
            } else {
                if (isZoneFilterVisible) {
                    if (isDateFilterVisible) {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 6), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 5), getResources().getDisplayMetrics());
                        }
                    } else {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 5), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 4), getResources().getDisplayMetrics());
                        }
                    }
                } else {
                    if (isDateFilterVisible) {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 5), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 4), getResources().getDisplayMetrics());
                        }
                    } else {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 4), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 3), getResources().getDisplayMetrics());
                        }
                    }
                }

            }

        } else {
            if (eventsFragment.isMapShown) {
                if (isZoneFilterVisible) {
                    if (isDateFilterVisible) {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 4), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 3), getResources().getDisplayMetrics());
                        }
                    } else {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 3), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 2), getResources().getDisplayMetrics());
                        }
                    }
                } else {
                    if (isDateFilterVisible) {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 3), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 2), getResources().getDisplayMetrics());
                        }
                    } else {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 2), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 1), getResources().getDisplayMetrics());
                        }
                    }
                }

            } else {
                if (isZoneFilterVisible) {
                    if (isDateFilterVisible) {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 5), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 4), getResources().getDisplayMetrics());
                        }
                    } else {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 4), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 3), getResources().getDisplayMetrics());
                        }
                    }
                } else {
                    if (isDateFilterVisible) {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 4), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 3), getResources().getDisplayMetrics());
                        }
                    } else {
                        if (isSortFilterVisible) {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 3), getResources().getDisplayMetrics());
                        } else {
                            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64 * 2), getResources().getDisplayMetrics());
                        }
                    }
                }

            }

        }

        ResizeAnimation resizeAnimation = new ResizeAnimation(filtersContainer, targetHeight);
        resizeAnimation.setDuration(200);
        resizeAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        resizeAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                isFiltersShown = true;
                updateExpandBtn();

                if (!weekend.isShown())
                    weekend.setVisibility(View.VISIBLE);
                if (!selectCustomDates.isShown()) {
                    selectCustomDates.setVisibility(View.VISIBLE);
                    /*dateFilter.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                    dateFilter.postDelayed(new Runnable() {
                        public void run() {
                            dateFilter.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                        }
                    }, 100L);*/
                }
                //  addDrawable(R.drawable.ic_action_highlight_remove);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        filtersContainer.startAnimation(resizeAnimation);
    }

    public static final int SHOW_SORT = 1;
    public static final int HIDE_SORT = 2;
    public static final int SHOW_MAP_EVENTS_LIST = 3;
    public static final int SHOW_MAP_VIEW = 4;

    public void hideFilterComponents(int hideValue) {
        if (hideValue == SHOW_SORT) {
            findViewById(R.id.sort_separator).setVisibility(View.VISIBLE);
            sortFilter.setVisibility(View.VISIBLE);
        } else if (hideValue == HIDE_SORT) {
            findViewById(R.id.sort_separator).setVisibility(View.GONE);
            sortFilter.setVisibility(View.GONE);
        }

    }

    public void updateExpandBtn() {
        if (isFiltersShown) {

            expandBtn.setImageResource(R.drawable.ic_keyboard_arrow_up_white_24dp);
        } else {

            expandBtn.setImageResource(R.drawable.ic_keyboard_arrow_down_white_24dp);
        }


    }


    public void collapseAnimation(final int hideValue) {

        ResizeAnimation resizeAnimation = new ResizeAnimation(filtersContainer, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (64), getResources().getDisplayMetrics()));
        resizeAnimation.setDuration(200);
        resizeAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        resizeAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                //dateFilter.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                isFiltersShown = false;
                updateExpandBtn();
                addDrawable(-1);

                if (hideValue == SHOW_MAP_EVENTS_LIST) {
                    eventsFragment.bringMapEventsVisible();
                } else if (hideValue == SHOW_MAP_VIEW) {
                    hideFilterComponents(HIDE_SORT);
                    eventsFragment.hideListView();
                } else {
                    hideFilterComponents(hideValue);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        filtersContainer.startAnimation(resizeAnimation);
    }

    public void addDrawable(int resId) {
        Drawable drawableLeft = null;
        if (resId != -1) {
            drawableLeft = getResources().getDrawable(resId);
            drawableLeft.setBounds(0, 0, drawableLeft.getIntrinsicWidth(), drawableLeft.getIntrinsicHeight());

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
                    distance.setText("Distance from " + Utils.capitalize(placeName));
                    makeDistanceSortTrue();
                    LocalityLatLong locality = new LocalityLatLong(placeName, latLng);
                    account.setLastLocality(locality);
                    Log.i("TestActivity", "Place: " + placeName);
                } else {
                    Toast.makeText(this, "Selected locality not found ", Toast.LENGTH_LONG).show();
                }
            } else if (resultCode == RESULT_CANCELED) {

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isFiltersShown) {
                    collapseAnimation(SHOW_MAP_VIEW);
                } else {
                    eventsFragment.hideListView();
                }

                ((FloatingActionButton) fabBrowseMap).setImageResource(R.drawable.ic_list_menu);

            }
        }
    }

    public void makeDistanceSortTrue() {
        distance.setSelected(true);
        price.setSelected(false);
        trending.setSelected(false);
        eventsFragment.sortAccToSortState(SORT_STATE_DISTANCE);
        sortFilter.postDelayed(new Runnable() {
            @Override
            public void run() {
                sortFilter.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }
        }, 100L);

    }

    int filterHeight = 2500;


    public void translateSpecialFilterDown(final boolean isRefreshData) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(specialFilterLayout, View.TRANSLATION_Y, 0, filterHeight);
        anim.setDuration(500);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isFabFilterVisible = false;
                if (specialFilters != null && isRefreshData) {
                    //eventsFragment.startFilterAsyncTask(SPECIAL_FILTER, null, null, -1, null, specialFilters, false, -1);
                    // Toast.makeText(EventsGridActivity.this, specialFilters.size() + " filters selected", Toast.LENGTH_SHORT).show();

                }
                showFab(fabSpecialFilter);
                showFab(fabBrowseMap);

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        anim.start();
    }

    public void translateSpecialFilterUp() {

        ObjectAnimator anim = ObjectAnimator.ofFloat(specialFilterLayout, View.TRANSLATION_Y, filterHeight, 0);
        anim.setDuration(500);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                specialFilterLayout.setVisibility(View.VISIBLE);

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isFabFilterVisible = true;
                hideFab(fabBrowseMap);
                hideFab(fabSpecialFilter);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        anim.start();
    }

    //Hide fab
    public void hideFab(final View view) {
        Animation scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down);
        scaleDown.setDuration(200);
        scaleDown.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(scaleDown);
    }

    //Show Fab
    public void showFab(final View view) {
        Animation scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        scaleDown.setDuration(200);
        scaleDown.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(scaleDown);
    }


}
