package com.eventshigh.nearme.app.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.VolleyHelper;

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
    public void onAttach(Context context) {
        super.onAttach(context);
        this.activity = (BaseContextActivity) context;
        eventsContext = getArguments().getParcelable(EVENT_CONTEXT_PARAM);
        showFollowCard = getArguments().getBoolean(SHOW_FOLLOW_PARAM);
        showCategories = getArguments().getBoolean(SHOW_CATEGORIES_PARAM);
    }

    @Override
    public void onStop() {
        super.onStop();
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
}
