package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.EventFilterAttribute;

import java.util.ArrayList;

/**
 * Created by umesh on 22/12/16.
 */

public class EventFilterAttributeData implements AdapterData {

    EventFilterAttribute attribute;

    BaseActivity activity;

    ArrayList<String> selectedFilters;

    public EventFilterAttributeData(BaseActivity activity, EventFilterAttribute attribute, ArrayList<String> selectedFilters) {
        this.attribute = attribute;
        this.activity = activity;
        this.selectedFilters = selectedFilters;
    }

    @Override
    public DataType getType() {
        return DataType.EVENT_FILTER_ATTRIBUTE_CARD;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((EventFilterAttributeCard) card).bindData(activity, attribute, selectedFilters);
    }

    @Override
    public String getId() {
        return attribute.getName();
    }
}
