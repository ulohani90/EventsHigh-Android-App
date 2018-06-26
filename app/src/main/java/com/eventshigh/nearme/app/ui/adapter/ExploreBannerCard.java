package com.eventshigh.nearme.app.ui.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.CityBannerObject;

public class ExploreBannerCard extends RecyclerView.ViewHolder {

    ImageView bannerImage;

    public static ExploreBannerCard newInstance(Activity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.banner_layout_file, parent, false);
        return new ExploreBannerCard(view);
    }

    public ExploreBannerCard(View itemView) {
        super(itemView);
        bannerImage = (ImageView) itemView.findViewById(R.id.banner_img);
    }

    public void bindData(final BaseContextActivity activity, final CityBannerObject obj, int width, final String city) {
       int height = (obj.getImageHeight()*width)/obj.getImageWidth();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width,height);
        bannerImage.setLayoutParams(lp);
        Glide.with(activity).load(obj.getImgUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                .into(bannerImage);
        bannerImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.reportActionToAnalytics("homepagebannerclick", city);
                if(obj.getDestinationType().equalsIgnoreCase("detail")){
                    activity.showEventDetails(obj.geteId(), "homepagebanner",null);
                }else if(obj.getDestinationType().equalsIgnoreCase("browse")){
                    activity.showSearchView(obj.getInterest());
                }
            }
        });
    }

}
