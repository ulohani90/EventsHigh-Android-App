package com.eventshigh.nearme.app.utils;

import android.content.Context;

import com.eventshigh.nearme.app.user.Account;
import com.zendesk.sdk.feedback.impl.BaseZendeskFeedbackConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * A configuration to be used in Zendesk configuration.
 */
public class FeedbackConfiguration extends BaseZendeskFeedbackConfiguration {
    private final Context context;

    public FeedbackConfiguration(Context context) {
        this.context = context;
    }

    @Override
    public String getRequestSubject() {
        return "App Feedback";
    }

    public List<String> getTags() {
        List<String> tags = new ArrayList<>();
        tags.add(Utils.getAndroidId(context));
        String phoneNo = new Account(context).getPhoneNumber().first;
        if (phoneNo != null) {
            tags.add(phoneNo);
        }
        return tags;
    }
}
