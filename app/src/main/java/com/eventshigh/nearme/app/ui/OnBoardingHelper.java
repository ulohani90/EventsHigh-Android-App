package com.eventshigh.nearme.app.ui;

import android.app.ActionBar;
import android.app.backup.BackupManager;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
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
    private final BaseActivity activity;

    // Is the OnBoarding is shown ?
    private boolean isShowing = false;

    // Targets
    private int targetsIndex = 0;
    private final List<Pair<Target, Integer>> targets;

    public OnBoardingHelper(BaseActivity activity) {
        this.activity = activity;

        targets = new ArrayList<>();
        addTarget(new ActionItemTarget(activity, R.id.action_filter),
                R.string.onboarding_action);

        try {
            ActionBar actionBar = activity.getActionBar();
            if (actionBar != null) {
                Field mTabScrollViewField = actionBar.getClass().getDeclaredField("mTabScrollView");
                mTabScrollViewField.setAccessible(true);
                View view = (View) mTabScrollViewField.get(actionBar);
                if (view != null) {
                    addTarget(new ViewTarget(view), R.string.onboarding_filter);
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
            activity.reportActionToAnalytics("endOnboarding");
            isShowing = false;
        }

        @Override
        public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
        }

        @Override
        public void onShowcaseViewShow(ShowcaseView showcaseView) {
            activity.reportActionToAnalytics("startOnboarding");
            isShowing = true;
        }
    };

    private OnClickListener mShowcaseViewClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            targetsIndex ++;
            if (targetsIndex >= targets.size()) {
                showcaseView.hide();
                new BackupManager(activity).dataChanged();
                return;
            }

            showcaseView.setShowcase(targets.get(targetsIndex).first, true);
            showcaseView.setContentText(activity.getText(targets.get(targetsIndex).second));
            if (targetsIndex == targets.size() - 1) {
                showcaseView.setButtonText("Close");
            }
        }
    };

    private void addTarget(Target target, int messageResId) {
        try {
            target.getPoint();
            targets.add(Pair.create(target, messageResId));
        } catch (Exception e) {
            // Workaround for NPE for hidden items
            // See https://github.com/amlcurran/ShowcaseView/issues/195
            // do nothing.
        }
    }
}
