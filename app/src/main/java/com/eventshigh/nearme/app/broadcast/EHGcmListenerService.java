package com.eventshigh.nearme.app.broadcast;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.CustomUrlActivity;
import com.eventshigh.nearme.app.activity.EventInfoFragment;
import com.eventshigh.nearme.app.activity.FeedbackActivity;
import com.eventshigh.nearme.app.activity.LaunchActivity;
import com.eventshigh.nearme.app.activity.NewEventDetailActivity;
import com.eventshigh.nearme.app.activity.PointsBreakdownActivity;
import com.eventshigh.nearme.app.activity.SelectInterestsActivity;
import com.eventshigh.nearme.app.activity.UserProfileActivity;
import com.eventshigh.nearme.app.activity.WriteReviewActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.FriendsStore;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.data.stream.EventNotificationStreamItem;
import com.eventshigh.nearme.app.data.stream.QueryNotificationStreamItem;
import com.eventshigh.nearme.app.data.stream.TicketNotificationStreamItem;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyInterestEventsRequest;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest;
import com.eventshigh.nearme.app.notification.EHNotification;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Account.UserInfo;
import com.eventshigh.nearme.app.utils.ContactUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.GAHelper;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.utils.ZendeskUtils;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * See https://developers.google.com/cloud-messaging/android/client.
 */
public class EHGcmListenerService extends FirebaseMessagingService {
    private static final String LOG_TAG = EHGcmListenerService.class.getSimpleName();

    private GAHelper gaHelper;

    @Override
    public void onMessageReceived(RemoteMessage message) {
        gaHelper = GAHelper.getInstance(EHGcmListenerService.this);
        System.out.println("Notification Received");
        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : message.getData().entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
        EHNotification notification = parseBundle(bundle);
        if (notification != null) {
            notification.showNotification();
        }
    }

    private
    @Nullable
    EHNotification parseBundle(Bundle msg) {

        String title = Utils.checkIfUnknown(msg.getString("t"));
        String message = Utils.checkIfUnknown(msg.getString("m"));
        if (message == null || title == null) {
            Log.w(LOG_TAG, "Invalid notification: message: " + message + ", title: " + title);
            return null;
        }

        String eventId = Utils.checkIfUnknown(msg.getString("id"));
        String query = Utils.checkIfUnknown(msg.getString("q"));
        String contestUrl = Utils.checkIfUnknown(msg.getString("contest"));
        String imageUrl = Utils.checkIfUnknown(msg.getString("img"));
        String ticket = Utils.checkIfUnknown(msg.getString("ticket"));
        String target = Utils.checkIfUnknown(msg.getString("target"));
        String priority = Utils.checkIfUnknown(msg.getString("priority"));
        String mobileNo = Utils.checkIfUnknown(msg.getString("mobile"));
        String personalisedNotif = Utils.checkIfUnknown(msg.getString("personalised_notif"));
        String personalizeInterest = Utils.checkIfUnknown(msg.getString("perosnalize_interest"));

        String special = Utils.checkIfUnknown(msg.getString("special"));
        String pointsBreakdown = Utils.checkIfUnknown(msg.getString("points_breakdown"));

        String reviewFor = Utils.checkIfUnknown(msg.getString("review_for"));
        String reviewEntityId = Utils.checkIfUnknown(msg.getString("review_entity_id"));
        String reviewEntityImage = Utils.checkIfUnknown(msg.getString("review_entity_image"));
        String reviewedEntity = Utils.checkIfUnknown(msg.getString("reviewed_entity"));
        int notificationId = -1;

        UserContact contact = null;
        if (mobileNo != null) {
            UserInfo userInfo = new Account(this).getUserInfo();
            if (mobileNo.equals(userInfo.phoneNo)) {
                reportAction("notificationSkipped");
                return null;
            }
            try {
                contact = ContactUtils.getContactForServerPhone(this, mobileNo);
            } catch (Exception e) {
                Crashlytics.getInstance().core.logException(e);
            }
        }

        if (contact != null) {
            if (!(new FriendsStore(this)).isFollowing(contact.contactId)) {
                // Don't show notification if the user is not following this friend
                return null;
            }

            title = title.replace("Your friend", contact.name);
            message = message.replace("Your friend", contact.name);
        }

        if (eventId == null && query == null && contestUrl == null && ticket == null && target == null && personalisedNotif == null && personalizeInterest == null && special == null && pointsBreakdown == null && reviewFor == null && reviewEntityId == null && reviewEntityImage == null && reviewedEntity == null) {
            Log.w(LOG_TAG, "Invalid notification, nether eventId, query, ticket or contest param passed");
            return null;
        }


        PendingIntent contentIntent;
        if (eventId != null) {
            City city = new Account(this).getLastCity();
            if (city == null) {
                city = City.BANGALORE;
            }
            contentIntent = createPendingIntent(this, eventId, city);
            EventNotificationStreamItem.record(this, title, message, imageUrl, mobileNo, eventId, city.name());
        } else if (query != null) {
            Intent intent = new Intent(this, LaunchActivity.class);
            intent.setAction(BaseActivity.NOTIFICATION_ACTION + query);
            intent.putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, new EventsContext(null, query));
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
            QueryNotificationStreamItem.record(this, title, message, imageUrl, mobileNo, query);
        } else if (ticket != null) {
            ZendeskUtils.initZendesk(this);
            Intent intent = new Intent(this, FeedbackActivity.class);
            intent.setAction(BaseActivity.NOTIFICATION_ACTION);
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
            TicketNotificationStreamItem.record(this, title, message, imageUrl, ticket);
        } else if (target != null) {
            Intent intent = null;
            if (target.startsWith("tab:")) {
                intent = new Intent(this, LaunchActivity.class);
                intent.setAction(BaseActivity.NOTIFICATION_ACTION);
                intent.putExtra(LaunchActivity.DEFAULT_TAB_PARAM, target.split(":", 2)[1]);
            } else {
                try {
                    intent = new Intent(this, getClassLoader().loadClass(target));
                } catch (ClassNotFoundException e) {
                    Crashlytics.getInstance().core.logException(e);
                }
            }

            if (intent == null) {
                return null;
            }
            intent.setAction(BaseActivity.NOTIFICATION_ACTION + target);
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        } else if (personalisedNotif != null) {
            City city = new Account(this).getLastCity();
            if (city == null) {
                return null;
            }

            MyEventsRequest.TopicEvents interestName = new MyInterestEventsRequest(
                    getApplicationContext(),
                    new EventsContext(city.cityBounds.getCenter(), ""),
                    Request.Priority.HIGH,
                    null, false, true, null, null).getNonEmptyInterest();
            if (interestName == null) {
                return null;
            }
            // show notification
            //--
            title = "We know you like " + interestName.topicName;

            String interestCount = getInterestCount(interestName.events.size());
            message = "So we thought of you. Explore " + interestCount + " experiences happening this week.";
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.putExtra(UserProfileActivity.FROM_NOTIFICATION_PARAM, true);
            intent.setAction(BaseActivity.NOTIFICATION_ACTION);
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        } else if (personalizeInterest != null) {
            Intent intent = new Intent(this, SelectInterestsActivity.class);
            intent.setAction(BaseActivity.NOTIFICATION_ACTION);
            intent.putExtra(SelectInterestsActivity.FROM_NOTIFICATION_PARAM, true);
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        } else if (special != null) {
            Intent intent = new Intent(this, LaunchActivity.class);
            intent.setAction(BaseActivity.NOTIFICATION_ACTION + "specials");
            intent.putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, new EventsContext(null, "eventshigh specials"));
            intent.putExtra("special_obj", SocialInvitationsRequest.SpecialCoupons.parseJson(special));
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
            QueryNotificationStreamItem.record(this, title, message, imageUrl, mobileNo, "eventshigh specials");
        } else if (pointsBreakdown != null) {
            Intent intent = new Intent(this, PointsBreakdownActivity.class);
            intent.setAction(BaseActivity.NOTIFICATION_ACTION);
            intent.putExtra(PointsBreakdownActivity.FROM_NOTIFICATION_PARAM, true);
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        } else if (reviewFor != null) {
            Intent intent = new Intent(this, WriteReviewActivity.class);
            intent.setAction(BaseActivity.NOTIFICATION_ACTION + title);
            intent.putExtra(WriteReviewActivity.FROM_NOTIFICATION_PARAM, true);
            intent.putExtra(EventInfoFragment.OBJECT_TYPE, reviewFor);
            intent.putExtra(WriteReviewActivity.REVIEW_ENTITY_ID, reviewEntityId);
            intent.putExtra(WriteReviewActivity.REVIEW_ENTITY_IMAGE, reviewEntityImage);
            intent.putExtra(WriteReviewActivity.REVIEW_ENTITY_NAME, reviewedEntity);
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
            //imageUrl = reviewEntityImage;
            notificationId = 2;
        } else {
            Intent intent = new Intent(this, CustomUrlActivity.class);
            intent.setAction(BaseActivity.NOTIFICATION_ACTION + title);
            intent.setData(Uri.parse(contestUrl));
            intent.putExtra(CustomUrlActivity.EXTRA_TITLE_KEY, title);
            contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        return new EHNotification(this, title, message,
                imageUrl, contentIntent,
                priority == null ? Notification.PRIORITY_LOW : Notification.PRIORITY_HIGH, notificationId,
                contact
        );
    }


    public String getInterestCount(int count) {
        if (count < 5 || count % 5 == 0) {
            return count + "";
        } else {
            return count - (count % 5) + "+";
        }
    }

    private void reportAction(String actionName) {
        gaHelper.reportActionToAnalytics(LOG_TAG, actionName);
    }

    private static PendingIntent createPendingIntent(Context context, String eventId, @Nullable City city) {
        if (city == null) {
            // placeholder for city.
            city = City.BANGALORE;
        }

        Intent intent = new Intent(context, NewEventDetailActivity.class);
        intent.setAction(BaseActivity.NOTIFICATION_ACTION);
        intent.setData(EventsHighEndpoints.getEventDetailsURI(city.name(), eventId));
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    @Override
    public void onMessageSent(String msgId) {
        super.onMessageSent(msgId);
        Log.i("Message sent", msgId);
    }

    @Override
    public void onSendError(String s, Exception e) {
        super.onSendError(s, e);
        Log.i("Message Error", s);
    }


}
