package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.CityBannerObject;

public class ExploreBannerData implements AdapterData {

    private BaseContextActivity activity;

    private CityBannerObject cityBannerObject;

    private int width;

    private String city;

    public ExploreBannerData(BaseContextActivity activity, CityBannerObject cityBannerObject, int width, String city) {
        this.activity = activity;
        this.cityBannerObject = cityBannerObject;
        this.width = width;
        this.city = city;
    }

    @Override
    public DataType getType() {
        return DataType.EXPLORE_BANNER_CARD;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((ExploreBannerCard) (card)).bindData(activity, cityBannerObject, width, city);
    }

    @Override
    public String getId() {
        return cityBannerObject.getImgUrl();
    }
}
