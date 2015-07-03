package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.IntentUtils;

import java.util.concurrent.TimeUnit;

/**
 * An abstract base fragment which is used to show events information.
 */
public abstract class BaseEventsFragment extends Fragment {
    public static final String EVENT_CONTEXT_PARAM = BaseEventsFragment.class.getName() + "_event_context";
    public static final String SHOW_FOLLOW_PARAM = BaseEventsFragment.class.getName() + "_add_follow_card";
    public static final String SHOW_CATEGORIES_PARAM = BaseEventsFragment.class.getName() + "_show_categories";
    public static final Long DEFAULT_REFRESH_INTERVAL = TimeUnit.HOURS.toMillis(1);

    protected BaseContextActivity activity;
    protected EventsContext eventsContext;
    protected boolean showFollowCard;
    protected boolean showCategories;
    private long lastRefreshTimestamp;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseContextActivity) activity;

        eventsContext = getArguments().getParcelable(EVENT_CONTEXT_PARAM);
        showFollowCard = getArguments().getBoolean(SHOW_FOLLOW_PARAM);
        showCategories = getArguments().getBoolean(SHOW_CATEGORIES_PARAM);
        lastRefreshTimestamp = 0;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        VolleyHelper.getRequestQueue(activity).cancelAll(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshIfneeded();
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        refresh();
        lastRefreshTimestamp = System.currentTimeMillis();
    }

    protected void refreshIfneeded() {
        if (lastRefreshTimestamp < System.currentTimeMillis() - refreshInterval()) {
            refresh();
            lastRefreshTimestamp = System.currentTimeMillis();
        }
    }

    protected abstract void refresh();

    protected long refreshInterval() {
        return DEFAULT_REFRESH_INTERVAL;
    }

    protected static Bundle getArgs(EventsContext eventsContext, boolean showFollowCard,
            boolean showCategories) {
        Bundle args = new Bundle();
        args.putParcelable(EVENT_CONTEXT_PARAM, eventsContext);
        args.putBoolean(SHOW_FOLLOW_PARAM, showFollowCard);
        args.putBoolean(SHOW_CATEGORIES_PARAM, showCategories);
        return args;
    }

    public BaseContextActivity getContextActivity() {
        return activity;
    }

    public void showSearchView(String query) {
        activity.reportActionToAnalytics("header:" + query, eventsContext.getLabel());
        EventsContext param = new EventsContext(eventsContext.location, query);
        param.dateFilter = eventsContext.dateFilter;
        Intent intent = new Intent(activity, activity.getClass())
                .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, param);
        startActivity(intent);
    }

    public void showEventDetails(Uri eventDetailsURI) {
        activity.showEventDetails(eventDetailsURI, eventsContext.getLabel());
    }

    public void showEventDetails(Event event, @Nullable Bundle bundle) {
        activity.showEventDetails(event, eventsContext.getLabel(), bundle);
    }

    public void seeAll() {
        activity.reportActionToAnalytics("seeAll", eventsContext.query + eventsContext.dateFilter);
        EventsContext param = new EventsContext(eventsContext.location, eventsContext.query);
        Intent intent = new Intent(activity, activity.getClass())
                .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, param);
        startActivity(intent);
    }
}
