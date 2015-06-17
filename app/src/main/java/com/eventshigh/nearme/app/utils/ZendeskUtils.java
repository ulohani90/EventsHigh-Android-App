package com.eventshigh.nearme.app.utils;

import android.content.Context;

import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.user.Account;
import com.zendesk.logger.Logger;
import com.zendesk.sdk.feedback.impl.BaseZendeskFeedbackConfiguration;
import com.zendesk.sdk.model.network.AnonymousIdentity;
import com.zendesk.sdk.model.network.Identity;
import com.zendesk.sdk.network.impl.ZendeskConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to manage Zendesk.
 */
public class ZendeskUtils {
    public static void initZendesk(Context context) {
        Logger.setLoggable(BuildConfig.DEBUG);

        // Initialize ZenDesk.
        ZendeskConfig.INSTANCE.init(context,
                "https://eventshigh.zendesk.com",
                "e768bcce15686cef22667ce751438e5637c2f22957521a51",
                "mobile_sdk_client_c134c9f3e705c0b37a78");

        // Anonymous reporting.
        Identity anonymousIdentity = new AnonymousIdentity.Builder()
                .withExternalIdentifier("user:" + Utils.md5(Utils.getAndroidId(context)))
                .withNameIdentifier("Me")
                .build();
        ZendeskConfig.INSTANCE.setIdentity(anonymousIdentity);

        // Set the configuration used by the Contact ZenDesk component.
        ZendeskConfig.INSTANCE.setContactConfiguration(new FeedbackConfiguration(context));
    }

    public static void setEventFeedbackConfiguration(Context context, Event event) {
        ZendeskConfig.INSTANCE.setContactConfiguration(new EventFeedbackConfiguration(context, event));
    }

    public static class FeedbackConfiguration extends BaseZendeskFeedbackConfiguration {
        private final Context context;

        public FeedbackConfiguration(Context context) {
            this.context = context;
        }

        @Override
        public String getRequestSubject() {
            return "Feedback";
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

    public static class EventFeedbackConfiguration  extends FeedbackConfiguration {
        private final Event event;

        public EventFeedbackConfiguration(Context context, Event event) {
            super(context);

            this.event = event;
        }

        @Override
        public String getRequestSubject() {
            return "Event Query: " + event.title;
        }

        @Override
        public String getAdditionalInfo() {
            return event.getEventDetailsURI().toString();
        }
    }
}
