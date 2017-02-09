package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseContextActivity;

/**
 * Created by umesh on 11/06/16.
 */
public class ExploreCategoryHeaderData implements AdapterData {

    BaseContextActivity activity;

    boolean showNewYearImage;

    public ExploreCategoryHeaderData(BaseContextActivity activity, boolean showNewYearImage) {
        this.activity = activity;
        this.showNewYearImage = showNewYearImage;
    }

    @Override
    public DataType getType() {
        return DataType.EXPLORE_CATEGORY_HEADER;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((ExploreCategoriesHeaderCard) card).bindData(activity, showNewYearImage);
    }

    @Override
    public String getId() {
        return "header";
    }
}
