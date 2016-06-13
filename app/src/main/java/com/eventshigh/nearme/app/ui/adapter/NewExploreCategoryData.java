package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.EventCategory;

/**
 * Created by umesh on 10/06/16.
 */
public class NewExploreCategoryData implements AdapterData {
    public final String tag;
    public final BaseContextActivity activity;
    public final SocialDataProvider socialDataProvider;

    public NewExploreCategoryData(String tag, BaseContextActivity activity, SocialDataProvider socialDataProvider) {
        this.tag = tag;
        this.activity = activity;
        this.socialDataProvider = socialDataProvider;
    }

    @Override
    public DataType getType() {
        return DataType.NEW_EXPLORE_CATEOGRY;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, final int position) {
        ((ExploreCategoryCard) card).bindData(this);
    }

    public String getId() {
        return tag;
    }

    public int getInfoGraphId() {
        try {
            return R.drawable.class.getField("explore_ic_" +
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
