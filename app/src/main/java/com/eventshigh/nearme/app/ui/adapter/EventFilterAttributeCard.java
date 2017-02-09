package com.eventshigh.nearme.app.ui.adapter;


import android.app.Activity;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.EventFilterAttribute;

import java.util.ArrayList;

/**
 * Created by umesh on 22/12/16.
 */

public class EventFilterAttributeCard extends ViewHolder {


    ImageView selectIcon;
    TextView filterName;

    LinearLayout attributeParent;

    public static EventFilterAttributeCard newInstance(Activity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.event_attribute_filter_layout, parent, false);
        return new EventFilterAttributeCard(view);
    }

    public EventFilterAttributeCard(View itemView) {
        super(itemView);
        selectIcon = (ImageView) itemView.findViewById(R.id.follow_icon);
        filterName = (TextView) itemView.findViewById(R.id.filter_name);
        attributeParent = (LinearLayout) itemView.findViewById(R.id.attribute_parent);
    }

    public void bindData(BaseActivity activity, final EventFilterAttribute attribute, final ArrayList<String> selectedFilters) {

        filterName.setText(attribute.getName());
        if (selectedFilters != null && selectedFilters.contains(attribute.getName())) {
            attributeParent.setSelected(true);
            selectIcon.setImageResource(R.drawable.ic_check_circle_green_24dp);
            filterName.setSelected(true);
        } else {
            attributeParent.setSelected(false);
            selectIcon.setImageResource(R.drawable.ic_add_circle_outline_black_24dp);
            filterName.setSelected(false);
        }
        attributeParent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (attributeParent.isSelected()) {
                    attributeParent.setSelected(false);
                    selectedFilters.remove(attribute.getName());
                    selectIcon.setImageResource(R.drawable.ic_add_circle_outline_black_24dp);
                    filterName.setSelected(false);
                } else {
                    attributeParent.setSelected(true);
                    selectedFilters.add(attribute.getName());
                    selectIcon.setImageResource(R.drawable.ic_check_circle_green_24dp);
                    filterName.setSelected(true);
                }
            }
        });
    }
}
