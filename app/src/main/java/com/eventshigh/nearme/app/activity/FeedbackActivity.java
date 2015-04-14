package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.utils.ZendeskUtils;
import com.zendesk.sdk.feedback.ui.ContactZendeskActivity;
import com.zendesk.sdk.model.network.ErrorResponse;
import com.zendesk.sdk.network.impl.ZendeskRequestService.RequestLoadingListener;
import com.zendesk.sdk.requests.RequestListFragment;

/**
 * An activity which wraps around the ZenDesk SDK to manage user interactions.
 */
public class FeedbackActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        ZendeskUtils.initZendesk(this);
        onRetry(null);
    }

    public void newFeedback(View view) {
        if (view != null) {
            reportActionToAnalytics("newFeedback");
        }

        Intent feedbackIntent = new Intent(this, ContactZendeskActivity.class);
        startActivity(feedbackIntent);
    }

    public void onRetry(View view) {
        if (view != null) {
            reportActionToAnalytics("retry");
        }

        RequestListFragment requestListFragment = new RequestListFragment();
        requestListFragment.setRequestLoadingListener(mRequestLoadingListener);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.conversations, requestListFragment);
        fragmentTransaction.commit();
    }

    private RequestLoadingListener mRequestLoadingListener = new RequestLoadingListener() {
        @Override
        public void onLoadStarted() {
        }

        @Override
        public void onLoadFinished(int num) {
            if (num == 0) {
                newFeedback(null);
                finish();
            }
        }

        @Override
        public void onLoadError(ErrorResponse errorResponse) {
        }
    };
}
