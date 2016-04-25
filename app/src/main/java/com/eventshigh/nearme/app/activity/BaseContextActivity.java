package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.IntentUtils;

/**
 * Base activity for location aware events listing. This class implements common methods to fetch
 * fetch event listings when needed and asking the parent activity to show events as per user
 * interactions.
 *
 * This class also implements base user interactions like tabs, filters etc.
 */
public abstract class BaseContextActivity extends BaseActivity {
    protected EventsContext eventsContext;
    protected EventsMarkerManager eventsMarkerManager;

    protected Toolbar toolbar;

    @Override
    protected void onStart() {
        super.onStart();

        // Initialize the EventsMarkerManager.Editor.
        eventsMarkerManager = EventsMarkerManager.getInstance(this);

        // Show the verify phone snakbar if needed.
       // showVerifyPhoneSnackbar();
    }

    @Override
    public boolean onSearchRequested() {
        reportActionToAnalytics("onSearchRequested");
        Bundle appData = new Bundle();
        appData.putParcelable(IntentUtils.EXTRA_EVENT_CONTEXT, eventsContext);
        startSearch(null, false, appData, false);
        return true;
    }

    @Override
    public View getViewForSnackbar() {
        return toolbar;
    }

    protected void setupLayout(@LayoutRes int layoutResID) {
        // Setup the UI.
        setContentView(layoutResID);

        // Setup action bar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // See if we have context passed to us within intent.
        eventsContext = IntentUtils.processIntent(this, getIntent());
    }

    protected void setTitle() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            String title = DateTimeUtils.queryToTitle(eventsContext.query);
            if (!eventsContext.dateFilter.isEmpty()) {
                title += " on " +  DateTimeUtils.queryToTitle(eventsContext.dateFilter);
            }
            actionBar.setTitle(title);
        }
    }

    public void showActionBar() {
        if (toolbar.getVisibility() == View.VISIBLE) {
            return;
        }

        toolbar.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        final int targetHeight = toolbar.getMeasuredHeight();

        toolbar.getLayoutParams().height = 0;
        toolbar.setVisibility(View.VISIBLE);
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                toolbar.getLayoutParams().height = interpolatedTime == 1
                        ? LayoutParams.WRAP_CONTENT
                        : (int)(targetHeight * interpolatedTime);
                toolbar.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        animation.setDuration(200);
        toolbar.startAnimation(animation);
    }

    public void hideActionBar() {
        if (toolbar.getVisibility() == View.GONE) {
            return;
        }

        final int initialHeight = toolbar.getMeasuredHeight();
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1){
                    toolbar.setVisibility(View.GONE);
                }else{
                    toolbar.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
                    toolbar.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        animation.setDuration(200);
        toolbar.startAnimation(animation);
    }

    protected void showVerifyPhoneSnackbar() {
        boolean isVerificationPending = Account.isPhoneVerifyPending(this);
        final View view = findViewById(R.id.verify_phone_container);
        view.setVisibility(isVerificationPending ? View.VISIBLE :View.GONE);
        view.findViewById(R.id.verify_phone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BaseContextActivity.this, PhoneLoginActivity.class));
                view.setVisibility(View.GONE);
            }
        });
        view.findViewById(R.id.verify_phone_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Account.disablePhoneVerifySnackbar();
                view.setVisibility(View.GONE);
            }
        });
    }

    public boolean isFavourite(Event event) {
        return eventsMarkerManager.isFavourite(event.id);
    }

    public @Nullable
    EventMark getEventMark(Event event) {
        return eventsMarkerManager.getEventMark(event.id);
    }

    public void recordEventMark(Event event, @Nullable EventMark mark) {
        eventsMarkerManager.getEditor().recordEventMark(event, mark).close();
    }

    public void reportEventAction(Event event, String actionName, int position) {
        reportActionToAnalytics(actionName,
                eventsContext.dateFilter,
                1,
                isFavourite(event) ? "Favourite" : "No-Favourite",
                event.ehRecommended ? "Recommended" : "Non-Recommended",
                eventsContext.query.isEmpty() ? " " : eventsContext.query,
                Integer.toString(position));
    }

    public void showSearchView(String query) {
        reportActionToAnalytics("showSearchView", query);
        EventsContext param = new EventsContext(eventsContext.city, query);
        param.dateFilter = eventsContext.dateFilter;
        Intent intent = new Intent(this, getClass())
                .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, param);
        startActivity(intent);
    }

    public void showEventDetails(Uri eventDetailsURI, @Nullable String label) {
        reportActionToAnalytics("showEventDetails", label);
        Intent detailIntent = new Intent(this, EventDetailActivity.class);
        detailIntent.setData(eventDetailsURI);
        startActivity(detailIntent);
    }

    public void showEventDetails(Event event, @Nullable String label, @Nullable Bundle bundle) {
        reportEventAction(event, "showEventDetails", label);
        Intent detailIntent = new Intent(this, EventDetailActivity.class);
        detailIntent.putExtra(EventDetailActivity.EXTRA_EVENT_PARAM, event);
        startActivity(detailIntent, bundle);
    }

    public void seeAll() {
        reportActionToAnalytics("seeAll", eventsContext.getLabel());
        EventsContext param = new EventsContext(eventsContext.city, eventsContext.query);
        Intent intent = new Intent(this, this.getClass())
                .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, param);
        startActivity(intent);
    }
}
