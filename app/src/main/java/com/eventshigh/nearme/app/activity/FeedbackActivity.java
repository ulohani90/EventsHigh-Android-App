package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.utils.FeedbackConfiguration;
import com.eventshigh.nearme.app.utils.Utils;
import com.zendesk.sdk.feedback.ui.ContactZendeskActivity;
import com.zendesk.sdk.logger.Logger;
import com.zendesk.sdk.model.network.AnonymousIdentity;
import com.zendesk.sdk.model.network.ErrorResponse;
import com.zendesk.sdk.model.network.Identity;
import com.zendesk.sdk.network.impl.ZendeskConfig;
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

        initZenDesk();
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

    private void initZenDesk() {
        Logger.setLoggable(BuildConfig.DEBUG);

        // Initialize ZenDesk.
        ZendeskConfig.INSTANCE.init(this,
                "https://eventshigh.zendesk.com",
                "e768bcce15686cef22667ce751438e5637c2f22957521a51",
                "mobile_sdk_client_c134c9f3e705c0b37a78");

        // Anonymous reporting.
        Identity anonymousIdentity = new AnonymousIdentity.Builder()
                .withExternalIdentifier("user:" + Utils.md5(Utils.getAndroidId(this)))
                .withNameIdentifier("Me")
                .build();
        ZendeskConfig.INSTANCE.setIdentity(anonymousIdentity);

        // Set the configuration used by the Contact ZenDesk component.
        ZendeskConfig.INSTANCE.setContactConfiguration(new FeedbackConfiguration(this));
    }
}
