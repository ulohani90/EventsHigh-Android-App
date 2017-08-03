package com.eventshigh.nearme.app.ui.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.ui.FBSigninDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.ContactListView;

import java.util.Set;

public class EventCard extends ViewHolder {
    private final boolean shouldAdjustImageHeight;
    private final ImageView bgView;
    private final ImageView recommendedView;
    private final TextView titleView;
    private final ImageView favouriteView;
    //private final TextView eventTimeView;
    private final TextView eventDay;
    private final TextView eventDate;
    private final TextView eventTimeText;
    private final LinearLayout eventTimeLayout;
    private final View eventTimeSeparator;
    private final TextView priceView;
    private final TextView venueView;
    private final TextView travelTimeView;
    private final View arrowView;
    private final TextView eventStatsView;

    private final View infoArrowView;
    private final ImageView share;
    private final LinearLayout cardParent;
    private final LinearLayout eventInfo;
    private final boolean addShadow;
    private final TextView eventLocation;
    private final ImageView img1, img2;
    private final ContactListView contactListView;
    private final LinearLayout statsLayout;
    private final ImageView trustedPartner;
    private final TextView sponsoredEvent;
    private final TextView discountTag;


    public static EventCard newInstance(Activity activity, ViewGroup parent,
                                        boolean shouldAdjustImageHeight, boolean isAddShadow) {
        View view = activity.getLayoutInflater().inflate(R.layout.new_card_event, parent, false);
        return new EventCard(view, shouldAdjustImageHeight, isAddShadow);
    }

    // Build the view, reuse existing if possible.
    public static View getEventCard(final Event event, final BaseContextActivity activity,
                                    @Nullable View reuseView, ViewGroup parent, boolean isAddShadow) {
        EventCard card = reuseView != null ? new EventCard(reuseView, true, isAddShadow) :
                newInstance(activity, parent, true, isAddShadow);
        card.bindEventView(event, activity, 0, null);
        return card.itemView;
    }

    public EventCard(View cardView, boolean shouldAdjustImageHeight, boolean isAddShadow) {
        super(cardView);

        this.shouldAdjustImageHeight = shouldAdjustImageHeight;
        bgView = (ImageView) cardView.findViewById(R.id.event_bg);
        recommendedView = (ImageView) cardView.findViewById(R.id.event_recommended);
        eventLocation = (TextView) cardView.findViewById(R.id.event_location);
        titleView = (TextView) cardView.findViewById(R.id.event_title);
        favouriteView = (ImageView) cardView.findViewById(R.id.action_favourite);
        // eventTimeView = (TextView) cardView.findViewById(R.id.event_time);
        eventDay = (TextView) cardView.findViewById(R.id.event_day);
        eventDate = (TextView) cardView.findViewById(R.id.event_date);
        eventTimeText = (TextView) cardView.findViewById(R.id.event_time);
        eventTimeLayout = (LinearLayout) cardView.findViewById(R.id.event_time_layout);
        eventTimeSeparator = cardView.findViewById(R.id.event_time_separator);
        priceView = (TextView) cardView.findViewById(R.id.event_price);
        venueView = (TextView) cardView.findViewById(R.id.event_venue);
        travelTimeView = (TextView) cardView.findViewById(R.id.event_travel_time);
        arrowView = cardView.findViewById(R.id.arrow);
        eventStatsView = (TextView) cardView.findViewById(R.id.event_stats);
        infoArrowView = cardView.findViewById(R.id.info_arrow);
        share = (ImageView) cardView.findViewById(R.id.share);
        cardParent = (LinearLayout) cardView.findViewById(R.id.card_parent);
        eventInfo = (LinearLayout) cardView.findViewById(R.id.event_info);
        img1 = (ImageView) cardView.findViewById(R.id.img1);
        img2 = (ImageView) cardView.findViewById(R.id.img2);
        contactListView = (ContactListView) cardView.findViewById(R.id.followed_by);
        statsLayout = (LinearLayout) cardView.findViewById(R.id.stats_parent);
        trustedPartner = (ImageView) cardView.findViewById(R.id.trusted_partner);
        sponsoredEvent = (TextView) cardView.findViewById(R.id.is_sponsered_event);
        addShadow = isAddShadow;
        discountTag = (TextView) cardView.findViewById(R.id.event_discount);
    }


    public void setFavouriteView(@Nullable EventMark eventMark) {


        favouriteView.setTag(eventMark);
        favouriteView.setImageResource(EventMark.isFavourite(eventMark) ?
                R.drawable.ic_favorite_red_18dp : R.drawable.ic_favorite_border_black_18dp);

    }

    @SuppressLint("SetTextI18n")
    public void bindEventView(final Event event, boolean isFirstEvent, final int position,
                              final BaseContextActivity activity,
                              Set<SocialFriend> likedBy) {
        bindEventView(event, activity, position, null);
        eventInfo.setVisibility(View.VISIBLE);
        arrowView.setVisibility(isFirstEvent ? View.VISIBLE : View.GONE);

        // Set the travel time.
        Account account = new Account(activity);
        if (account.getLastLocality() != null) {
            String travelTime = LocationUtils.getTravelTime(activity, account.getLastLocality().getLatLng(), event.location);
            if (travelTime != null) {
                travelTimeView.setText(travelTime);
                travelTimeView.setVisibility(View.VISIBLE);
            } else {
                travelTimeView.setVisibility(View.GONE);
            }
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
                if (newMark == EventMark.FAVOURITE && !(new Account(activity).getUserInfo().isSignedIn)) {
                    FBSigninDialog.show(activity, R.string.ui_signin_via_fb, R.string.ui_signin_fb_plan_more, 0);
                }

                activity.reportEventAction(event,
                        EventMark.isFavourite(newMark) ? "addFavourite" : "removeFavourite",
                        position);
                activity.recordEventMark(event, newMark, false);

                setFavouriteView(newMark);
                if (EventMark.isFavourite(newMark)) {
                    activity.showMessage("Added to My Events");
                } else {
                    activity.showMessage("Removed from My Events");
                }

            }
        });

        // Is user invited to this event ?


        if (likedBy != null && likedBy.size() > 0) {

            statsLayout.setVisibility(View.VISIBLE);
            img1.setVisibility(View.GONE);
            img2.setVisibility(View.GONE);
            contactListView.setVisibility(View.VISIBLE);
            contactListView.setFollowers(
                    activity, likedBy);
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
            builder.append(event.numViews > 0 ? (event.numViews + " people interested") : "interested");
            eventStatsView.setText(builder.toString());
        } else {
            if (event.numViews > 0) {
                statsLayout.setVisibility(View.VISIBLE);
                img1.setVisibility(View.VISIBLE);
                img2.setVisibility(View.VISIBLE);
                int imgResource1;
                if (img1.getTag() != null) {
                    imgResource1 = (int) img1.getTag();
                    img1.setImageResource(imgResource1);
                } else {
                    imgResource1 = Utils.getDummyImageResource();
                    img1.setTag(imgResource1);
                    img1.setImageResource(imgResource1);
                }

                int imgResource2;
                if (img2.getTag() != null) {
                    imgResource2 = (int) img2.getTag();
                    img2.setImageResource(imgResource2);
                } else {
                    imgResource2 = getRandomImageResource(imgResource1);
                    img2.setTag(imgResource2);
                    img2.setImageResource(imgResource2);
                }


                StringBuilder builder = new StringBuilder();
                builder.append(event.numViews + " people interested");
                eventStatsView.setText(builder.toString());
            } else {
                statsLayout.setVisibility(View.GONE);
            }
        }

        // ((View) eventStatsView.getParent()).setVisibility(View.VISIBLE);
        eventStatsView.setVisibility(View.VISIBLE);


        //Share event
        share.setVisibility(View.VISIBLE);
        share.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.shareEvent(event, null, null);
            }
        });
    }

    public void bindEventView(final Event event, final BaseContextActivity activity, final int position, final EventsAdapter.OnItemClickedListener listener) {
        itemView.setVisibility(View.VISIBLE);
        itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null)
                    listener.onItemClicked(position);
                Bundle bundle = new Bundle();
                if (img1.getTag() != null) {
                    bundle.putInt("resource_1", (int) img1.getTag());
                }
                if (img2.getTag() != null) {
                    bundle.putInt("resource_2", (int) img2.getTag());
                }
                activity.showEventDetailsWithUserImages(event, "", null, bundle);
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
        EventTime lastEventTime = null;
        if (event.eventTimings.size() > 1) {
            lastEventTime = DateTimeUtils.getEventTime(event, event.eventTimings.size() - 1);
        }
        if (eventTime == null) {
            eventTimeLayout.setVisibility(View.GONE);
            eventTimeSeparator.setVisibility(View.GONE);
            //  eventTimeView.setVisibility(View.GONE);
        } else {
            eventTimeLayout.setVisibility(View.VISIBLE);
            eventTimeSeparator.setVisibility(View.VISIBLE);
            //eventTimeView.setVisibility(View.VISIBLE);
            eventDay.setText(eventTime.day.toUpperCase());
            eventDate.setText(eventTime.date);
            eventTimeText.setText(eventTime.time);
            /*if (lastEventTime == null) {

                eventTimeView.setText(eventTime.toString());
            } else {
                SpannableString dateString = new SpannableString(eventTime.toString() + " - " + lastEventTime.toString());
                dateString.setSpan(new StyleSpan(Typeface.BOLD), eventTime.toString().length(), eventTime.toString().length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                eventTimeView.setText(dateString);
            }*/

        }

        if (event.venue != null) {
            eventLocation.setVisibility(View.VISIBLE);
            eventLocation.setText(event.venue);
        } else {
            eventLocation.setVisibility(View.GONE);
        }


        // Set the price.

        String priceString = event.getPriceString();
        if (priceString == null) {
            priceView.setVisibility(View.GONE);
        } else {
            priceView.setVisibility(View.VISIBLE);
            priceView.setText(priceString);
        }
        if (event.discountPercentageText != null) {
            discountTag.setVisibility(View.VISIBLE);
            discountTag.setText(event.discountPercentageText);
        } else if (event.discountPercentage != null) {
            discountTag.setVisibility(View.VISIBLE);
            discountTag.setText(event.discountPercentage + "% OFF");
        } else {
            discountTag.setVisibility(View.GONE);

        }

        //
        if (addShadow) {
            venueView.setText(event.getShortAddress());
            Drawable drawable = activity.getResources().getDrawable(R.drawable.ic_location_on_white_12dp);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            venueView.setCompoundDrawables(drawable, null, null, null);
            venueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            cardParent.setBackground(activity.getResources().getDrawable(R.drawable.card_item_bg));
        } else {
            venueView.setText(event.title);
            venueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            venueView.setCompoundDrawables(null, null, null, null);
            cardParent.setBackground(null);
        }

        // Set the venue.
        eventInfo.setVisibility(View.GONE);


        arrowView.setVisibility(View.GONE);
        favouriteView.setVisibility(View.GONE);
        travelTimeView.setVisibility(View.GONE);

        share.setVisibility(View.GONE);

        infoArrowView.setVisibility(View.GONE);

        if (event.isPrimaryOrganizer) {
            trustedPartner.setVisibility(View.VISIBLE);
        } else {
            trustedPartner.setVisibility(View.GONE);
        }
        if (event.isSponsoredEvent) {
            sponsoredEvent.setVisibility(View.VISIBLE);
        } else {
            sponsoredEvent.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SetTextI18n")
    public void bindEventView(final Event event, boolean isFirstEvent, final int position,
                              final BaseContextActivity activity,
                              @Nullable Set<SocialFriend> likedBy, EventsAdapter.OnItemClickedListener listener) {

        bindEventView(event, activity, position, listener);
        eventInfo.setVisibility(View.VISIBLE);
        arrowView.setVisibility(isFirstEvent ? View.VISIBLE : View.GONE);
        Account account = new Account(activity);
        // Set the travel time.
        if (account.getLastLocality() != null) {
            String travelTime = LocationUtils.getTravelTime(activity, account.getLastLocality().getLatLng(), event.location);
            if (travelTime != null) {
                travelTimeView.setText(travelTime);
                travelTimeView.setVisibility(View.VISIBLE);
            } else {
                travelTimeView.setVisibility(View.GONE);
            }
        } else if (activity.getUserLocation() != null) {
            String travelTime = LocationUtils.getTravelTime(activity, activity.getUserLocation(), event.location);
            if (travelTime != null) {
                travelTimeView.setText(travelTime);
                travelTimeView.setVisibility(View.VISIBLE);
            } else {
                travelTimeView.setVisibility(View.GONE);
            }
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
                if (newMark == EventMark.FAVOURITE && !(new Account(activity).getUserInfo().isSignedIn)) {
                    FBSigninDialog.show(activity, R.string.ui_signin_via_fb, R.string.ui_signin_fb_plan_more, 0);
                }

                activity.reportEventAction(event,
                        EventMark.isFavourite(newMark) ? "addFavourite" : "removeFavourite",
                        position);
                activity.recordEventMark(event, newMark, false);
                setFavouriteView(newMark);
                if (EventMark.isFavourite(newMark)) {
                    activity.showMessage("Added to My Events");
                } else {
                    activity.showMessage("Removed from My Events");
                }

            }
        });

        // Is user invited to this event ?
        /*if (invite != null && invite.getInvitedBy() != null) {
            invitedByView.setVisibility(View.VISIBLE);
            invitedByView.setFollowers(activity, invite.getAllInvitedBy());
            infoArrowView.setVisibility(View.GONE);
        } else if (event.numViews > 5) {
            eventStatsView.setVisibility(View.VISIBLE);
            eventStatsView.setText("" + event.numViews + " views");
            infoArrowView.setVisibility(View.GONE);
        } else {
            eventStatsView.setVisibility(View.VISIBLE);
            eventStatsView.setText("" + Utils.getRandomNumber(10, 50) + " views");
            infoArrowView.setVisibility(View.GONE);
        }*/
        if (likedBy != null && likedBy.size() > 0) {
            statsLayout.setVisibility(View.VISIBLE);
            img1.setVisibility(View.GONE);
            img2.setVisibility(View.GONE);
            contactListView.setVisibility(View.VISIBLE);
            contactListView.setFollowers(
                    activity, likedBy);
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
            eventStatsView.setText(builder.toString());
        } else {
            if (event.numViews > 0) {
                statsLayout.setVisibility(View.VISIBLE);
                contactListView.setVisibility(View.GONE);
                img1.setVisibility(View.VISIBLE);
                img2.setVisibility(View.VISIBLE);

                int imgResource1;
                if (img1.getTag() != null) {
                    imgResource1 = (int) img1.getTag();
                    img1.setImageResource(imgResource1);
                } else {
                    imgResource1 = Utils.getDummyImageResource();
                    img1.setTag(imgResource1);
                    img1.setImageResource(imgResource1);
                }

                int imgResource2;
                if (img2.getTag() != null) {
                    imgResource2 = (int) img2.getTag();
                    img2.setImageResource(imgResource2);
                } else {
                    imgResource2 = getRandomImageResource(imgResource1);
                    img2.setTag(imgResource2);
                    img2.setImageResource(imgResource2);
                }
                StringBuilder builder = new StringBuilder();
                builder.append(event.numViews + " people interested");
                eventStatsView.setText(builder.toString());
            } else {
                contactListView.setVisibility(View.GONE);
                statsLayout.setVisibility(View.GONE);
            }
        }

        // ((View) eventStatsView.getParent()).setVisibility(View.VISIBLE);
        eventStatsView.setVisibility(View.VISIBLE);
        //Share event
        share.setVisibility(View.VISIBLE);
        share.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.shareEvent(event, null, null);
            }
        });
    }


    public int getRandomImageResource(int num) {
        int newNum = Utils.getDummyImageResource();
        if (newNum == num) {
            newNum = Utils.getDummyImageResource();
        }
        return newNum;
    }


}
