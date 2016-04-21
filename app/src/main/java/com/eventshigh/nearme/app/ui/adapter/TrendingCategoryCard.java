package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.utils.Utils;

public class TrendingCategoryCard extends ViewHolder {
    private ImageView imageView;
    private TextView titleView;
    private FrameLayout parent;

    public static TrendingCategoryCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_explore, parent, false);
        return new TrendingCategoryCard(view);
    }

    public TrendingCategoryCard(View itemView) {
        super(itemView);

        imageView = (ImageView) itemView.findViewById(R.id.cat_image);
        titleView = (TextView) itemView.findViewById(R.id.cat_title);
        parent = (FrameLayout)itemView.findViewById(R.id.parent);


    }

    public void populateTrendingCategoryData(final TrendingCategoryData data) {
        Glide.with(data.activity).load(data.trendingTopic.imgUrl)
                .crossFade().centerCrop()
                .into(imageView);
        titleView.setText(data.trendingTopic.tagName);
        itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                data.trendingTopic.launch(data.activity);
            }
        });

        Utils.waitForViewVisible(imageView, new Runnable() {
            @Override
            public void run() {
                LayoutParams lp = imageView.getLayoutParams();
                lp.height = 3 * imageView.getWidth() / 4;
                imageView.setLayoutParams(lp);
                LayoutParams lp1 = parent.getLayoutParams();
                lp1.height = 3 * parent.getWidth() / 4;
                parent.setLayoutParams(lp1);
                // parent.postInvalidate();

            }
        });
    }

    public void populateExploreCategoryData(final ExploreCategoryData data) {
        imageView.setImageResource(data.getInfoGraphId());
        titleView.setVisibility(View.GONE);
        itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                data.activity.showSearchView(data.tag);
            }
        });

        Utils.waitForViewVisible(imageView, new Runnable() {
            @Override
            public void run() {
                LayoutParams lp = imageView.getLayoutParams();
                lp.height = imageView.getWidth();
                imageView.setLayoutParams(lp);
                // parent.postInvalidate();
            }
        });
    }
}

