package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.EventCategory;

public class ExploreCategoryData implements AdapterData {
    public final String tag;
    public final BaseContextActivity activity;

    public ExploreCategoryData(String tag, BaseContextActivity activity) {
        this.tag = tag;
        this.activity = activity;
    }

    @Override
    public DataType getType() {
        return DataType.EXPLORE_CATEGORY;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, final int position) {
        ((TrendingCategoryCard) card).populateExploreCategoryData(this);
    }

    public String getId() {
        return tag;
    }

    public int getInfoGraphId() {
        try {
            return R.drawable.class.getField("infograph_" +
                    EventCategory.toCategoryParsableString(tag).toLowerCase()).getInt(null);
        } catch (IllegalAccessException e) {
            // Ignore
        } catch (NoSuchFieldException e) {
            // Ignore
            Log.d("", "No image for: " + tag, e);
        }

        return R.drawable.eh_default_event;
    }
}
