package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseEventsFragment;
import com.eventshigh.nearme.app.data.TrendingTopic;

public class TrendingCategoryData implements AdapterData {
    public final TrendingTopic trendingTopic;
    public final BaseEventsFragment eventsFragment;
    public final SocialDataProvider socialDataProvider;

    public TrendingCategoryData(TrendingTopic trendingTopic, BaseEventsFragment eventsFragment, SocialDataProvider socialDataProvider) {
        this.trendingTopic = trendingTopic;
        this.eventsFragment = eventsFragment;
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
