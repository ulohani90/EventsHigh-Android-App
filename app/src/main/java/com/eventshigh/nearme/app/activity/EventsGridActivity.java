package com.eventshigh.nearme.app.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.Locality;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.ui.animation.ResizeAnimation;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.squareup.timessquare.CalendarPickerView;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import pl.snowdog.material.ui.ToolbarColorizeHelper;

/**
 * Shows the events in Grid layout.
 */
public class EventsGridActivity extends BaseContextActivity {

    private boolean showFollowCard;
    private View fabShare;

    String shareImageUrl;

    private boolean isFromNotification;

    EventsFragment eventsFragment;

    ThisWeekFragment thisWeekFragment;

    HorizontalScrollView categoryFilter, priceFilter, dateFilter;

    boolean isCategoryFilterVisible;
    Account account;


    public static final int FREE = 1;
    public static final int UPTO_250 = 2;
    public static final int PRICE_250_TO_750 = 3;
    public static final int MORE_THAN_750 = 4;

    LinearLayout filtersContainer;

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
        filtersContainer = (LinearLayout) findViewById(R.id.filters_container);
        categoryFilter = (HorizontalScrollView) findViewById(R.id.category_filter);
        priceFilter = (HorizontalScrollView) findViewById(R.id.price_filter);
        dateFilter = (HorizontalScrollView) findViewById(R.id.date_filter);

        // Show query as title.
        if (!eventsContext.query.isEmpty()) {
            setTitle();
        }

        // Fab Share.
        fabShare = findViewById(R.id.fab_share);
        fabShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //shareEvents(eventsContext);
                shareEventsWithBranch(eventsContext, shareImageUrl);
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

            if (Locality.getLocality(eventsContext.query) == null) {
                isCategoryFilterVisible = false;
                findViewById(R.id.category_filter).setVisibility(View.GONE);
            } else {
                isCategoryFilterVisible = true;
                findViewById(R.id.category_filter).setVisibility(View.VISIBLE);
            }

            boolean showEhInviteForNotification = getIntent() != null &&
                    getIntent().getAction() != null && getIntent().getAction().startsWith(NOTIFICATION_ACTION);
            EventsFragment eventFragment1 = EventsFragment.getInstance(
                    eventsContext, showFollowCard, false, showEhInviteForNotification, (SocialInvitationsRequest.SpecialCoupons) getIntent().getParcelableExtra("special_obj"));
            eventFragment1.setOnScrollListener(
                    false ? followCardScrollListener : doNothingScrollListener);

            this.eventsFragment = eventFragment1;
            FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
            tr.replace(R.id.event_container, eventsFragment);
            tr.commit();
        } else {
            thisWeekFragment = ThisWeekFragment.getInstance(eventsContext, false, 14);
            FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
            tr.replace(R.id.event_container, thisWeekFragment);
            tr.commit();
        }

        addFiltersData();

    }

    TextView followBtn;

    public void addToolbarView() {
        if (toolbar != null) {
            View view = LayoutInflater.from(this).inflate(R.layout.event_grid_toolbar_layout, toolbar, false);
            TextView title = (TextView) view.findViewById(R.id.title);
            title.setText(eventsContext.query);
            followBtn = (TextView) view.findViewById(R.id.follow_btn);
            followBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (followBtn.isSelected()) {
                        unSelectFollowBtn();
                    } else {
                        selectFollowBtn();
                    }
                }
            });
            if (account.isFollowing(eventsContext.query)) {
                changeFollowBtnState(true);
            } else {
                changeFollowBtnState(false);
            }
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
        if (!account.getUserInfo().isVerified) {
            PhoneVerificationDialog.show(this,
                    R.string.ui_verify_phone, R.string.ui_phone_verify_pa);
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

    TextView showMoreFilterText;

    TextView selectCustomDates, today, tomorrow, weekend;

    public void addFiltersData() {
        isFiltersShown = true;
        final LinearLayout horizontalCategories = (LinearLayout) findViewById(R.id.category_container);
        final EventCategory[] categories = EventCategory.values();
        for (int i = 0; i < categories.length; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.filter_tags_layout, horizontalCategories, false);
            final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
            filterText.setText(categories[i].categoryName);
            horizontalCategories.addView(view);
            filterText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (filterText.isSelected()) {
                        filterText.setSelected(false);
                    } else {
                        filterText.setSelected(true);
                    }

                    eventsFragment.filterEventsWithCategory(filterText.getText().toString(), null);
                }
            });
        }
        LinearLayout horizontalprice = (LinearLayout) findViewById(R.id.price_container);
        String[] priceRanges = {"Free", " \u20B9 ", "\u20B9 \u20B9", "\u20B9 \u20B9 \u20B9"};
        for (int i = 0; i < priceRanges.length; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.filter_tags_layout, horizontalCategories, false);
            final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
            filterText.setText(priceRanges[i]);
            horizontalprice.addView(view);
            filterText.setTag(i);
            filterText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (Integer) filterText.getTag();
                    if (position == 0) {
                        eventsFragment.filterEventsWithPrice(null, FREE);
                    } else if (position == 1) {
                        eventsFragment.filterEventsWithPrice(null, UPTO_250);
                    } else if (position == 2) {
                        eventsFragment.filterEventsWithPrice(null, PRICE_250_TO_750);
                    } else {
                        eventsFragment.filterEventsWithPrice(null, MORE_THAN_750);
                    }

                    if (filterText.isSelected()) {
                        filterText.setSelected(false);
                    } else {
                        filterText.setSelected(true);
                    }
                }
            });
        }
        collapseAnimation();
        LinearLayout horizontalDate = (LinearLayout) findViewById(R.id.date_container);
        String[] dateRanges = {"Today", "Tomorrow", "Weekend", "Dates", "\u2022 • •"};
        for (int i = 0; i < dateRanges.length; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.filter_tags_layout, horizontalCategories, false);
            final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
            filterText.setText(dateRanges[i]);
            if (i == dateRanges.length - 1) {
                showFiltersView = view;
                showMoreFilterText = filterText;
                showMoreFilterText.setTypeface(null, Typeface.BOLD);
            } else if (i == dateRanges.length - 2) {
                selectCustomDates = filterText;
                selectCustomDates.setVisibility(View.GONE);
            } else if (i == dateRanges.length - 3) {
                weekend = filterText;
                weekend.setVisibility(View.GONE);
            } else if (i == dateRanges.length - 4) {
                tomorrow = filterText;
            } else {
                today = filterText;
            }


            horizontalDate.addView(view);
            filterText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (filterText.getText().toString().equalsIgnoreCase("Today")) {
                        checkIfCustomDateSelected();
                        eventsFragment.filterEventsWithDate(null, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime());
                    } else if (filterText.getText().toString().equalsIgnoreCase("Tomorrow")) {
                        checkIfCustomDateSelected();
                        eventsFragment.filterEventsWithDate(null, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime() + DateTimeUtils.MILLISECONDS_IN_A_DAY);
                    } else if (filterText.getText().toString().equalsIgnoreCase("Weekend")) {
                        checkIfCustomDateSelected();
                        eventsFragment.filterEventsWithDate(null, DateTimeUtils.getWeekEndDates());
                    } else if (filterText.getText().toString().equalsIgnoreCase("Dates")) {

                        showDateDialog();
                        Calendar currentYear = Calendar.getInstance();
                        Calendar nextYear = Calendar.getInstance();
                        nextYear.add(Calendar.DAY_OF_MONTH, 45);

                        if (selectedDates != null) {
                            dialogView.init(currentYear.getTime(), nextYear.getTime()).withSelectedDates(selectedDates) //
                                    .inMode(CalendarPickerView.SelectionMode.MULTIPLE);
                        } else {
                            dialogView.init(currentYear.getTime(), nextYear.getTime()) //
                                    .inMode(CalendarPickerView.SelectionMode.MULTIPLE);
                        }
                    } else if (filterText.getText().toString().equalsIgnoreCase("• • •")) {
                        expandAnimation();
                        return;
                    } else {
                        collapseAnimation();
                        return;
                    }
                    if (!(filterText.getText().toString().equalsIgnoreCase("Dates"))) {
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
            for (int i = 0; i < selectedDates.size(); i++) {
                dates[i] = selectedDates.get(i).getTime();
            }
            eventsFragment.filterEventsWithDate(null, dates);
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
        getMenuInflater().inflate(R.menu.activity_event, menu);

        // Set visibility.
        menu.findItem(R.id.action_show_map).setVisible(isPlayServicesPresent);

        if (showFollowCard || EventsHighEndpoints.isDateQuery(eventsContext.query)) {
            fabShare.setVisibility(View.VISIBLE);
        }

        return true;
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
        if (isFromNotification) {
            Intent intent = new Intent(this, LaunchActivity.class);
            startActivity(intent);
        }
        super.onBackPressed();
    }

    public void expandAnimation() {
        int targetHeight;
        if (isCategoryFilterVisible) {
            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (56 * 3), getResources().getDisplayMetrics());
        } else {
            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (56 * 2), getResources().getDisplayMetrics());
        }

        ResizeAnimation resizeAnimation = new ResizeAnimation(filtersContainer, targetHeight);
        resizeAnimation.setDuration(100);
        resizeAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                showMoreFilterText.setText("");
                if (!weekend.isShown())
                    weekend.setVisibility(View.VISIBLE);
                if (!selectCustomDates.isShown()) {
                    selectCustomDates.setVisibility(View.VISIBLE);
                    dateFilter.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                    dateFilter.postDelayed(new Runnable() {
                        public void run() {
                            dateFilter.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                        }
                    }, 100L);
                }
                addDrawable(R.drawable.ic_action_highlight_remove);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        filtersContainer.startAnimation(resizeAnimation);
    }

    public void collapseAnimation() {
        ResizeAnimation resizeAnimation = new ResizeAnimation(filtersContainer, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (56), getResources().getDisplayMetrics()));
        resizeAnimation.setDuration(100);
        resizeAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                addDrawable(-1);
                showMoreFilterText.setText("• • •");

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
        showMoreFilterText.setCompoundDrawables(drawableLeft, null, null, null);
    }
}
