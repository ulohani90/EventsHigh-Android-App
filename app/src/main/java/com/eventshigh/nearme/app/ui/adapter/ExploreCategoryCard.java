package com.eventshigh.nearme.app.ui.adapter;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;

/**
 * Created by umesh on 10/06/16.
 */
public class ExploreCategoryCard extends RecyclerView.ViewHolder {


    TextView categoryName;

    public static ExploreCategoryCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.explore_category_button_layout, parent, false);
        return new ExploreCategoryCard(view);
    }


    public ExploreCategoryCard(View itemView) {
        super(itemView);
        categoryName = (TextView) itemView.findViewById(R.id.category_text);
    }

    public void bindData(NewExploreCategoryData data) {
        categoryName.setText(data.tag);
        Drawable drawable = data.activity.getResources().getDrawable(data.getInfoGraphId());
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        categoryName.setCompoundDrawables(drawable, null, null, null);
    }
}
