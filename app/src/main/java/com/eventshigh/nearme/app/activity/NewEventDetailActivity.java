package com.eventshigh.nearme.app.activity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventDescriptionSection;
import com.eventshigh.nearme.app.data.EventInfoObject;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.MovieUserReviewObject;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.network.EventRequest;
import com.eventshigh.nearme.app.network.MyReviewsRequest;
import com.eventshigh.nearme.app.network.SocialActionsRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.FBSigninDialog;
import com.eventshigh.nearme.app.ui.InviteFriendsDialog;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.ui.RateAppDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.user.UserActionHelper;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.ContactListView;
import com.eventshigh.nearme.app.view.SmartViewPager;
import com.google.ads.conversiontracking.AdWordsRemarketingReporter;
import com.google.gson.internal.LinkedTreeMap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

/**
 * Created by umesh on 17/06/16.
 */
public class NewEventDetailActivity extends BaseContextActivity {


    ViewPager imagePager, viewPager;
    Account account;

    Event event = null;

    private View topProgressBar;

    //Tells whether the user has written the review or not
    boolean isMyReviewWritten;

    public static final String INFO_TAB = "Info";

    public static final String FAQS_TAB = "FAQS";

    String planId;

    LinearLayout statsLayout;

    public static final String EVENT_OBJECT = "event_detail_object";


    public static final String EXTRA_EVENT_PARAM = NewEventDetailActivity.class.getSimpleName() + "_event";
    public static final String EXTRA_PLAN_ID_PARAM = NewEventDetailActivity.class.getSimpleName() + "_plan_id";

    public static final String EVENT_REVIEWS = "event_reviews";
    public static final String EVENT_ID = "event_id";

    LinearLayout dotsView;
    TextView bookView;
    View joinView;

    View configLayout, configParentLayout;

    public static final int REQUEST_FOR_RESULT_BOOK_TICKETS = 0x009;
    public static final int REQUEST_FOR_RESULT_AMA = 0x010;
    public static final int REQUEST_FOR_RESULT_CALL_EVENT = 0x011;
    public static final int REQUEST_FOR_RESULT_WRITE_REVIEW = 0x012;

    boolean isDestroyed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_event_detail_activity);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setUpToolBar(toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        imagePager = (ViewPager) findViewById(R.id.image_pager);
        viewPager = (ViewPager) findViewById(R.id.view_pager);
        dotsView = (LinearLayout) findViewById(R.id.dots_parent);
        account = new Account(this);
        topProgressBar = findViewById(R.id.top_progress_bar);
        bookView = (TextView) findViewById(R.id.book_ticket);
        joinView = findViewById(R.id.join_event);
        statsLayout = (LinearLayout) findViewById(R.id.stats_layout);
        configLayout = findViewById(R.id.event_highlights_layout);
        configParentLayout = findViewById(R.id.event_highlights_parent_layout);
        configParentLayout.setY(MOVE_VIEW_TO_POS);


    }

    ImageView favAction, shareAction;

    TextView title;

    public void setUpToolBar(Toolbar toolbar) {
        View view = LayoutInflater.from(this).inflate(R.layout.event_detail_toolbar_layout, toolbar, false);
        title = (TextView) view.findViewById(R.id.title);
        favAction = (ImageView) view.findViewById(R.id.fav_action);
        shareAction = (ImageView) view.findViewById(R.id.share_action);
        shareAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (event != null) {
                    showRateAppDialog = true;
                    shareEvent(event, null, "Toolbar");
                }
            }
        });

        favAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (event != null) {
                    if (account.getUserInfo().phoneNo == null) {
                        //FBSigninDialog.show(this, R.string.ui_signin_via_fb, R.string.ui_signin_fb_plan_more, 1);
                        PhoneVerificationDialog.show(NewEventDetailActivity.this, R.string.ui_verify_phone, R.string.ui_phone_verify_plan);
                    } else {
                        if (favAction.isSelected()) {
                            removeFavourite(v);
                            favAction.setSelected(false);
                            showMessage("Removed from favourites");
                        } else {
                            addFavourite(v);
                            favAction.setSelected(true);
                            showMessage("Added to favourites");

                        }
                    }
                }
            }
        });
        toolbar.addView(view);
    }

    protected void onStart() {
        super.onStart();

        if (!isDataAttached) {
            String action = getIntent().getAction();
            if (action != null) {
                if (BaseActivity.NOTIFICATION_ACTION.equals(action)) {
                    reportActionToAnalytics("openNotification", getIntent().getData().getLastPathSegment());

                } else if (action.equalsIgnoreCase(Intent.ACTION_VIEW)) {
                    reportActionToAnalytics("deeplink", getIntent().getDataString());
                }
            }

        /*findViewById(R.id.event_container).setMinimumHeight(
                (int) (1.33 * getResources().getDisplayMetrics().heightPixels));*/
// Get the event from Intent.


            if (getIntent().hasExtra(EXTRA_EVENT_PARAM)) {
                event = getIntent().getParcelableExtra(EXTRA_EVENT_PARAM);
                if (event != null) {
                    makeMyReviewsServerRequest(false);
                } else {
                    Toast.makeText(NewEventDetailActivity.this, R.string.failed_load,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else if (getIntent().getData() != null) {

                EventRequest.submit(this, getIntent().getData(), Request.Priority.IMMEDIATE, mEventListener,
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                                if (!isDestroyed) {
                                    Toast.makeText(NewEventDetailActivity.this, R.string.failed_load,
                                            Toast.LENGTH_SHORT).show();
                                    VolleyHelper.log(NewEventDetailActivity.this, volleyError);
                                    finish();
                                }
                            }
                        });
            }
        }
    }

    boolean showRateAppDialog, showInviteDialog, addToFavourite;

    @Override
    protected void onResume() {
        super.onResume();
        if (showRateAppDialog) {
            RateAppDialog.show(this);
            showRateAppDialog = false;
        } else if (showInviteDialog) {
            InviteFriendsDialog.show(this, event, planId);
            showInviteDialog = false;
        }

        if (addToFavourite && event != null && !isFavourite(event)) {
            addFavourite(null);
        }
        addToFavourite = false;
        if (Preferences.getInstance(this).isReviewAdded()) {
            makeMyReviewsServerRequest(true);
        }
    }

    public void addFavourite(View v) {
        if (v != null) {
            reportEventAction(event, "addFavourite");
        }

        EventsMarkerManager.Editor eventsMarkerEditor =
                EventsMarkerManager.getInstance(this).getEditor();
        eventsMarkerEditor.recordEventMark(event, EventsMarkerManager.EventMark.FAVOURITE, false);
        //mark view as favourite
        eventsMarkerEditor.close();
    }


    public void openSourceSite(View view) {
        showInviteDialog = true;
        reportEventAction(event, "organizer", view.getId() == R.id.join_event ? "joinEvent" : "openSource");

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.sourceUrl));
        startActivitySafe(intent);
    }

    public void startActivitySafe(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open url. ignore.
            Crashlytics.getInstance().core.logException(e);
        }
    }

    private Response.Listener<Event> mEventListener = new Response.Listener<Event>() {
        @Override
        public void onResponse(final Event event, boolean isIntermediate) {
            if (!isDestroyed) {
                if (event != null) {
                    NewEventDetailActivity.this.event = event;
                    makeMyReviewsServerRequest(false);
                } else {
                    Toast.makeText(NewEventDetailActivity.this, R.string.failed_load,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            // populateView(event);

        }
    };

    public void makeMyReviewsServerRequest(boolean shouldByPassCache) {
        MyReviewsRequest.submit(this, account.getUserInfo().email, Request.Priority.IMMEDIATE, this, shouldByPassCache, mReviewListener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if (!isDestroyed) {
                    Toast.makeText(NewEventDetailActivity.this, R.string.failed_load,
                            Toast.LENGTH_SHORT).show();
                    VolleyHelper.log(NewEventDetailActivity.this, volleyError);
                    finish();
                }
            }
        });
    }

    private Response.Listener<List<MovieUserReviewObject>> mReviewListener = new Response.Listener<List<MovieUserReviewObject>>() {
        @Override
        public void onResponse(List<MovieUserReviewObject> reviews, boolean isIntermediate) {
            if (!isDestroyed && event != null) {
                findReviewsByUserForMovie(reviews);
                //if (fragment == null)
                addAdapterData();
            }
          /*  if (fragment != null)
                fragment.updateReview(event);*/
        }
    };


    public void findReviewsByUserForMovie(List<MovieUserReviewObject> reviews) {
        for (MovieUserReviewObject obj : reviews) {
            if (obj.getReviewerId().equalsIgnoreCase(account.getUserInfo().email)
                    && obj.getReviewedEntityId().equalsIgnoreCase(event.id + "")) {
                event.reviewObjects.add(0, obj);
                isMyReviewWritten = true;
                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    ArrayList<String> TABS;

    TextView statsView;

    boolean isDataAttached;

    @SuppressWarnings("ResourceType")

    public void addAdapterData() {
        if (isFavourite(event)) {
            favAction.setSelected(true);
        } else {
            favAction.setSelected(false);
        }
        isDataAttached = true;
        title.setText(event.title);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("dynx_itemid", event.id);
        params.put("dynx_pagetype", "offerdetail");


        AdWordsRemarketingReporter.reportWithConversionId(
                getApplicationContext(),
                Utils.ADWORDS_CONVERSION_ID,
                params);

        /*final TextView eventPrice = (TextView) findViewById(R.id.event_price);
        String priceString = event.getPriceString();
        if (priceString == null) {
            eventPrice.setVisibility(View.GONE);
        } else {
            eventPrice.setVisibility(View.VISIBLE);
            eventPrice.setText(priceString);
        }
        TextView eventDiscount = (TextView) findViewById(R.id.event_discount);
        if (event.discountPercentageText != null) {
            eventDiscount.setVisibility(View.VISIBLE);
            eventDiscount.setText(event.discountPercentageText);
        } else if (event.discountPercentage != null) {
            eventDiscount.setVisibility(View.VISIBLE);
            eventDiscount.setText(event.discountPercentage + "% OFF");
        } else {
            eventDiscount.setVisibility(View.GONE);
        }*/

        viewPager.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                // How far the user has to scroll before it locks the parent vertical scrolling.
                final int margin = 10;
                final int fragmentOffset = v.getScrollX() % v.getWidth();

                if (fragmentOffset > margin && fragmentOffset < v.getWidth() - margin) {
                    viewPager.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
        });

        topProgressBar.setVisibility(View.GONE);
        // callView.setVisibility(event.organizerPhone != null ? View.VISIBLE : View.GONE);
        findViewById(R.id.share_whatsapp_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.share_whatsapp_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareEvent(event, PACKAGE_NAME_WHATSAPP, " bottombar");
            }
        });

        statsView = (TextView) findViewById(R.id.event_stats);
       /* if (event.numViews > 5) {
            int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
            ((ImageView) findViewById(R.id.img1)).setImageDrawable(UserContact.getDrawableForName(String.valueOf(getRandomCharacter()), size));
            ((ImageView) findViewById(R.id.img2)).setImageDrawable(UserContact.getDrawableForName(String.valueOf(getRandomCharacter()), size));
            ((View) statsView.getParent()).setVisibility(View.VISIBLE);
            statsView.setVisibility(View.VISIBLE);
        } else {
            ((ImageView) findViewById(R.id.img1)).setVisibility(View.GONE);
            ((ImageView) findViewById(R.id.img2)).setVisibility(View.GONE);
            ((View) statsView.getParent()).setVisibility(View.GONE);
            statsView.setVisibility(View.GONE);
        }

        statsView.setText(" and " + event.numViews + " more viewed this");
*/

        if (event.bookingUrl != null && event.bookingUrl.toLowerCase().contains("bookmyshow")) {
            bookView.setVisibility(View.GONE);
            joinView.setVisibility(View.GONE);
        } else {

            bookView.setVisibility(event.bookingUrl != null && event.bookingUrl.length() > 0 ? View.VISIBLE : View.GONE);
            if (event.bookingText != null) {
                bookView.setText(event.bookingText);
            }

            if (bookView.isShown()) {
                bookView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openBookingSite();
                    }
                });
            }


            joinView.setVisibility(
                    (bookView.getVisibility() != View.VISIBLE && event.sourceUrl != null &&
                            event.sourceUrl.contains("facebook.com/"))
                            ? View.VISIBLE : View.GONE);

            if (joinView.isShown()) {
                joinView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openSourceSite(v);
                    }
                });
            }

        }
        EventImagePagerAdapter imagePagerAdapter = new EventImagePagerAdapter();
        imagePager.setAdapter(imagePagerAdapter);
        dotsView.removeAllViews();
        if (imagePagerAdapter.getCount() > 1) {
            dotsView.setVisibility(View.VISIBLE);
            for (int i = 0; i < imagePagerAdapter.getCount(); i++) {
                View view = getLayoutInflater().inflate(
                        R.layout.view_dot_featured, dotsView, false);
                view.setSelected(i == 0);
                dotsView.addView(view);
            }
        } else {
            dotsView.setVisibility(View.GONE);
        }

        imagePager.clearOnPageChangeListeners();
        imagePager.addOnPageChangeListener(new DotsSelector(this));

        TABS = new ArrayList<>();

        TABS.add(INFO_TAB);

        if (event.descriptionSections != null && event.descriptionSections.size() > 0) {
            for (EventDescriptionSection section : event.descriptionSections) {
                TABS.add(section.name);
            }
        }
        if (event.sessions != null && event.sessions.size() > 0) {
            TABS.add(event.sessionTitlePhrase != null ? event.sessionTitlePhrase : "Sessions");
        }

        if (event.faqs != null && event.faqs.size() > 0) {
            TABS.add(FAQS_TAB);
        }


        EventPagerAdapter adapter = new EventPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);

        TabLayout tabsView = (TabLayout) findViewById(R.id.tabs);
        if (TABS.size() > 1) {
            tabsView.setVisibility(View.VISIBLE);
            tabsView.setTabGravity(TabLayout.GRAVITY_FILL);
            tabsView.setTabMode(TabLayout.MODE_SCROLLABLE);
            tabsView.setupWithViewPager(viewPager);
            tabsView.setScrollPosition(0, 0, true);
        } else {
            tabsView.setVisibility(View.GONE);
        }
// Show social data.
        SocialActionsRequest.submit(this, Request.Priority.LOW, this, false,
                new Response.Listener<SocialActionsRequest.SocialActions>() {
                    @Override
                    public void onResponse(SocialActionsRequest.SocialActions socialActions, boolean isIntermediate) {
                        if (event != null) {
                            Set<SocialFriend> likedBy = socialActions.eventFavourites.get(event.id);
                            reportActionToAnalytics("showSocialInfo", "likes",
                                    likedBy == null ? 0 : likedBy.size());
                            if (likedBy != null && likedBy.size() > 0) {
                                statsLayout.setVisibility(View.VISIBLE);
                                ((ImageView) findViewById(R.id.img1)).setVisibility(View.GONE);
                                ((ImageView) findViewById(R.id.img2)).setVisibility(View.GONE);
                                ((ContactListView) findViewById(R.id.followed_by)).setVisibility(View.VISIBLE);
                                ((ContactListView) findViewById(R.id.followed_by)).setFollowers(
                                        NewEventDetailActivity.this, likedBy);
                                StringBuilder builder = new StringBuilder();
                                int pos = 0;
                                for (SocialFriend socialFriend : likedBy) {
                                    if (pos > 0) {
                                        break;
                                    }
                                    builder.append(socialFriend.getName());
                                    pos++;
                                }
                                if (builder.length() > 0 && event.numViews > 0) {
                                    builder.append(" and ");
                                }
                                builder.append(event.numViews > 0 ? (event.numViews + " people interested") : " interested");
                                statsView.setText(builder.toString());
                                ((View) statsView.getParent()).setVisibility(View.VISIBLE);
                                statsView.setVisibility(View.VISIBLE);
                            } else {
                                if (event.numViews > 0) {
                                    statsLayout.setVisibility(View.VISIBLE);
                                    ((ImageView) findViewById(R.id.img1)).setVisibility(View.VISIBLE);
                                    ((ImageView) findViewById(R.id.img2)).setVisibility(View.VISIBLE);
                                    int resource1 = -1;
                                    int resource2 = -1;
                                    if (getIntent().getExtras() != null) {
                                        resource1 = getIntent().getExtras().getInt("resource_1", -1);
                                        resource2 = getIntent().getExtras().getInt("resource_2", -1);
                                    }
                                    // ((ImageView) findViewById(R.id.img1)).setImageDrawable(getResources().getDrawable());

                                    ((ImageView) findViewById(R.id.img1)).setImageResource((resource1 != -1 ? resource1 : Utils.getDummyImageResource()));
                                    ((ImageView) findViewById(R.id.img2)).setImageResource(resource2 != -1 ? resource2 : Utils.getDummyImageResource());
                                    StringBuilder builder = new StringBuilder();
                                    builder.append(event.numViews + " people interested");
                                    statsView.setText(builder.toString());
                                    ((View) statsView.getParent()).setVisibility(View.VISIBLE);
                                    statsView.setVisibility(View.VISIBLE);
                                } else {
                                    statsLayout.setVisibility(View.GONE);
                                }
                            }

                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        VolleyHelper.log(NewEventDetailActivity.this, volleyError);
                    }
                }
        );
    }


    public char getRandomCharacter() {
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final int N = alphabet.length();
        Random r = new Random();
        for (int i = 0; i < 50; i++) {
            return alphabet.charAt(r.nextInt(N));
        }
        return 'P';
    }


    public void openBookingSite() {
        Account account = new Account(this);
        Account.UserInfo userInfo = account.getUserInfo();
        /*if (!userInfo.isSignedIn) {
            // FBSigninDialog.show(this, R.string.ui_signin_via_fb, R.string.ui_signin_fb_plan, REQUEST_FOR_RESULT_BOOK_TICKETS);
            Intent intent = new Intent(this, FBLoginActivity.class);
            intent.putExtra("show_special_text", true);
            intent.putExtra("hide_skip", true);
            startActivityForResult(intent, REQUEST_FOR_RESULT_BOOK_TICKETS);
            return;
        }*/

        showRateAppDialog = true;
        reportEventAction(event, "bookTicket");
        new UserActionHelper(this).recordAction(UserActionHelper.EventAction.BOOK, event.id);

        final Uri.Builder bookingUriBuilder = Uri.parse(event.bookingUrl).buildUpon();

        if (event.bookingUrl.equalsIgnoreCase("https://ticketing.eventshigh.com/checkout3.jsp?eid=" + event.id) && !isEventInNext24Hrs() && isNoAdditionalField() && (event.ticketingEnabledStatus != 0)) {
            /*
            bookingUriBuilder.appendQueryParameter("did", Utils.getAndroidId(this));
            bookingUriBuilder.appendQueryParameter("name", userInfo.name);
            bookingUriBuilder.appendQueryParameter("mobile", userInfo.phoneNo);
            bookingUriBuilder.appendQueryParameter("src", "eh-android");*/

        } else {
            if (event.bookingUrl.contains("ticketing.eventshigh.com")) {
                try {
                    bookingUriBuilder.appendQueryParameter("src", "eh-android");
                    if (userInfo != null) {
                        if (userInfo.name != null && userInfo.name.length() > 0) {
                            bookingUriBuilder.appendQueryParameter("name", userInfo.name);
                        }
                        if (userInfo.email != null && userInfo.email.length() > 0) {
                            bookingUriBuilder.appendQueryParameter("email", userInfo.email);
                        }
                        if (userInfo.phoneNo != null && userInfo.phoneNo.length() > 0) {
                            bookingUriBuilder.appendQueryParameter("mobile", userInfo.phoneNo);
                        }
                    }
                    CustomUrlActivity.launchCustomUrl(this, bookingUriBuilder.build(),
                            getString(R.string.title_book));
                } catch (Exception e) {
                    Crashlytics.getInstance().core.logException(e);
                    showMessage(R.string.retry);
                }
            } else {
                try {
                    URL url = new URL(event.bookingUrl);
                    String host = url.getHost();
                    SpannableString messageText = new SpannableString("You are being redirected to " + host + ". Please click confirm to proceed further.");
                    messageText.setSpan(new StyleSpan(Typeface.BOLD), 28, (28 + host.length()), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                    new AlertDialog.Builder(NewEventDetailActivity.this).setMessage(messageText).setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            bookingUriBuilder.appendQueryParameter("src", "eh-android");
                            CustomUrlActivity.launchCustomUrl(NewEventDetailActivity.this, bookingUriBuilder.build(),
                                    getString(R.string.title_book));
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
                } catch (MalformedURLException e) {


                }
            }
        }

    }

    public boolean isNoAdditionalField() {
        if ((event.additionalTicketFieldList == null) || (event.additionalTicketFieldList != null && event.additionalTicketFieldList.size() == 0)) {
            return true;
        }
        return false;
    }

    public boolean isEventInNext24Hrs() {
        if (event.eventTimings != null && event.eventTimings.size() > 0) {
            if ((event.eventTimings.get(0) - System.currentTimeMillis()) < 86400000) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }

    }

    EventInfoFragment fragment;

    public class EventPagerAdapter extends FragmentPagerAdapter {

        private int mCurrentPosition = -1;

        public EventPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            if (position == 0) {

                bundle.putParcelable("event_info_object", new EventInfoObject(event));
                // System.out.println("Bundle size " + bundle.size());
                fragment = EventInfoFragment.newInstance(bundle);
                return fragment;
            }

            if (event.faqs != null && event.faqs.size() > 0) {
                if (position == TABS.size() - 1) {
                    bundle.putParcelableArrayList("faqs", event.faqs);
                    return EventFaqsSection.newInstance(bundle);
                } else {
                    if (event.sessions != null && event.sessions.size() > 0 && position == TABS.size() - 1) {
                        return EventSessionDetailFragment.newInstance((ArrayList) event.sessions, event.city);
                    }
                    bundle.putString("description", event.descriptionSections.get(position - 1).description);
                    return EventDetailCustomFragment.newInstance(bundle);
                }
            } else {
                if (event.sessions != null && event.sessions.size() > 0 && position == TABS.size() - 2) {
                    return EventSessionDetailFragment.newInstance((ArrayList) event.sessions, event.city);
                }
                bundle.putString("description", event.descriptionSections.get(position - 1).description);
                return EventDetailCustomFragment.newInstance(bundle);
            }

        }

        @Override
        public int getCount() {
            return TABS.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TABS.get(position);
        }

        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            if (position != mCurrentPosition) {
                Fragment fragment = (Fragment) object;
                SmartViewPager pager = (SmartViewPager) container;
                if (fragment != null && fragment.getView() != null) {
                    mCurrentPosition = position;
                    pager.measureCurrentView(fragment.getView());
                }
            }
        }
    }

    public class EventImagePagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            if (event.allImages != null) {
                return event.allImages.size();
            }
            return 0;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.card_refer, container, false);
            TextView helloUserText = (TextView) view.findViewById(R.id.hello_user_text);
            helloUserText.setVisibility(View.GONE);
            ImageView banner = (ImageView) view.findViewById(R.id.banner_img);
            Glide.with(NewEventDetailActivity.this).load(event.allImages.get(position)).placeholder(R.drawable.eh_default_event)
                    .crossFade().centerCrop()
                    .into(banner);
            view.setTag(event.allImages.get(position));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    imagePreview((String) v.getTag());
                }
            });
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }

    public void imagePreview(String imgUrl) {
        if (event == null || (event != null && event.imgUrl == null)) {
            return;
        }

        reportEventAction(event, "imagePreview");

        final Dialog nagDialog = new Dialog(this,
                android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        nagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        nagDialog.setCancelable(true);
        nagDialog.setContentView(R.layout.dialog_image_preview);

        ImageViewTouch preview = (ImageViewTouch) nagDialog.findViewById(R.id.image_preview);
        Glide.with(this).load(imgUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .crossFade()
                .into(preview);

        Button btnClose = (Button) nagDialog.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                nagDialog.dismiss();
            }
        });

        nagDialog.show();
    }

    private class DotsSelector implements ViewPager.OnPageChangeListener {
        private final BaseContextActivity activity;

        private DotsSelector(BaseContextActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // do nothing.
        }

        @Override
        public void onPageSelected(int position) {
            if (position != 0) {
                activity.reportActionToAnalytics("featuredSwipe");
            }
            for (int i = 0; i < dotsView.getChildCount(); i++) {
                dotsView.getChildAt(i).setSelected(i == position);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // do nothing.
        }
    }

    public void removeFavourite(View v) {
        reportEventAction(event, "removeFavourite");

        EventsMarkerManager.Editor eventsMarkerEditor =
                EventsMarkerManager.getInstance(this).getEditor();
        eventsMarkerEditor.recordEventMark(event, null, false);

        eventsMarkerEditor.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_FOR_RESULT_AMA) {
                if (fragment != null) {
                    fragment.ama(new EventInfoObject(event));
                }
            }
            if (requestCode == REQUEST_FOR_RESULT_BOOK_TICKETS) {
                openBookingSite();
            }
            if (requestCode == REQUEST_FOR_RESULT_CALL_EVENT) {
                if (fragment != null) {
                    fragment.call(event);
                }
            }

            if (requestCode == REQUEST_FOR_RESULT_WRITE_REVIEW) {
                makeMyReviewsServerRequest(true);
                if (fragment != null) {
                    fragment.setShowWriteReview(true);

                }
            }
        }
    }

    boolean isConfigLayoutShown = false;

    public void showHideConfigsLayout() {
        if (isConfigLayoutShown) {
            hideConfigLayout();
        } else {
            showConfigLayout();
        }
    }

    public static final int MOVE_VIEW_TO_POS = 2500;

    public void hideConfigLayout() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(configParentLayout, View.TRANSLATION_Y, 0, MOVE_VIEW_TO_POS);
        anim.setDuration(500);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isConfigLayoutShown = false;
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

    public void showConfigLayout() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(configParentLayout, View.TRANSLATION_Y, MOVE_VIEW_TO_POS, 0);
        anim.setDuration(500);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isConfigLayoutShown = true;
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

    public String getCommaSeparatedString(ArrayList<String> items) {
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(item);
        }
        return builder.toString();
    }


    public boolean addConfigsData(LinkedTreeMap<String, Object> configMap) {
        boolean isKeyAdded = false;
        configLayout.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Hide layout
                hideConfigLayout();
            }
        });
        if (checkIfKeyHasValue("special_highlights", configMap) || checkIfKeyHasValue("artists_performing", configMap)) {
            configLayout.findViewById(R.id.highlights_layout).setVisibility(View.VISIBLE);
            // configLayout.findViewById(R.id.highlights_border).setVisibility(View.VISIBLE);
            if (checkIfKeyHasValue("special_highlights", configMap)) {
                isKeyAdded = true;
                LinearLayout highlightsContainer = (LinearLayout) configLayout.findViewById(R.id.highlights_container);
                highlightsContainer.setVisibility(View.VISIBLE);
                configLayout.findViewById(R.id.highlights_title).setVisibility(View.VISIBLE);
                String[] value;
                if (((LinkedTreeMap<String, Object>) configMap.get("special_highlights")).get("value") instanceof String) {
                    value = ((String) ((LinkedTreeMap<String, Object>) configMap.get("special_highlights")).get("value")).split(",");
                } else {
                    value = (String[]) ((ArrayList) ((LinkedTreeMap<String, Object>) configMap.get("special_highlights")).get("value")).toArray();
                }
                for (int i = 0; i < value.length; i++) {
                    View highlightView = getLayoutInflater().inflate(R.layout.textview_layout, highlightsContainer, false);
                    TextView textView = (TextView) highlightView.findViewById(R.id.textview_text);
                    textView.setText("\u2022 " + value[i].trim());
                    highlightsContainer.addView(highlightView);
                }
            } else {
                configLayout.findViewById(R.id.highlights_container).setVisibility(View.GONE);
                configLayout.findViewById(R.id.highlights_title).setVisibility(View.GONE);
            }

            if (checkIfKeyHasValue("artists_performing", configMap)) {
                isKeyAdded = true;
                LinearLayout artistsContainer = (LinearLayout) configLayout.findViewById(R.id.artists_container);
                artistsContainer.setVisibility(View.VISIBLE);
                configLayout.findViewById(R.id.artists_title).setVisibility(View.VISIBLE);
                String[] value;
                if (((LinkedTreeMap<String, Object>) configMap.get("artists_performing")).get("value") instanceof String) {
                    value = ((String) ((LinkedTreeMap<String, Object>) configMap.get("artists_performing")).get("value")).split(",");
                } else {
                    value = (String[]) ((ArrayList) ((LinkedTreeMap<String, Object>) configMap.get("artists_performing")).get("value")).toArray();
                }
                for (int i = 0; i < value.length; i++) {
                    View highlightView = getLayoutInflater().inflate(R.layout.textview_layout, artistsContainer, false);
                    TextView textView = (TextView) highlightView.findViewById(R.id.textview_text);
                    textView.setText("\u2022 " + value[i].trim());
                    artistsContainer.addView(highlightView);
                }
            } else {
                configLayout.findViewById(R.id.artists_container).setVisibility(View.GONE);
                configLayout.findViewById(R.id.artists_title).setVisibility(View.GONE);
            }


        } else {
            //  configLayout.findViewById(R.id.highlights_border).setVisibility(View.GONE);
            configLayout.findViewById(R.id.highlights_layout).setVisibility(View.GONE);
        }
        //Add party

        if (checkIfParentChildKeyHasValue("is_parties_and_nightlife", "party_venue_type", configMap)
                || checkIfParentChildKeyHasValue("is_parties_and_nightlife", "venue_view", configMap)
                || checkIfKeyHasValue("is_unlimited_food", configMap)
                || checkIfKeyHasValue("is_unlimited_alcohol", configMap)
                || checkIfParentChildKeyHasValue("is_parties_and_nightlife", "is_stags_allowed", configMap)
                || checkIfParentChildKeyHasValue("is_parties_and_nightlife", "is_parking_available", configMap)
                || checkIfKeyHasValue("is_group_discounts", configMap)) {
            LinearLayout partyLayout = (LinearLayout) configLayout.findViewById(R.id.party_info_layout);
            partyLayout.setVisibility(View.VISIBLE);
            if ((configLayout.findViewById(R.id.highlights_layout)).isShown()) {
                configLayout.findViewById(R.id.highlights_border).setVisibility(View.VISIBLE);
            } else {
                configLayout.findViewById(R.id.highlights_border).setVisibility(View.GONE);
            }

            int partyLayoutCount = 1;
            int childCount = 0;
            if (checkIfParentChildKeyHasValue("is_parties_and_nightlife", "party_venue_type", configMap)) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("party_venue_type")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("party_venue_type")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("party_venue_type")).get("value"));
                }
                isKeyAdded = true;
                addPartyVenue(partyLayoutCount, childCount, "Venue Type", value, "party_info_layout_", "party_info_textview_");

            }
            if (checkIfParentChildKeyHasValue("is_parties_and_nightlife", "venue_view", configMap)) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("venue_view")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("venue_view")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("venue_view")).get("value"));
                }
                isKeyAdded = true;
                addPartyVenue(partyLayoutCount, childCount, "Venue View", value, "party_info_layout_", "party_info_textview_");
            }
            if (checkIfKeyHasValue("is_unlimited_food", configMap)) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_unlimited_food")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                isKeyAdded = true;
                addPartyVenue(partyLayoutCount, childCount, "Unlimited Food Available", finalValue, "party_info_layout_", "party_info_textview_");
            }
            if (checkIfKeyHasValue("is_unlimited_alcohol", configMap)) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_unlimited_alcohol")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                isKeyAdded = true;
                addPartyVenue(partyLayoutCount, childCount, "Unlimited Alcohol Available", finalValue, "party_info_layout_", "party_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_parties_and_nightlife", "is_stags_allowed", configMap)) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("is_stags_allowed")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                isKeyAdded = true;
                addPartyVenue(partyLayoutCount, childCount, "Stag Entry Allowed", finalValue, "party_info_layout_", "party_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_parties_and_nightlife", "is_parking_available", configMap)) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("is_parking_available")).get("value");

                isKeyAdded = true;
                addPartyVenue(partyLayoutCount, childCount, "Parking Available", value, "party_info_layout_", "party_info_textview_");
            }
            if (checkIfKeyHasValue("is_group_discounts", configMap)) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_group_discounts")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                isKeyAdded = true;
                addPartyVenue(partyLayoutCount, childCount, "Group Discounts", finalValue, "party_info_layout_", "party_info_textview_");
            }

        } else {
            configLayout.findViewById(R.id.party_info_layout).setVisibility(View.GONE);
            configLayout.findViewById(R.id.highlights_border).setVisibility(View.GONE);
        }
        //Add Outdoors
        boolean showOutdoors = false;
        if (configMap.containsKey("is_outdoors") && ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).containsKey("value")) {
            String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("value");
            showOutdoors = value.equalsIgnoreCase("true") ? true : false;
        }
        if (showOutdoors && (checkIfParentChildKeyHasValue("is_outdoors", "is_transportation_available", configMap)
                || checkIfParentChildKeyHasValue("is_outdoors", "is_alcohol_allowed", configMap)
                || checkIfParentChildKeyHasValue("is_outdoors", "tour_duration", configMap)
                || checkIfParentChildKeyHasValue("is_outdoors", "outdoor_venue_type", configMap)
                || checkIfParentChildKeyHasValue("is_outdoors", "stay_type", configMap)
                || checkIfParentChildKeyHasValue("is_outdoors", "tent_sharing_type", configMap)
                || checkIfParentChildKeyHasValue("is_outdoors", "food_type", configMap)
                || checkIfParentChildKeyHasValue("is_outdoors", "activity_type", configMap))) {
            configLayout.findViewById(R.id.outdoor_info_layout).setVisibility(View.VISIBLE);
            if ((configLayout.findViewById(R.id.party_info_layout)).isShown()) {
                configLayout.findViewById(R.id.party_border).setVisibility(View.VISIBLE);
            } else {
                configLayout.findViewById(R.id.party_border).setVisibility(View.GONE);
            }

            int outdoorLayoutCount = 1;
            int outdoorChildCount = 0;
            if (checkIfParentChildKeyHasValue("is_outdoors", "is_transportation_available", configMap)) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("is_transportation_available")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                isKeyAdded = true;
                addPartyVenue(outdoorLayoutCount, outdoorChildCount, "Transportation Available", finalValue, "outdoor_info_layout_", "outdoor_info_textview_");

            }

            if (checkIfParentChildKeyHasValue("is_outdoors", "is_alcohol_allowed", configMap)) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("is_alcohol_allowed")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                isKeyAdded = true;
                addPartyVenue(outdoorLayoutCount, outdoorChildCount, "Alcohol Allowed", finalValue, "outdoor_info_layout_", "outdoor_info_textview_");

            }

            if (checkIfParentChildKeyHasValue("is_outdoors", "tour_duration", configMap)) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("tour_duration")).get("value");
                isKeyAdded = true;
                addPartyVenue(outdoorLayoutCount, outdoorChildCount, "Duration", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }
            if (checkIfParentChildKeyHasValue("is_outdoors", "outdoor_venue_type", configMap)) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("outdoor_venue_type")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("outdoor_venue_type")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("outdoor_venue_type")).get("value"));
                }
                isKeyAdded = true;

                addPartyVenue(outdoorLayoutCount, outdoorChildCount, "Venue type", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }
            if (checkIfParentChildKeyHasValue("is_outdoors", "stay_type", configMap)) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("stay_type")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("stay_type")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("stay_type")).get("value"));
                }
                isKeyAdded = true;
                addPartyVenue(outdoorLayoutCount, outdoorChildCount, "Stay type", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }

            if (checkIfParentChildKeyHasValue("is_outdoors", "tent_sharing_type", configMap)) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("tent_sharing_type")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("tent_sharing_type")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("tent_sharing_type")).get("value"));
                }
                isKeyAdded = true;
                addPartyVenue(outdoorLayoutCount, outdoorChildCount, "Tent sharing type", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }
            if (checkIfParentChildKeyHasValue("is_outdoors", "food_type", configMap)) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("food_type")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("food_type")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("food_type")).get("value"));
                }
                isKeyAdded = true;
                addPartyVenue(outdoorLayoutCount, outdoorChildCount, "Food type", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }
            if (checkIfParentChildKeyHasValue("is_outdoors", "activity_type", configMap)) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("activity_type")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("activity_type")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("activity_type")).get("value"));
                }
                isKeyAdded = true;
                addPartyVenue(outdoorLayoutCount, outdoorChildCount, "Activity type", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }

        } else {
            configLayout.findViewById(R.id.outdoor_info_layout).setVisibility(View.GONE);
            configLayout.findViewById(R.id.party_border).setVisibility(View.GONE);
        }

        //Add Kid friendly
        boolean showKidFriendly = false;
        if (configMap.containsKey("is_kid_friendly") && ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).containsKey("value")) {
            String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("value");
            showKidFriendly = value.equalsIgnoreCase("true") ? true : false;
        }
        if (showKidFriendly && (checkIfKeyHasValue("is_kid_friendly", configMap)
                || checkIfParentChildKeyHasValue("is_kid_friendly", "is_free_for_kids_below_five", configMap)
                || checkIfParentChildKeyHasValue("is_kid_friendly", "is_child_care_zone", configMap)
                || checkIfParentChildKeyHasValue("is_kid_friendly", "max_age_kids_pricing", configMap)
                || checkIfParentChildKeyHasValue("is_kid_friendly", "kid_activities", configMap))) {
            configLayout.findViewById(R.id.kids_info_layout).setVisibility(View.VISIBLE);
            if ((configLayout.findViewById(R.id.outdoor_info_layout)).isShown())
                configLayout.findViewById(R.id.outdoor_border).setVisibility(View.VISIBLE);
            else
                configLayout.findViewById(R.id.outdoor_border).setVisibility(View.GONE);

            int kidsLayoutCount = 1;
            int kidsChildCount = 0;
            if (checkIfKeyHasValue("is_kid_friendly", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                isKeyAdded = true;
                addPartyVenue(kidsLayoutCount, kidsChildCount, "Kids Friendly", finalValue, "kids_info_layout_", "kids_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_kid_friendly", "is_free_for_kids_below_five", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("is_free_for_kids_below_five")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                isKeyAdded = true;
                addPartyVenue(kidsLayoutCount, kidsChildCount, "Free for kids below 5 years", finalValue, "kids_info_layout_", "kids_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_kid_friendly", "is_child_care_zone", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("is_child_care_zone")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                isKeyAdded = true;
                addPartyVenue(kidsLayoutCount, kidsChildCount, "Child care zone available", finalValue, "kids_info_layout_", "kids_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_kid_friendly", "max_age_kids_pricing", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("max_age_kids_pricing")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("max_age_kids_pricing")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("max_age_kids_pricing")).get("value"));
                }
                isKeyAdded = true;
                addPartyVenue(kidsLayoutCount, kidsChildCount, "Max age for kids pricing ", value, "kids_info_layout_", "kids_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_kid_friendly", "kid_activities", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("kid_activities")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("kid_activities")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("kid_activities")).get("value"));
                }
                isKeyAdded = true;
                addPartyVenue(kidsLayoutCount, kidsChildCount, "Activities for Kids", value, "kids_info_layout_", "kids_info_textview_");
            }

        } else {
            configLayout.findViewById(R.id.kids_info_layout).setVisibility(View.GONE);
            configLayout.findViewById(R.id.outdoor_border).setVisibility(View.GONE);
        }

        //Add Stay Type
        boolean showStayProvided = false;
        if (configMap.containsKey("is_stay_provided") && ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).containsKey("value")) {
            String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("value");
            showStayProvided = value.equalsIgnoreCase("true") ? true : false;
        }
        if (showStayProvided && (checkIfKeyHasValue("is_stay_provided", configMap)
                || checkIfParentChildKeyHasValue("is_stay_provided", "is_breakfast_included", configMap)
                || checkIfParentChildKeyHasValue("is_stay_provided", "is_extra_bed_available", configMap)
                || checkIfParentChildKeyHasValue("is_stay_provided", "check_in_time", configMap)
                || checkIfParentChildKeyHasValue("is_stay_provided", "check_out_time", configMap))) {
            configLayout.findViewById(R.id.stay_info_layout).setVisibility(View.VISIBLE);
            if ((configLayout.findViewById(R.id.kids_info_layout)).isShown())
                configLayout.findViewById(R.id.kids_border).setVisibility(View.VISIBLE);
            else
                configLayout.findViewById(R.id.kids_border).setVisibility(View.GONE);

            int kidsLayoutCount = 1;
            int kidsChildCount = 0;
            if (checkIfKeyHasValue("is_stay_provided", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                isKeyAdded = true;
                addPartyVenue(kidsLayoutCount, kidsChildCount, "Stay Provided", finalValue, "stay_info_layout_", "stay_info_textview_");
            }
            if (checkIfParentChildKeyHasValue("is_stay_provided", "is_breakfast_included", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("is_breakfast_included")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                isKeyAdded = true;

                addPartyVenue(kidsLayoutCount, kidsChildCount, "Breakfast included", finalValue, "stay_info_layout_", "stay_info_textview_");
            }
            if (checkIfParentChildKeyHasValue("is_stay_provided", "is_extra_bed_available", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("is_extra_bed_available")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                isKeyAdded = true;
                addPartyVenue(kidsLayoutCount, kidsChildCount, "Extra Bed Available", finalValue, "stay_info_layout_", "stay_info_textview_");
            }
            if (checkIfParentChildKeyHasValue("is_stay_provided", "check_in_time", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("check_in_time")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("check_in_time")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("check_in_time")).get("value"));
                }
                isKeyAdded = true;
                addPartyVenue(kidsLayoutCount, kidsChildCount, "Check in time", value, "stay_info_layout_", "stay_info_textview_");
            }
            if (checkIfParentChildKeyHasValue("is_stay_provided", "check_out_time", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("check_out_time")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("check_out_time")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("check_out_time")).get("value"));
                }
                isKeyAdded = true;
                addPartyVenue(kidsLayoutCount, kidsChildCount, "Check out time", value, "stay_info_layout_", "stay_info_textview_");
            }

        } else {
            configLayout.findViewById(R.id.stay_info_layout).setVisibility(View.GONE);
            configLayout.findViewById(R.id.kids_border).setVisibility(View.GONE);
        }
        return isKeyAdded;
    }

    public void addPartyVenue(int layoutCount, int childCount, String keyName, String value, String layoutName, String textViewName) {
        try {
            int layoutKey = R.id.class.getField(layoutName + layoutCount).getInt(null);
            int textViewKey = R.id.class.getField(textViewName + layoutCount + childCount).getInt(null);
            configLayout.findViewById(layoutKey).setVisibility(View.VISIBLE);
            TextView textView = (TextView) configLayout.findViewById(textViewKey);
            textView.setVisibility(View.VISIBLE);
            SpannableString string = new SpannableString(keyName + " : " + value);
            string.setSpan(new StyleSpan(Typeface.BOLD), keyName.length(), string.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(string);

        } catch (NoSuchFieldException | IllegalAccessException e) {

        }
    }

    public boolean checkIfKeyHasValue(String key, LinkedTreeMap<String, Object> configMap) {
        if (configMap.containsKey(key)) {
            LinkedTreeMap<String, Object> map = (LinkedTreeMap<String, Object>) configMap.get(key);
            if (map.containsKey("value")
                    && map.get("value") != null
                    && (map.get("value") instanceof String && ((String) map.get("value")).length() > 0
                    && !((String) map.get("value")).equalsIgnoreCase("n/a"))
                    && !((String) map.get("value")).equalsIgnoreCase("false")
                    || (map.get("value") instanceof ArrayList && ((ArrayList) map.get("value")).size() > 0
                    && !((ArrayList) map.get("value")).contains("n/a"))) {
                return true;
            }
        }
        return false;
    }

    public boolean checkIfParentChildKeyHasValue(String parentKey, String childkey, LinkedTreeMap<String, Object> configMap) {
        if (configMap.containsKey(parentKey)) {
            LinkedTreeMap<String, Object> map = (LinkedTreeMap<String, Object>) configMap.get(parentKey);
            if (map.containsKey(childkey)) {
                LinkedTreeMap<String, Object> childMap = (LinkedTreeMap<String, Object>) map.get(childkey);
                if (childMap.containsKey("value")
                        && childMap.get("value") != null
                        && (childMap.get("value") instanceof String && ((String) childMap.get("value")).length() > 0 &&
                        !((String) childMap.get("value")).equalsIgnoreCase("n/a"))
                        && !((String) childMap.get("value")).equalsIgnoreCase("false")
                        || (childMap.get("value") instanceof ArrayList && ((ArrayList) childMap.get("value")).size() > 0
                        && !((ArrayList) childMap.get("value")).contains("n/a"))) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public void onBackPressed() {
        if (isConfigLayoutShown) {
            hideConfigLayout();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        super.onDestroy();

    }
}
