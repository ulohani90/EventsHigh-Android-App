package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.EventCategory;

public class ExploreCategoryData implements AdapterData {
    public final String tag;
    public final BaseContextActivity activity;
    public final SocialDataProvider socialDataProvider;

    public ExploreCategoryData(String tag, BaseContextActivity activity, SocialDataProvider socialDataProvider) {
        this.tag = tag;
        this.activity = activity;
        this.socialDataProvider = socialDataProvider;
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
            Crashlytics.getInstance().core.logException(e);
        } catch (NoSuchFieldException e) {
            // Ignore
            Crashlytics.getInstance().core.logException(e);
            Log.d("", "No image for: " + tag, e);
        }

        return R.drawable.eh_default_event;
    }
}
