package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.stream.EventSubcategory;
import com.eventshigh.nearme.app.network.TagsSuggestRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.EventSearchSuggestionsProvider;
import com.eventshigh.nearme.app.ui.adapter.SearchInterestResultAdapter;
import com.eventshigh.nearme.app.ui.adapter.SelectInterestAdapter;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.view.ZCustomFlowLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by umesh on 14/03/16.
 */
public class SelectInterestsActivity extends BaseActivity {


    public static final String FROM_NOTIFICATION_PARAM = "is_from_notification";
    public static final String ONBOARDING_FLOW = "is_onboarding";
    ExpandableListView categoriesList;
    ProgressBar topProgressBar;
    boolean isFromNotification;

    int minFlowLayoutHeight;

    LinearLayout selectedCategoryContainer;


    public static final EventCategory[] categories = {
            EventCategory.EDITOR_PICKS,
            EventCategory.FREE_EVENTS,
            EventCategory.NIGHTLIFE,
            EventCategory.OUTDOORS,
            EventCategory.LIVE_PERFORMANCES,
            EventCategory.FOOD,
            EventCategory.SPORTS,
            EventCategory.WORKSHOPS,
            EventCategory.HEALTH_WELLNESS,
            EventCategory.KIDS_ENTERTAINMENT,
            EventCategory.TECH,
            EventCategory.ART,

    };

    Toolbar toolbar;

    ZCustomFlowLayout selectedCategoryFlowLayout;

    Account account;

    boolean isOnboarding;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_interest_layout);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        account = new Account(this);
        isOnboarding = getIntent().getBooleanExtra(ONBOARDING_FLOW, false);

        addToolbarView(toolbar);

        setSupportActionBar(toolbar);
        if (!isOnboarding) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        categoriesList = (ExpandableListView) findViewById(R.id.categories_list);
        topProgressBar = (ProgressBar) findViewById(R.id.top_progress_bar);
        minFlowLayoutHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 54, getResources().getDisplayMetrics());

        loadTags();

    }

    List<String> allTags;

    public void loadTags() {
        topProgressBar.setVisibility(View.VISIBLE);
        TagsSuggestRequest requestTags = new TagsSuggestRequest(mTagSuggestListener, mTagErrorListener);
        VolleyHelper.addToRequestQueue(this, requestTags);

    }

    private Response.Listener<List<String>> mTagSuggestListener = new Response.Listener<List<String>>() {
        @Override
        public void onResponse(List<String> tagEvents, boolean isIntermediate) {
            allTags = tagEvents;
            topProgressBar.setVisibility(View.GONE);
            addHeaderViewToList();
            makeCategoryData();

        }
    };

    private Response.ErrorListener mTagErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            loadTags();
        }
    };

    ImageView expandSelectedInterest;

    public void addHeaderViewToList() {
        View view = LayoutInflater.from(this).inflate(R.layout.selected_interest_container_layout, categoriesList, false);
        final AutoCompleteTextView textView = (AutoCompleteTextView) view.findViewById(R.id.search_interest_actv);
        SearchInterestResultAdapter searchAdapter = new SearchInterestResultAdapter(this, account, (ArrayList<String>) allTags);
        textView.setAdapter(searchAdapter);
        textView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    textView.clearFocus();
                    textView.setText("");
                    ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(textView.getWindowToken(), 0);

                }

                return false;
            }
        });
        searchAdapter.setOnTagFollowedListener(new SearchInterestResultAdapter.OnTagFollowedListener() {
            @Override
            public void onTagFollowed(String tag) {
                if (!tags.contains(tag)) {
                    tags.add(tag);
                    reportActionToAnalytics("remove_tag", tag);
                    selectedCategoryFlowLayout.setRecipientForSelectedInterest(tags);
                    checkTagsCount();
                }
            }
        });
        selectedCategoryContainer = (LinearLayout) view.findViewById(R.id.selected_category_container);
        selectedCategoryFlowLayout = (ZCustomFlowLayout) view.findViewById(R.id.selected_category_flowlayout);
        expandSelectedInterest = (ImageView) view.findViewById(R.id.expand_selected_interests);
        expandSelectedInterest.setSelected(true);
        expandSelectedInterest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandSelectedInterest.isSelected()) {
                    reportActionToAnalytics("collapse_categories");
                    selectedCategoryFlowLayout.setMeasureActualHeight(false);
                    collapse(selectedCategoryFlowLayout);
                    expandSelectedInterest.setSelected(false);

                } else {
                    reportActionToAnalytics("expand_categories");
                    selectedCategoryFlowLayout.setMeasureActualHeight(true);
                    expand(selectedCategoryFlowLayout);
                    expandSelectedInterest.setSelected(true);

                }
            }
        });
        selectedCategoryFlowLayout.setOnRecipientRemoveListener(new ZCustomFlowLayout.OnRecipientRemoveListener() {
            @Override
            public void refreshRecipientNames(String tag) {
                tags.remove(tag);
                account.setIsFollowing(tag, false);
                adapter.notifyDataSetChanged();
                checkTagsCount();
                // selectedCategoryFlowLayout.setRecipientForSelectedInterest(tags);
            }
        });

        categoriesList.addHeaderView(view);
    }

    public void checkTagsCount() {
        if (tags.size() > 2) {
            selectedCategoryContainer.setVisibility(View.VISIBLE);
            expandSelectedInterest.setVisibility(View.VISIBLE);
        } else if (tags.size() == 0) {
            selectedCategoryContainer.setVisibility(View.GONE);
            expandSelectedInterest.setVisibility(View.GONE);
        } else {
            selectedCategoryContainer.setVisibility(View.VISIBLE);
            expandSelectedInterest.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getIntent() != null && getIntent().getAction() != null && getIntent().getAction().equals(BaseActivity.NOTIFICATION_ACTION)) {
            reportActionToAnalytics("openNotification");
        }
    }

    @Override
    public View getViewForSnackbar() {
        return toolbar;
    }

    public void addToolbarView(Toolbar toolbar) {
        View view = LayoutInflater.from(this).inflate(R.layout.skip_btn_layout, toolbar, false);

        if (getIntent().getBooleanExtra(FROM_NOTIFICATION_PARAM, false)) {
            isFromNotification = true;
        }

        toolbar.addView(view);
    }

    ArrayList<String> tags = new ArrayList<>();

    SelectInterestAdapter adapter;

    public void makeCategoryData() {
        topProgressBar.setVisibility(View.VISIBLE);
        HashMap<EventCategory, List<EventSubcategory>> subCategories = new HashMap<>();
        for (EventCategory eventCategory : categories) {
            if (account.isFollowing(eventCategory.categoryName)) {
                //  tags.add(eventCategory.categoryName);
            }
            subCategories.put(eventCategory, getEventCategories(eventCategory));
        }


        adapter = new SelectInterestAdapter(SelectInterestsActivity.this, categories, subCategories, new SelectInterestAdapter.OnFollowUnfollowOptionClick() {
            @Override
            public void onInterestClick(String tag, boolean isAdding) {
                if (isAdding) {
                    if (!tags.contains(tag)) {
                        tags.add(tag);
                        selectedCategoryFlowLayout.setRecipientForSelectedInterest(tags);
                        checkTagsCount();
                    }
                } else {
                    if (tags.contains(tag)) {
                        tags.remove(tag);
                        selectedCategoryFlowLayout.setRecipientForSelectedInterest(tags);
                        checkTagsCount();
                    }
                }
            }
        });
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

        tags.addAll(account.getFollowingInterests());
        topProgressBar.setVisibility(View.GONE);
        selectedCategoryFlowLayout.setMeasureActualHeight(true);
        selectedCategoryFlowLayout.setRecipientForSelectedInterest(tags);
        checkTagsCount();
    }

    int previousGroup = -1;

   /* public class LoadEventsSubCategories extends AsyncTask<Void, Void, HashMap<EventCategory, List<EventSubcategory>>> {

        @Override
        protected void onPreExecute() {
            topProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected HashMap<EventCategory, List<EventSubcategory>> doInBackground(Void... params) {

            HashMap<EventCategory, List<EventSubcategory>> subCategories = new HashMap<>();
            for (EventCategory eventCategory : categories) {
                subCategories.put(eventCategory, EventSubcategory.getEventCategories(eventCategory, true));
            }
            return subCategories;
        }


        @Override
        protected void onPostExecute(HashMap<EventCategory, List<EventSubcategory>> eventCategoryListHashMap) {
            final SelectInterestAdapter adapter = new SelectInterestAdapter(SelectInterestsActivity.this, categories, eventCategoryListHashMap);
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
    }*/

    public void doneClicked(View view) {
        if (isFromNotification) {
            reportActionToAnalytics("donePClickedNotif");
        } else {
            reportActionToAnalytics("donePClicked");
        }
        //Remove the next lines and call closeActivity directly
       /* Intent intent = new Intent(this, LaunchActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);*/
        closeActivity(isFromNotification);
    }

    public void skipClicked(View view) {
        closeActivity(isFromNotification);
    }

    public void launchNextActivity() {
        Intent phoneLoginIntent = new Intent(this, PhoneLoginActivity.class);
        startActivity(phoneLoginIntent);
        finish();
    }

    public void closeActivity(boolean shouldStartHome) {
        if (isFromNotification) {
            Intent intent = new Intent(this, LaunchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (isOnboarding) {
            launchNextActivity();

        } else {
            finish();
        }

    }

    public boolean isFromNotification() {
        return isFromNotification;
    }

    @Override
    public void onBackPressed() {
        closeActivity(isFromNotification);
    }

    public List<EventSubcategory> getEventCategories(EventCategory category) {
        List<EventSubcategory> subCategories = new ArrayList<>();

        for (EventSubcategory subcategory : EventSubcategory.values()) {
            if (subcategory.category == category) {
                if (account.isFollowing(subcategory.name)) {
                    // tags.add(subcategory.name);
                }
                subCategories.add(subcategory);

            }
        }

        return subCategories;
    }

    public void expand(final View v) {

       /* v.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(selectedCategoryFlowLayout.getActualHeight(), View.MeasureSpec.EXACTLY));*/
        //v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = selectedCategoryFlowLayout.getActualHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        // v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : minFlowLayoutHeight + (int) ((targetHeight - minFlowLayoutHeight) * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    public void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();
        final int finalHeight = minFlowLayoutHeight;

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    //v.setVisibility(View.GONE);

                } else {
                    v.getLayoutParams().height = initialHeight - (int) ((initialHeight - finalHeight) * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }
}
