package com.eventshigh.nearme.app.ui.adapter;

import android.app.ProgressDialog;
import android.net.Uri;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.android.volley.Request.Method;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.AskForContactsDialog;
import com.eventshigh.nearme.app.ui.AskForContactsDialog.ContactsRequestCallback;
import com.eventshigh.nearme.app.ui.OneSecDialog;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.Signer;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class EhInviteNotificationCard extends ViewHolder {
    private final View shareAppButton;

    public static EhInviteNotificationCard newInstance(BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_eh_invite_notification, parent, false);
        return new EhInviteNotificationCard(view);
    }

    public EhInviteNotificationCard(View itemView) {
        super(itemView);

        shareAppButton = itemView.findViewById(R.id.share_app);
    }

    public void populate(final BaseActivity activity) {
        shareAppButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Preferences prefs = Preferences.getInstance(activity);
                if (! prefs.canUploadContacts()) {
                    AskForContactsDialog.show(activity, prefs, new ContactsRequestCallback() {
                        @Override
                        public void onContactsUploadAccepted() {
                            activity.reportActionToAnalytics("shareAppAcceptOnNotification");
                            recordUserConsent(activity);
                        }

                        @Override
                        public void onContactsUploadRejected() {
                            // do nothing.
                            activity.reportActionToAnalytics("shareAppRejectOnNotification");
                        }
                    });
                } else {
                    activity.reportActionToAnalytics("shareAppOnNotification");
                    recordUserConsent(activity);
                }
            }
        });
    }

    private static void recordUserConsent(final BaseActivity activity) {
        final ProgressDialog dialog = OneSecDialog.show(activity);
        Uri uri = UpdateAccountInfoService.getBaseUri(activity, "register_ok_to_invite_friends").build();
        try {
            VolleyHelper.addToRequestQueue(activity,
                    new StringRequest(Method.GET, Signer.sign(uri).toString(),
                            new Listener<String>() {
                                @Override
                                public void onResponse(String rep, boolean isIntermediate) {
                                    dialog.dismiss();
                                    activity.showMessage("Great! We will now invite your friends to Join EventsHigh!");
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                    // do nothing.
                                    activity.showMessage(R.string.failed_load);
                                    Crashlytics.getInstance().core.logException(volleyError.getCause());
                                }
                            }
                    )
            );
        } catch (IOException | GeneralSecurityException e) {
            Crashlytics.getInstance().core.logException(e);
            Log.w(UpdateAccountInfoService.class.getSimpleName(), "Failed to sendSignedRequest: " + uri, e);
        }
    }
}
