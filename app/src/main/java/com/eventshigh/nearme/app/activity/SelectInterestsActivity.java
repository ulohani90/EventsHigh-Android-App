package com.eventshigh.nearme.app.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.stream.EventSubcategory;
import com.eventshigh.nearme.app.ui.adapter.SelectInterestAdapter;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by umesh on 14/03/16.
 */
public class SelectInterestsActivity extends BaseActivity{



    ExpandableListView categoriesList;
    ProgressBar topProgressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_interest_layout);
        categoriesList = (ExpandableListView)findViewById(R.id.categories_list);
        topProgressBar = (ProgressBar)findViewById(R.id.top_progress_bar);

        new LoadEventsSubCategories().execute();
    }


    EventCategory[] categories;
    public class LoadEventsSubCategories extends AsyncTask<Void ,Void,HashMap<EventCategory,List<EventSubcategory>>>{

        @Override
        protected void onPreExecute() {

            topProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected HashMap<EventCategory,List<EventSubcategory>> doInBackground(Void... params) {
            categories =EventCategory.values();
            HashMap<EventCategory ,List<EventSubcategory>> subCategories = new HashMap<>();
            for(EventCategory eventCategory: categories){
                subCategories.put(eventCategory , EventSubcategory.getEventCategories(eventCategory));
            }
            return subCategories;
        }
        int previousGroup = -1;
        @Override
        protected void onPostExecute(HashMap<EventCategory, List<EventSubcategory>> eventCategoryListHashMap) {
            final SelectInterestAdapter adapter = new SelectInterestAdapter(SelectInterestsActivity.this,categories,eventCategoryListHashMap);
            categoriesList.setGroupIndicator(null);
            categoriesList.setAdapter(adapter);
            categoriesList.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
                // Keep track of previous expanded parent


                @Override
                public void onGroupExpand(int groupPosition) {
                    // Collapse previous parent if expanded.
                    if ((previousGroup != -1) && (groupPosition != previousGroup)) {
                        categoriesList.collapseGroup(previousGroup);

                    }
                    adapter.setSelectedGroup(groupPosition);
                    previousGroup = groupPosition;
                }
            });
            categoriesList.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
                @Override
                public void onGroupCollapse(int groupPosition) {
                    if (groupPosition == previousGroup) {
                        adapter.setSelectedGroup(-1);
                    }
                    previousGroup = -1;
                }
            });
            topProgressBar.setVisibility(View.GONE);
        }
    }


}
