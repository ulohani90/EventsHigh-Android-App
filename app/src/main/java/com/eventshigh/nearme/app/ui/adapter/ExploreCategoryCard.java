package com.eventshigh.nearme.app.ui.adapter;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.MovieBrowseActivity;
import com.eventshigh.nearme.app.data.EventCategory;

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

    public void bindData(final NewExploreCategoryData data) {
        if (data.tag.equalsIgnoreCase(EventCategory.NIGHTLIFE.categoryName)) {
            categoryName.setText("Parties");
        } else {
            categoryName.setText(data.tag);
        }
        Drawable drawable = data.activity.getResources().getDrawable(data.getInfoGraphId());
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        categoryName.setCompoundDrawables(drawable, null, null, null);
        categoryName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (data.tag.equalsIgnoreCase(EventCategory.MOVIES.categoryName)) {
                    Intent intent = new Intent(data.activity, MovieBrowseActivity.class);
                    data.activity.startActivity(intent);
                } else {
                    data.activity.showSearchView(data.tag);
                }
            }
        });
    }
}
