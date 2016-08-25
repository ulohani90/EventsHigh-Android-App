package com.eventshigh.nearme.app.activity;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
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
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.MovieUserReviewObject;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.network.EventRequest;
import com.eventshigh.nearme.app.network.MyReviewsRequest;
import com.eventshigh.nearme.app.network.SocialActionsRequest;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest;
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

import java.util.ArrayList;
import java.util.List;
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

    String planId;

    LinearLayout statsLayout;

    public static final String EVENT_OBJECT = "event_detail_object";

    @Override
    public View getViewForSnackbar() {
        return toolbar;
    }

    public static final String EXTRA_EVENT_PARAM = NewEventDetailActivity.class.getSimpleName() + "_event";
    public static final String EXTRA_PLAN_ID_PARAM = NewEventDetailActivity.class.getSimpleName() + "_plan_id";

    public static final String EVENT_REVIEWS = "event_reviews";
    public static final String EVENT_ID = "event_id";

    LinearLayout dotsView;
    TextView bookView;
    View joinView;

    public static final int REQUEST_FOR_RESULT_BOOK_TICKETS = 0x009;
    public static final int REQUEST_FOR_RESULT_AMA = 0x010;
    public static final int REQUEST_FOR_RESULT_CALL_EVENT = 0x011;
    public static final int REQUEST_FOR_RESULT_WRITE_REVIEW = 0x012;

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
                    shareEventWithBranch(event, null, "Toolbar");
                }
            }
        });

        favAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                makeMyReviewsServerRequest(false);

            } else {
                EventRequest.submit(this, getIntent().getData(), Request.Priority.IMMEDIATE, mEventListener,
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                                Toast.makeText(NewEventDetailActivity.this, R.string.failed_load,
                                        Toast.LENGTH_SHORT).show();
                                VolleyHelper.log(NewEventDetailActivity.this, volleyError);
                                finish();
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

            Account account = new Account(this);
            if (!account.getUserInfo().isSignedIn) {
                FBSigninDialog.show(this, R.string.ui_signin_via_fb, R.string.ui_signin_fb_plan_more, 1);
            }
        }

        EventsMarkerManager.Editor eventsMarkerEditor =
                EventsMarkerManager.getInstance(this).getEditor();
        eventsMarkerEditor.recordEventMark(event, EventsMarkerManager.EventMark.FAVOURITE);
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
            NewEventDetailActivity.this.event = event;
            makeMyReviewsServerRequest(false);
            // populateView(event);

        }
    };

    public void makeMyReviewsServerRequest(boolean shouldByPassCache) {
        MyReviewsRequest.submit(this, account.getUserInfo().email, Request.Priority.IMMEDIATE, this, shouldByPassCache, mReviewListener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(NewEventDetailActivity.this, R.string.failed_load,
                        Toast.LENGTH_SHORT).show();
                VolleyHelper.log(NewEventDetailActivity.this, volleyError);
                finish();
            }
        });
    }

    private Response.Listener<List<MovieUserReviewObject>> mReviewListener = new Response.Listener<List<MovieUserReviewObject>>() {
        @Override
        public void onResponse(List<MovieUserReviewObject> reviews, boolean isIntermediate) {
            findReviewsByUserForMovie(reviews);
            //if (fragment == null)
            addAdapterData();
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
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    ArrayList<String> TABS;

    TextView statsView;

    boolean isDataAttached;

    public void addAdapterData() {
        if (isFavourite(event)) {
            favAction.setSelected(true);
        } else {
            favAction.setSelected(false);
        }
        isDataAttached = true;
        title.setText(event.title);

        final TextView eventPrice = (TextView) findViewById(R.id.event_price);
        String priceString = event.getPriceString();
        if (priceString == null) {
            eventPrice.setVisibility(View.GONE);
        } else {
            eventPrice.setVisibility(View.VISIBLE);
            eventPrice.setText(priceString);
        }
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
                shareEventWithBranch(event, PACKAGE_NAME_WHATSAPP, " bottombar");
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
        if (event.descriptionSections != null && event.descriptionSections.length > 0) {
            for (EventDescriptionSection section : event.descriptionSections) {
                TABS.add(section.name);
            }
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
                                ((ImageView) findViewById(R.id.img1)).setImageResource(resource1 != -1 ? resource1 : Utils.getDummyImageResource());
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
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        VolleyHelper.log(NewEventDetailActivity.this, volleyError);
                    }
                }
        );
        /*SocialInvitationsRequest.submit(this, Request.Priority.LOW, this, false,
                new Response.Listener<SocialInvitationsRequest.CommonInviteObject>() {
                    @Override
                    public void onResponse(SocialInvitationsRequest.CommonInviteObject commonInviteObject, boolean isIntermediate) {
                        SocialInvitationsRequest.SocialInvite invite = commonInviteObject.getInvites().get(event.id);
                        if (invite == null || invite.getInvitedBy() == null) {
                            reportActionToAnalytics("showSocialInfo", "invitedBy", 0);
                            return;
                        }

                        planId = invite.getPlanId();
                        Set<SocialFriend> allInvitedBy = invite.getAllInvitedBy();
                        Set<SocialFriend> allParticipants = invite.getAllParticipants();
                        reportActionToAnalytics("showSocialInfo", "invitedBy", allInvitedBy.size());

                        String prefix = allInvitedBy.size() == 1 ?
                                allInvitedBy.iterator().next().getName() : "";
                        String suffix = allParticipants.size() < 3 ? "" : "and " + (allParticipants.size() - 2) + " more friends";

                        ContactListView invitedByView = (ContactListView) findViewById(R.id.invited_by);
                        invitedByView.setVisibility(View.VISIBLE);
                        invitedByView.setText(prefix + " has invited you " + suffix);
                        invitedByView.setFollowers(NewEventDetailActivity.this, allInvitedBy, allParticipants);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        VolleyHelper.log(NewEventDetailActivity.this, volleyError);
                    }
                }
        );*/

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
        addToFavourite = true;
        reportEventAction(event, "bookTicket");
        new UserActionHelper(this).recordAction(UserActionHelper.EventAction.BOOK, event.id);

        final Uri.Builder bookingUriBuilder = Uri.parse(event.bookingUrl).buildUpon();

        if (event.bookingUrl.contains("https://ticketing.eventshigh.com/checkout3.jsp?eid=" + event.id)) {
            /*
            bookingUriBuilder.appendQueryParameter("did", Utils.getAndroidId(this));
            bookingUriBuilder.appendQueryParameter("name", userInfo.name);
            bookingUriBuilder.appendQueryParameter("mobile", userInfo.phoneNo);
            bookingUriBuilder.appendQueryParameter("src", "eh-android");*/
            Intent intent = new Intent(this, EventBookingDetailActivity.class);
            intent.putExtra("event", event);
            startActivity(intent);

        } else {
            try {
                CustomUrlActivity.launchCustomUrl(this, bookingUriBuilder.build(),
                        getString(R.string.title_book));
            } catch (Exception e) {
                Crashlytics.getInstance().core.logException(e);
                showMessage(R.string.retry);
            }
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
                bundle.putParcelable("event", event);

                fragment = EventInfoFragment.newInstance(bundle);
                return fragment;
            }
            bundle.putString("description", event.descriptionSections[position - 1].description);
            return EventDetailCustomFragment.newInstance(bundle);

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
            return event.allImages.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = getLayoutInflater().inflate(R.layout.card_refer, container, false);
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
        eventsMarkerEditor.recordEventMark(event, null);

        eventsMarkerEditor.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_FOR_RESULT_AMA) {
                if (fragment != null) {
                    fragment.ama(event);
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
}
