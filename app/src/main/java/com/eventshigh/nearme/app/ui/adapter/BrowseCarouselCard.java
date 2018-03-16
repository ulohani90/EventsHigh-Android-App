package com.eventshigh.nearme.app.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.TopCropImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import jp.wasabeef.blurry.Blurry;

/**
 * Created by umesh on 08/11/17.
 */

public class BrowseCarouselCard extends ViewHolder {

    ViewPager cardBrowseCarousel;
    LinearLayout dotsView;

    int eventsCount;

    public static BrowseCarouselCard newInstance(BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_browse_carousel, parent, false);
        return new BrowseCarouselCard(view);
    }

    public BrowseCarouselCard(View itemView) {
        super(itemView);
        cardBrowseCarousel = (ViewPager) itemView.findViewById(R.id.browse_carousel);
        dotsView = (LinearLayout) itemView.findViewById(R.id.dots_parent);
    }

    public void populate(final BaseContextActivity activity, final List<Event> events) {
        eventsCount = events.size();
        BrowseCarouselCardAdapter adapter = new BrowseCarouselCardAdapter(activity, events);
        cardBrowseCarousel.setAdapter(adapter);
        dotsView.removeAllViews();
        if (adapter.getCount() > 1) {
            dotsView.setVisibility(View.VISIBLE);
            for (int i = 0; i < adapter.getCount(); i++) {
                View view = activity.getLayoutInflater().inflate(
                        R.layout.view_dot_featured, dotsView, false);
                view.setSelected(i == 0);
                dotsView.addView(view);
            }
        } else {
            dotsView.setVisibility(View.GONE);
        }
        cardBrowseCarousel.clearOnPageChangeListeners();
        cardBrowseCarousel.addOnPageChangeListener(new DotsSelector(activity));


        if (activity.timer == null) {
            setTimer(activity);
        }
    }

    public void setTimer(BaseActivity activity) {
        activity.timer = new Timer(); // This will create a new Thread
        activity.timer.schedule(new TimerTask() { // task to be scheduled

            @Override
            public void run() {
                System.out.println("Post Timer");
                handler.post(Update);
            }
        }, 8000, 8000);
    }

    final Handler handler = new Handler();
    final Runnable Update = new Runnable() {
        public void run() {
            int currentPage = cardBrowseCarousel.getCurrentItem();
            if (currentPage == eventsCount - 1) {
                currentPage = 0;
            } else {
                currentPage += 1;
            }
          //  System.out.println("Change Page to::" + currentPage);

            cardBrowseCarousel.setCurrentItem(currentPage, true);

        }
    };

    public class BrowseCarouselCardAdapter extends PagerAdapter {
        List<Event> events;
        BaseContextActivity activity;

        public BrowseCarouselCardAdapter(BaseContextActivity activity, List<Event> events) {
            this.activity = activity;
            this.events = new ArrayList<>(events);
        }

        @Override
        public int getCount() {
            if (events != null)
                return events.size();
            return 0;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final Event event = events.get(position);
            View view = activity.getLayoutInflater().inflate(R.layout.browse_carousel_item_layout, container, false);
            final ImageView eventImage = (ImageView) view.findViewById(R.id.event_img);

           /* Glide.with(activity).load(event.imgUrl).diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(R.drawable.eh_default_event)
                    .crossFade().centerCrop()
                    .into(eventImage);*/
            Glide.with(itemView.getContext()).load(event.imgUrl).asBitmap().dontTransform().diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(new BitmapImageViewTarget(eventImage) {
                        @Override
                        public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                            super.onResourceReady(bitmap, anim);
                            Glide.with(itemView.getContext()).load(event.imgUrl).dontTransform().into(eventImage);
                        }
                    });
            if (event.dominantBgColor != null && event.dominantBgColor.length() > 0) {
                try {
                    eventImage.setBackgroundColor(Color.parseColor("#" + getEventColor(event.dominantBgColor)));
                } catch (IllegalArgumentException e) {

                }
            }
            TextView eventTitle = (TextView) view.findViewById(R.id.event_title);
            eventTitle.setText(event.title);
            TextView eventDate = (TextView) view.findViewById(R.id.event_date);
            if (event.venue != null) {
                eventDate.setText(event.venue);
                eventDate.setVisibility(View.VISIBLE);
            } else {
                eventDate.setVisibility(View.GONE);
            }
           /* DateTimeUtils.EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
            if (eventTime != null) {
                eventDate.setVisibility(View.VISIBLE);
                eventDate.setText(eventTime.day + "," + eventTime.date);
            } else {
                eventDate.setVisibility(View.GONE);
            }*/

            view.findViewById(R.id.item_card_parent).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.showEventDetails(event, null, null);
                }
            });
            container.addView(view);
            return view;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
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

    public String getEventColor(String color) {
        StringBuilder finalColor = new StringBuilder();
        String[] colorArray = color.split(" ");
        for (String item : colorArray) {
            finalColor.append(item);
            if (item.length() < 2) {
                finalColor.append("0");
            }
        }
        return finalColor.toString();
    }

    private class DotsSelector implements ViewPager.OnPageChangeListener {
        private final BaseActivity activity;

        private DotsSelector(BaseActivity activity) {
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
            if (activity.timer != null) {
                activity.timer.cancel();
                setTimer(activity);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

}
