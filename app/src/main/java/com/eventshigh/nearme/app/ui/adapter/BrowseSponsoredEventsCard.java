package com.eventshigh.nearme.app.ui.adapter;

import android.graphics.Bitmap;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
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
import com.eventshigh.nearme.app.data.SponsoredEventObj;
import com.eventshigh.nearme.app.utils.DateTimeUtils;

import java.util.List;

/**
 * Created by umesh on 10/11/17.
 */

public class BrowseSponsoredEventsCard extends RecyclerView.ViewHolder {

    ViewPager sponsoredViewPager;


    public static BrowseSponsoredEventsCard newInstance(BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_browse_carousel, parent, false);
        return new BrowseSponsoredEventsCard(view);
    }

    public BrowseSponsoredEventsCard(View itemView) {
        super(itemView);
        sponsoredViewPager = (ViewPager) itemView.findViewById(R.id.browse_carousel);
        itemView.findViewById(R.id.dots_parent).setVisibility(View.GONE);
    }

    public void populate(final BaseContextActivity activity, final List<SponsoredEventObj> events, int width) {
        BrowseCarouselCardAdapter adapter = new BrowseCarouselCardAdapter(activity, events, width);
        sponsoredViewPager.setAdapter(adapter);
        sponsoredViewPager.setClipToPadding(false);
        sponsoredViewPager.setPadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, activity.getResources().getDisplayMetrics())
                , 0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, activity.getResources().getDisplayMetrics())
                , 0);
        sponsoredViewPager.setPageMargin((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, activity.getResources().getDisplayMetrics()));
        sponsoredViewPager.getLayoutParams().height = width;
    }

    public class BrowseCarouselCardAdapter extends PagerAdapter {
        List<SponsoredEventObj> events;
        BaseContextActivity activity;
        int width;

        public BrowseCarouselCardAdapter(BaseContextActivity activity, List<SponsoredEventObj> events, int width) {
            this.activity = activity;
            this.events = events;
            this.width = width;
        }

        @Override
        public int getCount() {
            if (events != null)
                return events.size();
            return 0;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final SponsoredEventObj event = events.get(position);

            View view = activity.getLayoutInflater().inflate(R.layout.sponsored_event_card_layout, container, false);
            AbsListView.LayoutParams params = new AbsListView.LayoutParams(width, width);
            FrameLayout parentLayout = (FrameLayout) view.findViewById(R.id.item_card_parent);
            parentLayout.setLayoutParams(params);

            final ImageView eventImage = (ImageView) view.findViewById(R.id.event_img);

            Glide.with(itemView.getContext()).load(event.bannerUrl.trim()).asBitmap().centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(new BitmapImageViewTarget(eventImage) {
                        @Override
                        public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                            super.onResourceReady(bitmap, anim);
                            Glide.with(itemView.getContext()).load(event.bannerUrl.trim()).centerCrop().into(eventImage);
                        }
                    });

            parentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (event.destinationUrl.contains("/detail/")) {
                        activity.showEventDetails(getEventId(event.destinationUrl), null, null);
                    } else {
                        activity.showSearchView(getSearchQuery(event.destinationUrl));
                    }
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
        public float getPageWidth(int position) {
            return 0.42f;
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

    public String getEventId(String url) {

        String subString = url.substring(url.lastIndexOf("/") + 1, url.length());
        int firstOccurrence = subString.indexOf("-");
        if (firstOccurrence == -1) {
            firstOccurrence = subString.indexOf("/");
        }
        if (firstOccurrence != -1)
            subString = subString.substring(0, firstOccurrence);
        return subString;

    }

    public String getSearchQuery(String url) {

        String subString = url.substring(url.lastIndexOf("/") + 1, url.length());
        int firstOccurrence = subString.indexOf("?");
        /*if (firstOccurrence == -1) {
            firstOccurrence = subString.indexOf("/");
        }*/
        if (firstOccurrence != -1)
            subString = subString.substring(0, firstOccurrence);
        subString = subString.replace("+", " ");
        return subString;

    }
}
