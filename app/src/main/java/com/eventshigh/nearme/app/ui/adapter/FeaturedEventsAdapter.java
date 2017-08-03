package com.eventshigh.nearme.app.ui.adapter;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.user.Account;

/**
 * A {@link android.support.v4.view.PagerAdapter} which can be used to show Featured Events.
 */
public class FeaturedEventsAdapter extends PagerAdapter {
    private static final int MAX_EVENTS = 5;

    private final EventPagerData eventPagerData;

    public static final String BANNER_IMAGE_URL = "https://storage.googleapis.com/ehassets/images/banner_new.jpg";

    public FeaturedEventsAdapter(EventPagerData eventPagerData) {
        this.eventPagerData = eventPagerData;
    }

    @Override
    public int getCount() {
        return Math.min(MAX_EVENTS, eventPagerData.events.size()) +
                (eventPagerData.showReferralOffer ? 1 : 0);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (position == 0) {
            // special app invite.
            View view = eventPagerData.activity.getLayoutInflater().inflate(R.layout.card_refer, container, false);
            TextView helloUserText = (TextView) view.findViewById(R.id.hello_user_text);
            Account account = new Account(eventPagerData.activity);
            if (account.getUserInfo() != null && account.getUserInfo().name != null && account.getUserInfo().name.length() > 0) {
                helloUserText.setText("Hello " + account.getUserInfo().name);
            } else {
                helloUserText.setText("Hello");
            }
            ImageView banner = (ImageView) view.findViewById(R.id.banner_img);
            Glide.with(eventPagerData.activity).load(BANNER_IMAGE_URL).placeholder(R.drawable.eh_default_event)
                    .crossFade().centerCrop()
                    .into(banner);
           /* view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    eventPagerData.activity.reportActionToAnalytics("headerreferralclick");
                    Intent intent = new Intent(eventPagerData.activity, ReferralActivity.class);
                    eventPagerData.activity.startActivity(intent);
                }
            });*/
            container.addView(view);
            return view;
        }

        int eventIndex = position;
        if (eventIndex > 0 && eventPagerData.showReferralOffer) {
            eventIndex--;
        }
        View view = EventCard.getEventCard(eventPagerData.events.get(eventIndex),
                eventPagerData.activity, null, container, false);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    // This method is added so that {@link PagerAdapter#notifyDataSetChanged} to work.
    // {@See http://stackoverflow.com/a/7287121/4340116} for details.
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}
