package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.TrendingTopic;

public class TrendingCategoryData implements AdapterData {
    public final TrendingTopic trendingTopic;
    public final BaseContextActivity activity;
    public final SocialDataProvider socialDataProvider;

    public TrendingCategoryData(TrendingTopic trendingTopic, BaseContextActivity activity,
                                SocialDataProvider socialDataProvider) {
        this.trendingTopic = trendingTopic;
        this.activity = activity;
        this.socialDataProvider = socialDataProvider;
    }

    @Override
    public DataType getType() {
        return DataType.TRENDING_CATEGORY;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, final int position) {
        ((TrendingCategoryCard) card).populateTrendingCategoryData(this);
    }

    public String getId() {
        return trendingTopic.tagName;
    }
}
