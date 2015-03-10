package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.user.Preferences;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

/**
 * Onboarding activity for first time users. This activity is shown to a user when the app is
 * launched and keeps showing until the user skips the onboarding steps.
 */
public class OnBoardingActivity extends BaseActivity {
    /**
     * The model that stores the resources for each onboarding step.
     */
    private class OnBoardingStepResource {
        // The text to be shown below the image in onboarding step
        private final int captionResId;

        // The image to be shown in onboarding step
        private final int imageResId;

        private OnBoardingStepResource(int captionResId, int imageResId) {
            this.captionResId = captionResId;
            this.imageResId = imageResId;
        }
    }


    // ***********************
    // Constants
    // ***********************

    private final OnBoardingStepResource[] ON_BOARDING_STEP_RESOURCES = new OnBoardingStepResource[]{
            new OnBoardingStepResource(R.string.onboarding_city, R.drawable.onboarding_city),
            new OnBoardingStepResource(R.string.onboarding_map, R.drawable.onboarding_map),
            new OnBoardingStepResource(R.string.onboarding_follow, R.drawable.onboarding_follow),
            new OnBoardingStepResource(R.string.onboarding_like, R.drawable.onboarding_like),
            new OnBoardingStepResource(R.string.onboarding_share, R.drawable.onboarding_share),
    };

    private final int NUM_ON_BOARDING_STEPS = ON_BOARDING_STEP_RESOURCES.length;

    /**
     * The view pager navigation dots shown below the view pager. A big dot indicates the step
     * that the user is currently on when swiping through the view pager content.
     */
    private LinearLayout dotsView;

    // show the coach mark for first time user has reached last step.
    private boolean showCoachMark = true;

    // User preferences.
    protected Preferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // Initialize the dotsView. Add dots to indicate the number of pages in view pager.
        dotsView = (LinearLayout) findViewById(R.id.dots_parent);
        LayoutInflater layoutInflater = getLayoutInflater();
        for (int i = 0; i < NUM_ON_BOARDING_STEPS; i++) {
            View view = layoutInflater.inflate(R.layout.viewpager_dot, dotsView, false);
            view.setSelected(i == 0);
            dotsView.addView(view);
        }

        // Initialize the The view pager which shows the onboarding steps
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(mOnBoardingStepsAdapter);
        viewPager.setOnPageChangeListener(mOnPageChangeListener);

        // Set action handler for skip and getting started button.
        findViewById(R.id.get_started).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent phoneLoginIntent = new Intent(OnBoardingActivity.this,
                        PhoneLoginActivity.class);
                phoneLoginIntent.putExtra(PhoneLoginActivity.EXTRA_IN_ONBOARDING_FLOW, true);
                startActivity(phoneLoginIntent);
                finish();
            }
        });

        // Read Preferences
        pref = Preferences.getInstance(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // We purposefully set the preference flag to not show on boarding screens
        // in onStart. If users kills the app or if app crashes for any reasons, we
        // do not show onboarding screen again.
        pref.setShowOnboarding(false);
    }


    // ***********************
    // Callbacks
    // ***********************

    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // do nothing.
        }

        @Override
        public void onPageSelected(int position) {
            if (position == NUM_ON_BOARDING_STEPS - 1 && showCoachMark) {
                dotsView.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing()) {
                                reportActionToAnalytics("onBoardingCoachHelp");
                                new ShowcaseView.Builder(OnBoardingActivity.this, true)
                                        .setTarget(new ViewTarget(findViewById(R.id.get_started)))
                                        .setContentText(R.string.onboarding_get_started)
                                        .setStyle(R.style.ShowcaseTheme)
                                        .hideOnTouchOutside()
                                        .build();
                            }
                        }
                    }, 5000);
                showCoachMark = false;
            }

            for (int i = 0 ; i < dotsView.getChildCount(); i++) {
                dotsView.getChildAt(i).setSelected(i == position);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // do nothing.
        }
    };

    /** The view pager adapter that creates the views for each onboarding step. */
    private PagerAdapter mOnBoardingStepsAdapter = new PagerAdapter() {
        @Override
        public int getCount() {
            return NUM_ON_BOARDING_STEPS;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = getLayoutInflater().inflate(R.layout.viewpager_onboarding, container, false);
            container.addView(view);
            ImageView imageView = (ImageView) view.findViewById(R.id.image);
            imageView.setImageResource(ON_BOARDING_STEP_RESOURCES[position].imageResId);
            TextView caption = (TextView) view.findViewById(R.id.caption);
            caption.setText(ON_BOARDING_STEP_RESOURCES[position].captionResId);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }
    };
}
