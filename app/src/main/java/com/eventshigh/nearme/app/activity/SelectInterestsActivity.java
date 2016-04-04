package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.stream.EventSubcategory;
import com.eventshigh.nearme.app.ui.adapter.SelectInterestAdapter;

import java.util.HashMap;
import java.util.List;

/**
 * Created by umesh on 14/03/16.
 */
public class SelectInterestsActivity extends BaseActivity{


    public static final String FROM_NOTIFICATION_PARAM = "is_from_notification";
    public static final String ONBOARDING_FLOW = "is_onboarding";
    ExpandableListView categoriesList;
    ProgressBar topProgressBar;
    boolean isFromNotification;


    public static final EventCategory[] categories = {
            EventCategory.EDITOR_PICKS,
            EventCategory.FREE_EVENTS,
            EventCategory.NIGHTLIFE,
            EventCategory.OUTDOORS,
            EventCategory.WORKSHOP,
            EventCategory.LIVE_PERFORMANCES,
            EventCategory.FOOD,
            EventCategory.SPORTS,
            EventCategory.HEALTH_WELLNESS,
            EventCategory.LITERATURE,
            EventCategory.KIDS_ENTERTAINMENT,
            EventCategory.ART,

    };

    Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_interest_layout);
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(Color.TRANSPARENT);
        addToolbarView(toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        categoriesList = (ExpandableListView)findViewById(R.id.categories_list);
        topProgressBar = (ProgressBar)findViewById(R.id.top_progress_bar);
        /*findViewById(R.id.done_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doneClicked();
            }
        });*/

        new LoadEventsSubCategories().execute();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (BaseActivity.NOTIFICATION_ACTION.equals(BaseActivity.NOTIFICATION_ACTION)) {
            reportActionToAnalytics("openNotification");
        }
    }

    @Override
    public View getViewForSnackbar() {
        return toolbar;
    }

    public void addToolbarView(Toolbar toolbar){
        View view = LayoutInflater.from(this).inflate(R.layout.skip_btn_layout,toolbar,false);

        if(getIntent().getBooleanExtra(ONBOARDING_FLOW,false)){
            view.findViewById(R.id.skip_btn).setVisibility(View.VISIBLE);
            view.findViewById(R.id.skip_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportActionToAnalytics("followPSkip");
                    skipClicked(v);
                }
            });
        }else if(getIntent().getBooleanExtra(FROM_NOTIFICATION_PARAM,false)){
            isFromNotification = true;
        }

        toolbar.addView(view);
    }

    public class LoadEventsSubCategories extends AsyncTask<Void ,Void,HashMap<EventCategory,List<EventSubcategory>>>{

        @Override
        protected void onPreExecute() {

            topProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected HashMap<EventCategory,List<EventSubcategory>> doInBackground(Void... params) {

            HashMap<EventCategory ,List<EventSubcategory>> subCategories = new HashMap<>();
            for(EventCategory eventCategory: categories){
                subCategories.put(eventCategory , EventSubcategory.getEventCategories(eventCategory,true));
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

    public void doneClicked(View view) {
        closeActivity(isFromNotification);
    }

    public void skipClicked(View view){
        closeActivity(isFromNotification);
    }

    public void closeActivity(boolean shouldStartHome){
        if(isFromNotification) {
            Intent intent = new Intent(this, LaunchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        finish();
    }

    public boolean isFromNotification() {
        return isFromNotification;
    }

    @Override
    public void onBackPressed() {
        closeActivity(isFromNotification);
    }
}
