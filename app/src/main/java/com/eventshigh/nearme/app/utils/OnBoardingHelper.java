package com.eventshigh.nearme.app.utils;

import android.app.ActionBar;
import android.app.Activity;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import com.eventshigh.nearme.app.R;
import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ActionItemTarget;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * This class helps showing on boarding messages to users.
 */
public class OnBoardingHelper {
    // ShowcaseView used for on boarding
    private ShowcaseView showcaseView;

    // Activity in which this OnBoarding messages are shown.
    private final Activity activity;

    // Is the OnBoarding is shown ?
    private boolean isShowing = false;

    // Targets
    private int targetsIndex = 0;
    private final List<Pair<Target, Integer>> targets;

    public OnBoardingHelper(Activity activity) {
        this.activity = activity;

        targets = new ArrayList<>();
        LinearLayout daySelector = (LinearLayout) activity.findViewById(R.id.daySelector);
        if (daySelector != null && daySelector.getChildCount() > 1) {
            targets.add(Pair.create(
                    (Target) new ViewTarget(daySelector.getChildAt(1)),
                    R.string.onboarding_change_date));
        }

        targets.add(Pair.create(
                (Target) new ActionItemTarget(activity, R.id.action_change_location),
                R.string.onboarding_action));

        try {
            ActionBar actionBar = activity.getActionBar();
            if (actionBar != null) {
                Field mTabScrollViewField = actionBar.getClass().getDeclaredField("mTabScrollView");
                mTabScrollViewField.setAccessible(true);
                View view = (View) mTabScrollViewField.get(actionBar);
                if (view != null) {
                    targets.add(Pair.create(
                            (Target) new ViewTarget(view),
                            R.string.onboarding_filter));
                }
            }
        } catch (Exception e) {
            // Ignore.
        }
    }

    public void next() {
        if (isShowing) {
            mShowcaseViewClickListener.onClick(null);
        } else {
            if (targetsIndex < targets.size() && showcaseView == null) {
                showcaseView = new ShowcaseView.Builder(activity, true)
                        .setTarget(targets.get(targetsIndex).first)
                        .setContentText(targets.get(targetsIndex).second)
                        .setStyle(R.style.ShowcaseTheme)
                        .setOnClickListener(mShowcaseViewClickListener)
                        .setShowcaseEventListener(mShowcaseEventListener)
                        .singleShot(1)
                        .build();
            }
        }
    }

    private OnShowcaseEventListener mShowcaseEventListener = new OnShowcaseEventListener() {
        @Override
        public void onShowcaseViewHide(ShowcaseView showcaseView) {
            isShowing = false;
        }

        @Override
        public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
        }

        @Override
        public void onShowcaseViewShow(ShowcaseView showcaseView) {
            isShowing = true;
        }
    };

    private OnClickListener mShowcaseViewClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            targetsIndex ++;
            if (targetsIndex >= targets.size()) {
                showcaseView.hide();
                return;
            }

            showcaseView.setShowcase(targets.get(targetsIndex).first, true);
            showcaseView.setContentText(activity.getText(targets.get(targetsIndex).second));
            if (targetsIndex == targets.size() - 1) {
                showcaseView.setButtonText("Close");
            }
        }
    };
}
