package com.eventshigh.nearme.app.activity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.user.Preferences;

/**
 * Created by umesh on 14/07/16.
 */
public class NewOnboardingActivity extends BaseActivity {

    LinearLayout dotsView;
    ViewPager pager;

    View arrowRight;

    boolean isArrowShown;

    boolean isLogout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_onboarding_layout);
        isLogout = getIntent().getBooleanExtra("is_logout", false);
        pager = (ViewPager) findViewById(R.id.pager);
        dotsView = (LinearLayout) findViewById(R.id.dots_parent);
        ImagePagerAdapter imagePagerAdapter = new ImagePagerAdapter();
        pager.setAdapter(imagePagerAdapter);
        dotsView.removeAllViews();
        for (int i = 0; i < imagePagerAdapter.getCount(); i++) {
            View view = getLayoutInflater().inflate(
                    R.layout.view_dot_featured, dotsView, false);
            view.setSelected(i == 0);
            dotsView.addView(view);
        }

        pager.clearOnPageChangeListeners();
        pager.addOnPageChangeListener(new DotsSelector());

        (findViewById(R.id.tell_us_interest)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportActionToAnalytics("tell_us_interest_click");
                launchNextActivity();
            }
        });

        arrowRight = findViewById(R.id.arrow_right);
        isArrowShown = true;

    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning())
                animateArrowForward();
        }
    };

    int images[] = {R.drawable.ic_onboarding_1, R.drawable.ic_onboarding_2, R.drawable.ic_onboarding_3, R.drawable.ic_onboarding_4};

    public class ImagePagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return images.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = getLayoutInflater().inflate(R.layout.card_onboarding_new, container, false);
            ImageView banner = (ImageView) view.findViewById(R.id.banner_img);
            Glide.with(NewOnboardingActivity.this).load(images[position]).placeholder(R.drawable.eh_default_event)
                    .crossFade().centerCrop()
                    .into(banner);

            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }

    public void hideArrow() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(arrowRight, View.ALPHA, 1, 0);
        anim.setDuration(200);
        anim.start();
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                arrowRight.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        isArrowShown = false;
    }

    private class DotsSelector implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (positionOffset > 0.2 && isArrowShown) {
                hideArrow();
            }
        }

        @Override
        public void onPageSelected(int position) {

            for (int i = 0; i < dotsView.getChildCount(); i++) {
                dotsView.getChildAt(i).setSelected(i == position);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // do nothing.
        }
    }


    private void launchNextActivity() {
        Intent phoneLoginIntent = new Intent(this, SelectInterestsActivity.class);
        phoneLoginIntent.putExtra("is_onboarding", true);
        phoneLoginIntent.putExtra("is_logout", isLogout);
        startActivity(phoneLoginIntent);
        // finish();
    }


    @Override
    protected void onStart() {
        super.onStart();

        // We purposefully set the preference flag to not show on boarding screens
        // in onStart. If users kills the app or if app crashes for any reasons, we
        // do not show onboarding screen again.

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FbLoginFragment.LOGOUT_BROADCAST_ACTION);
        registerReceiver(receiver, intentFilter);
        Preferences.getInstance(this).setShowOnboarding(false);


    }


    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void animateArrowForward() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(arrowRight, View.TRANSLATION_X, 0, 30f);
        anim.setDuration(500);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animateArrowBackward();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }
        });
        anim.start();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (isArrowShown) {
            new Handler().postDelayed(runnable, 1000);
        }
    }

    private void animateArrowBackward() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(arrowRight, View.TRANSLATION_X, 30, 0);
        anim.setDuration(500);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animateArrowForward();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }
        });
        anim.start();
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            finish();
        }
    };
}
