package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.IntentUtils;

/**
 * An abstract base fragment which is used to show events information.
 */
public abstract class BaseEventsFragment extends Fragment {
    public static final String EVENT_CONTEXT_PARAM = BaseEventsFragment.class.getName() + "_event_context";
    public static final String SHOW_FOLLOW_PARAM = BaseEventsFragment.class.getName() + "_add_follow_card";
    public static final String SHOW_CATEGORIES_PARAM = BaseEventsFragment.class.getName() + "_show_categories";

    protected BaseContextActivity activity;
    protected EventsContext eventsContext;
    protected boolean showFollowCard;
    protected boolean showCategories;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseContextActivity) activity;

        eventsContext = getArguments().getParcelable(EVENT_CONTEXT_PARAM);
        showFollowCard = getArguments().getBoolean(SHOW_FOLLOW_PARAM);
        showCategories = getArguments().getBoolean(SHOW_CATEGORIES_PARAM);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        VolleyHelper.getRequestQueue(activity).cancelAll(this);
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
        activity.reportActionToAnalytics("header:" + query,
                eventsContext.query + eventsContext.dateFilter);
        EventsContext param = new EventsContext(eventsContext.location, query);
        param.dateFilter = eventsContext.dateFilter;
        Intent intent = new Intent(activity, activity.getClass())
                .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, param);
        startActivity(intent);
    }

    public void seeAll() {
        activity.reportActionToAnalytics("seeAll", eventsContext.query + eventsContext.dateFilter);
        EventsContext param = new EventsContext(eventsContext.location, eventsContext.query);
        Intent intent = new Intent(activity, activity.getClass())
                .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, param);
        startActivity(intent);
    }
}
