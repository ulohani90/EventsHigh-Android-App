package com.eventshigh.nearme.app.ui.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest.SocialInvite;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.ContactListView;

public class EventCard extends ViewHolder {
    private final boolean shouldAdjustImageHeight;
    private final ImageView bgView;
    private final ImageView recommendedView;
    private final TextView titleView;
    private final ImageView favouriteView;
    private final TextView eventTimeView;
    private final TextView priceView;
    private final TextView venueView;
    private final TextView travelTimeView;
    private final View arrowView;
    private final TextView eventStatsView;
    private final ContactListView invitedByView;
    private final View infoArrowView;
    private final ImageView share;

    public static EventCard newInstance(Activity activity, ViewGroup parent,
                                        boolean shouldAdjustImageHeight) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_event, parent, false);
        return new EventCard(view, shouldAdjustImageHeight);
    }

    // Build the view, reuse existing if possible.
    public static View getEventCard(final Event event, final BaseContextActivity activity,
                                    @Nullable View reuseView, ViewGroup parent) {
        EventCard card = reuseView != null ? new EventCard(reuseView, true) :
                newInstance(activity, parent, true);
        card.bindEventView(event, activity,0,null);
        return card.itemView;
    }

    public EventCard(View cardView, boolean shouldAdjustImageHeight) {
        super(cardView);

        this.shouldAdjustImageHeight = shouldAdjustImageHeight;
        bgView = (ImageView) cardView.findViewById(R.id.event_bg);
        recommendedView = (ImageView) cardView.findViewById(R.id.event_recommended);
        titleView = (TextView) cardView.findViewById(R.id.event_title);
        favouriteView = (ImageView) cardView.findViewById(R.id.action_favourite);
        eventTimeView = (TextView) cardView.findViewById(R.id.event_time);
        priceView = (TextView) cardView.findViewById(R.id.event_price);
        venueView = (TextView) cardView.findViewById(R.id.event_venue);
        travelTimeView = (TextView) cardView.findViewById(R.id.event_travel_time);
        arrowView = cardView.findViewById(R.id.arrow);
        eventStatsView = (TextView) cardView.findViewById(R.id.event_stats);
        invitedByView = (ContactListView) cardView.findViewById(R.id.invited_by);
        infoArrowView = cardView.findViewById(R.id.info_arrow);
        share = (ImageView)cardView.findViewById(R.id.share);
    }


    public void setFavouriteView(@Nullable EventMark eventMark) {
        favouriteView.setTag(eventMark);
        favouriteView.setImageResource(EventMark.isFavourite(eventMark) ?
                R.drawable.ic_favorite_red_18dp : R.drawable.ic_favorite_border_black_18dp);

    }

    @SuppressLint("SetTextI18n")
    public void bindEventView(final Event event, boolean isFirstEvent, final int position,
                              final BaseContextActivity activity,
                              @Nullable SocialInvite invite) {
        bindEventView(event, activity,position,null);

        arrowView.setVisibility(isFirstEvent ? View.VISIBLE : View.GONE);

        // Set the travel time.
        String travelTime = LocationUtils.getTravelTime(activity, activity.getUserLocation(), event.location);
        if (travelTime != null) {
            travelTimeView.setText(travelTime);
            travelTimeView.setVisibility(View.VISIBLE);
        } else {
            travelTimeView.setVisibility(View.GONE);
        }

        // Set actions handlers.
        favouriteView.setVisibility(View.VISIBLE);
        setFavouriteView(activity.getEventMark(event));
        favouriteView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EventMark oldMark = (EventMark) favouriteView.getTag();
                EventMark newMark = EventMark.isFavourite(oldMark) ? null : EventMark.FAVOURITE;
                activity.reportEventAction(event,
                        EventMark.isFavourite(newMark) ? "addFavourite" : "removeFavourite",
                        position);
                activity.recordEventMark(event, newMark);
                setFavouriteView(newMark);
               if (EventMark.isFavourite(newMark)) {
                   activity.showMessage("Added to My Events");
               }else{
                   activity.showMessage("Removed from My Events");}

            }
        });

        // Is user invited to this event ?
        if (invite != null && invite.getInvitedBy() != null) {
            invitedByView.setVisibility(View.VISIBLE);
            invitedByView.setFollowers(activity, invite.getAllInvitedBy());
            infoArrowView.setVisibility(View.VISIBLE);
        } else if (event.numViews > 5) {
            eventStatsView.setVisibility(View.VISIBLE);
            eventStatsView.setText("" + event.numViews + " views");
            infoArrowView.setVisibility(View.VISIBLE);
        }else{
            eventStatsView.setVisibility(View.VISIBLE);
            eventStatsView.setText("" + Utils.getRandomNumber(10,50) + " views");
            infoArrowView.setVisibility(View.VISIBLE);
        }

        //Share event
        share.setVisibility(View.VISIBLE);
        share.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.shareEventWithBranch(event,null,null);
            }
        });
    }

    public void bindEventView(final Event event, final BaseContextActivity activity, final int position, final EventsAdapter.OnItemClickedListener listener) {
        itemView.setVisibility(View.VISIBLE);
        itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener!=null)
                    listener.onItemClicked(position);
                activity.showEventDetails(event, "", null);
            }
        });

        // Set the background image.
        Glide.with(activity).load(event.imgUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                .into(bgView);

        if (shouldAdjustImageHeight) {
            Utils.waitForViewVisible(bgView, new Runnable() {
                @Override
                public void run() {
                    LayoutParams lp = bgView.getLayoutParams();
                    lp.height = 9 * bgView.getWidth() / 16;
                    bgView.setLayoutParams(lp);
                }
            });
        }

        // Set the title.
        titleView.setText(event.title);
        recommendedView.setVisibility(event.ehRecommended ? View.VISIBLE : View.INVISIBLE);

        // Event Time.
        EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
        if (eventTime == null) {
            eventTimeView.setVisibility(View.INVISIBLE);
        } else {
            eventTimeView.setVisibility(View.VISIBLE);
            eventTimeView.setText(eventTime.toString());
        }

        // Set the price.
        String priceString = event.getPriceString();
        if (priceString == null) {
            priceView.setVisibility(View.GONE);
        } else {
            priceView.setVisibility(View.VISIBLE);
            priceView.setText(priceString);
        }

        // Set the venue.
        venueView.setText(event.getShortAddress());

        arrowView.setVisibility(View.GONE);
        favouriteView.setVisibility(View.GONE);
        travelTimeView.setVisibility(View.GONE);
        eventStatsView.setVisibility(View.GONE);
        share.setVisibility(View.GONE);
        invitedByView.setVisibility(View.GONE);
        infoArrowView.setVisibility(View.GONE);
    }
    @SuppressLint("SetTextI18n")
    public void bindEventView(final Event event, boolean isFirstEvent, final int position,
                              final BaseContextActivity activity,
                              @Nullable SocialInvite invite,EventsAdapter.OnItemClickedListener listener) {
        bindEventView(event, activity,position,listener);

        arrowView.setVisibility(isFirstEvent ? View.VISIBLE : View.GONE);

        // Set the travel time.
        String travelTime = LocationUtils.getTravelTime(activity, activity.getUserLocation(), event.location);
        if (travelTime != null) {
            travelTimeView.setText(travelTime);
            travelTimeView.setVisibility(View.VISIBLE);
        } else {
            travelTimeView.setVisibility(View.GONE);
        }

        // Set actions handlers.
        favouriteView.setVisibility(View.VISIBLE);
        setFavouriteView(activity.getEventMark(event));
        favouriteView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EventMark oldMark = (EventMark) favouriteView.getTag();
                EventMark newMark = EventMark.isFavourite(oldMark) ? null : EventMark.FAVOURITE;
                activity.reportEventAction(event,
                        EventMark.isFavourite(newMark) ? "addFavourite" : "removeFavourite",
                        position);
                activity.recordEventMark(event, newMark);
                setFavouriteView(newMark);
                if (EventMark.isFavourite(newMark)) {
                    activity.showMessage("Added to My Events");
                } else {
                    activity.showMessage("Removed from My Events");
                }

            }
        });

        // Is user invited to this event ?
        if (invite != null && invite.getInvitedBy() != null) {
            invitedByView.setVisibility(View.VISIBLE);
            invitedByView.setFollowers(activity, invite.getAllInvitedBy());
            infoArrowView.setVisibility(View.VISIBLE);
        } else if (event.numViews > 5) {
            eventStatsView.setVisibility(View.VISIBLE);
            eventStatsView.setText("" + event.numViews + " views");
            infoArrowView.setVisibility(View.VISIBLE);
        }else{
            eventStatsView.setVisibility(View.VISIBLE);
            eventStatsView.setText("" + Utils.getRandomNumber(10,50) + " views");
            infoArrowView.setVisibility(View.VISIBLE);
        }

        //Share event
        share.setVisibility(View.VISIBLE);
        share.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.shareEventWithBranch(event, null, null);
            }
        });
    }

}
