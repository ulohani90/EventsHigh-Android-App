package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.eventshigh.nearme.app.data.EventsContext;

/**
 * An abstract base fragment which is used to show events information.
 */
public abstract class BaseEventsFragment extends Fragment {
    public static final String EVENT_CONTEXT_PARAM = BaseEventsFragment.class.getName() + "_event_context";
    public static final String SHOW_FOLLOW_PARAM = BaseEventsFragment.class.getName() + "_add_follow_card";
    public static final String SHOW_OFFER_PARAM = BaseEventsFragment.class.getName() + "_add_offer";

    protected BaseContextActivity activity;
    protected EventsContext eventsContext;
    protected boolean showOfferCard;
    protected boolean showFollowCard;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseContextActivity) activity;

        eventsContext = getArguments().getParcelable(EVENT_CONTEXT_PARAM);
        showFollowCard = getArguments().getBoolean(SHOW_FOLLOW_PARAM);
        showOfferCard = getArguments().getBoolean(SHOW_OFFER_PARAM);
    }

    protected static Bundle getArgs(EventsContext eventsContext,
            boolean showFollowCard, boolean showOffer) {
        Bundle args = new Bundle();
        args.putParcelable(EVENT_CONTEXT_PARAM, eventsContext);
        args.putBoolean(SHOW_FOLLOW_PARAM, showFollowCard);
        args.putBoolean(SHOW_OFFER_PARAM, showOffer);
        return args;
    }
}
