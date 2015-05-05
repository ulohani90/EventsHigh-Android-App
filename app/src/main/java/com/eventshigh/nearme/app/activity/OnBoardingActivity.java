package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.animation.FakeDragAnimation;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.ImageUtils;

/**
 * Onboarding activity for first time users. This activity is shown to a user when the app is
 * launched and keeps showing until the user skips the onboarding steps.
 */
public class OnBoardingActivity extends BaseActivity {
    /**
     * The model that stores the resources for each onboarding step.
     */
    private class OnBoardingStepResource {
        // The title to be shown below the image
        private final int titleId;

        // The text to be shown below the image in onboarding step
        private final int captionResId;

        // The image to be shown in onboarding step
        private final Bitmap imageBitmap;

        private OnBoardingStepResource(int titleId, int captionResId, int imageResId) {
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int imageWidth = displayMetrics.widthPixels / 4;
            int imageHeight = displayMetrics.heightPixels / 4;

            this.titleId = titleId;
            this.captionResId = captionResId;
            this.imageBitmap = ImageUtils.decodeSampledBitmapFromResource(getResources(),
                imageResId, imageWidth, imageHeight);
        }
    }


    private OnBoardingStepResource[] onBoardingStepResources;

    /**
     * The view pager navigation dots shown below the view pager. A big dot indicates the step
     * that the user is currently on when swiping through the view pager content.
     */
    private LinearLayout dotsView;
    private ImageView imageView;

    // User preferences.
    protected Preferences pref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        onBoardingStepResources = new OnBoardingStepResource[]{
            new OnBoardingStepResource(R.string.onboarding_title_details,
                R.string.onboarding_details, R.drawable.onboarding_details),
            new OnBoardingStepResource(R.string.onboarding_title_favorites,
                R.string.onboarding_favorites, R.drawable.onboarding_favorites),
            new OnBoardingStepResource(R.string.onboarding_title_notification,
                R.string.onboarding_notification, R.drawable.onboarding_notification),
        };

        // Initialize the dotsView. Add dots to indicate the number of pages in view pager.
        dotsView = (LinearLayout) findViewById(R.id.dots_parent);
        LayoutInflater layoutInflater = getLayoutInflater();
        for (int i = 0; i < onBoardingStepResources.length; i++) {
            View view = layoutInflater.inflate(R.layout.viewpager_dot, dotsView, false);
            view.setSelected(i == 0);
            dotsView.addView(view);
        }

        imageView = (ImageView) findViewById(R.id.screenshot);

        // Initialize the The view pager which shows the onboarding steps
        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(mOnBoardingStepsAdapter);
        viewPager.setOnPageChangeListener(mOnPageChangeListener);

        // Set action handler for skip and getting started button.
        findViewById(R.id.skip).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                launchNextActivity();
            }
        });

        findViewById(R.id.next).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewPager.getCurrentItem() == onBoardingStepResources.length - 1) {
                    launchNextActivity();
                } else {
                    Animation animation = new FakeDragAnimation(viewPager,
                        -viewPager.getMeasuredWidth());
                    animation.setDuration(500);
                    viewPager.startAnimation(animation);
                }
            }
        });

        // Read Preferences
        pref = Preferences.getInstance(this);
    }

    private void launchNextActivity() {
        Intent phoneLoginIntent = new Intent(OnBoardingActivity.this,
            PhoneLoginActivity.class);
        phoneLoginIntent.putExtra(PhoneLoginActivity.EXTRA_IN_ONBOARDING_FLOW, true);
        startActivity(phoneLoginIntent);
        finish();
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
        private int currentImageIndex = -1;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            int imageIndex = (int) (position + 2 * positionOffset);
            float ratio = Math.abs(2 * positionOffset - 1);
            imageView.setAlpha(ratio);
            imageView.setTranslationY(100 * (1 - ratio));
            if (imageIndex != currentImageIndex) {
                imageView.setImageBitmap(onBoardingStepResources[imageIndex].imageBitmap);
                currentImageIndex = imageIndex;
            }
        }

        @Override
        public void onPageSelected(int position) {
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
            return  onBoardingStepResources.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = getLayoutInflater().inflate(R.layout.viewpager_onboarding, container, false);
            container.addView(view);
            TextView title = (TextView) view.findViewById(R.id.title);
            title.setText(onBoardingStepResources[position].titleId);
            TextView caption = (TextView) view.findViewById(R.id.caption);
            caption.setText(onBoardingStepResources[position].captionResId);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }
    };
}
