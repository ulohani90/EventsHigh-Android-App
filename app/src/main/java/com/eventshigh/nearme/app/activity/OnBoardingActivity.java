package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.settings.Preferences;
import com.eventshigh.nearme.app.utils.Utils;

/**
 * Onboarding activity for first time users. This activity is shown to a user when the app is
 * launched and keeps showing until the user skips the onboarding steps.
 */
public class OnBoardingActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {
    /**
     * The view pager navigation dots shown below the view pager. A big dot indicates the step
     * that the user is currently on when swiping through the view pager content.
     */
    private LinearLayout dotsView;

    /** The view pager which shows the onboarding steps */
    private ViewPager viewPager;

    /** The last view pager position from which the user navigated. */
    private int lastPosition = 0;

    /**
     * The layout param that makes the view pager dot look big indicating the current page the user
     * is currently on.
     */
    private LinearLayout.LayoutParams bigDotLayoutParams;

    /**
     * The layout param the makes the view pager dots look small indicating the number of pages in
     * the view pager.
     */
    private LinearLayout.LayoutParams smallDotLayoutParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // Convert dp units to pixels, because layout params uses px units
        int dp4 = Utils.dpToPx(this, 4);
        int dp8 = Utils.dpToPx(this, 8);
        int dp12 = Utils.dpToPx(this, 12);
        smallDotLayoutParams = new LinearLayout.LayoutParams(dp8, dp8);
        smallDotLayoutParams.setMargins(dp4, dp4, dp4, dp4);
        bigDotLayoutParams = new LinearLayout.LayoutParams(dp12, dp12);
        bigDotLayoutParams.setMargins(dp4, dp4, dp4, dp4);

        dotsView = (LinearLayout) findViewById(R.id.dots_parent);

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(new Adapter());
        viewPager.setOnPageChangeListener(this);

        // If the SHOW_ONBOARDING flag is disabled or if the user has already seen the onboarding
        // steps, then finish this activity and start the {@link LaunchActivity}.
        if (!BuildConfig.SHOW_ONBOARDING || !Preferences.getInstance(this).isFirstLaunch()) {
            startLaunchActivity();
        }
    }

    /**
     * Starts the {@link LaunchActivity} and finishes this activity.
     */
    private void startLaunchActivity() {
        startActivity(new Intent(this, LaunchActivity.class));
        finish();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        // Change the last active dot to small dot
        dotsView.getChildAt(lastPosition).setLayoutParams(smallDotLayoutParams);
        // Change the currently active dot to big dot
        dotsView.getChildAt(position).setLayoutParams(bigDotLayoutParams);
        // Keep track of the currently active page
        lastPosition = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    public void onSkipClicked(View view) {
        // If user skipped this activity then setup a flag that {@link OnBoardingActivity} need not
        // be shown again on the next launch.
        Preferences.getInstance(this).setPrefFirstLaunch(false);
        startLaunchActivity();
    }

    /** The view pager adapter that creates the views for each onboarding step. */
    private class Adapter extends PagerAdapter {
        /** The model that stores the resources for each onboarding step. */
        private class ViewResources {
            // The text to be shown below the image in onboarding step
            private final int captionResId;

            // The image to be shown in onboarding step
            private final int imageResId;

            private ViewResources(int captionResId, int imageResId) {
                this.captionResId = captionResId;
                this.imageResId = imageResId;
            }
        }

        private final ViewResources[] viewResourceses = new ViewResources[]{
                new ViewResources(R.string.onboarding_map, R.drawable.ic_onboarding_map),
                new ViewResources(R.string.onboarding_city, R.drawable.ic_onboarding_city),
                new ViewResources(R.string.onboarding_follow, R.drawable.ic_onboarding_follow),
                new ViewResources(R.string.onboarding_share, R.drawable.ic_onboarding_share),
        };

        private final LayoutInflater layoutInflater;

        public Adapter() {
            layoutInflater = getLayoutInflater();

            // Add dots to indicate the number of pages in view pager
            for (int i = 0; i < viewResourceses.length; i++) {
                View view = layoutInflater.inflate(R.layout.viewpager_dot, dotsView, false);
                dotsView.addView(view);
                view.setLayoutParams(smallDotLayoutParams);
            }

            // Make the dot corresponding to currently active page big.
            dotsView.getChildAt(0).setLayoutParams(bigDotLayoutParams);
        }

        @Override
        public int getCount() {
            return viewResourceses.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = layoutInflater.inflate(R.layout.viewpager_onboarding, container, false);
            container.addView(view);
            ImageView imageView = (ImageView) view.findViewById(R.id.image);
            imageView.setImageResource(viewResourceses[position].imageResId);
            TextView caption = (TextView) view.findViewById(R.id.caption);
            caption.setText(viewResourceses[position].captionResId);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }
    }
}
