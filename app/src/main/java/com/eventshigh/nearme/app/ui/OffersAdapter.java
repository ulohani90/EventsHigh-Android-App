package com.eventshigh.nearme.app.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.EventDetailActivity;
import com.eventshigh.nearme.app.activity.PhoneLoginActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Offer;
import com.eventshigh.nearme.app.network.OffersRequest.OffersResponse;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;

/**
 * Adapter used to show offers information in RecyclerView.
 */
public class OffersAdapter extends RecyclerView.Adapter<ViewHolder> {
    private final BaseContextActivity activity;
    private OffersResponse offersResponse;

    public OffersAdapter(BaseContextActivity activity) {
        this.activity = activity;
    }

    public void setOffersResponse(OffersResponse offersResponse) {
        this.offersResponse = offersResponse;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 0 : 1;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        if (type == 0) {
            View view = activity.getLayoutInflater().inflate(R.layout.card_offer_header, parent, false);
            return new OffersHeaderCard(view);
        }

        View view = activity.getLayoutInflater().inflate(R.layout.card_offer, parent, false);
        return new OfferCard(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        if (position == 0) {
            ((OffersHeaderCard) card).populate();
        }
        if (position > 0) {
            ((OfferCard) card).populate(offersResponse.offers.get(position - 1));
        }
    }

    @Override
    public int getItemCount() {
        return offersResponse == null ? 0 : offersResponse.offers.size() + 1;
    }

    private class OffersHeaderCard extends ViewHolder {
        private final TextView forClaimView;
        private final TextView claimedView;
        private final TextView totalInstallsView;

        private final TextView inviteLinkView;

        private final View inviteViaFB;
        private final View inviteViaTwitter;
        private final View inviteViaEmail;
        private final View inviteViaWhatsapp;

        public OffersHeaderCard(View itemView) {
            super(itemView);

            forClaimView = (TextView) itemView.findViewById(R.id.for_claim);
            claimedView = (TextView) itemView.findViewById(R.id.claimed);
            totalInstallsView = (TextView) itemView.findViewById(R.id.total_installs);

            inviteLinkView = (TextView) itemView.findViewById(R.id.invite_link);

            inviteViaFB = itemView.findViewById(R.id.share_fb);
            inviteViaTwitter = itemView.findViewById(R.id.share_twitter);
            inviteViaEmail = itemView.findViewById(R.id.share_email);
            inviteViaWhatsapp = itemView.findViewById(R.id.share_whatsapp);
        }

        public void populate() {

            forClaimView.setText(Integer.toString(offersResponse.forClaim));
            claimedView.setText(Integer.toString(offersResponse.claimed));
            totalInstallsView.setText(Integer.toString(offersResponse.totalInstalls));

            final String inviteLink = new Account(activity).getAppDownloadLink();
            inviteLinkView.setText(inviteLink);
            inviteLinkView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.reportActionToAnalytics("copyInviteLink");
                    ClipboardManager clipboard =
                            (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("invite link", inviteLink);
                    clipboard.setPrimaryClip(clip);
                    Snackbar.make(activity.getViewForSnackbar(), R.string.ui_invite_link_copy,
                            Snackbar.LENGTH_SHORT).show();
                }
            });

            inviteViaFB.setVisibility(
                    EventDetailActivity.isInstalled(activity, EventDetailActivity.PACKAGE_NAME_FACEBOOK) ?
                            View.VISIBLE : View.GONE);
            inviteViaTwitter.setVisibility(
                EventDetailActivity.isInstalled(activity, EventDetailActivity.PACKAGE_NAME_TWITTER) ?
                    View.VISIBLE : View.GONE);
            inviteViaEmail.setVisibility(
                EventDetailActivity.isInstalled(activity, EventDetailActivity.PACKAGE_NAME_EMAIL) ?
                    View.VISIBLE : View.GONE);
            inviteViaWhatsapp.setVisibility(
                EventDetailActivity.isInstalled(activity, EventDetailActivity.PACKAGE_NAME_WHATSAPP) ?
                    View.VISIBLE : View.GONE);

            inviteViaFB.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareAppFB(activity, inviteLink);
                }
            });
            inviteViaTwitter.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareAppTwitter(activity, inviteLink);
                }
            });
            inviteViaEmail.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareAppEmail(activity, inviteLink);
                }
            });
            inviteViaWhatsapp.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareAppWhatsApp(activity, inviteLink);
                }
            });
        }

        private void shareAppFB(BaseActivity activity, String inviteLink) {
            shareApp(activity, inviteLink, EventDetailActivity.PACKAGE_NAME_FACEBOOK);
        }

        private void shareAppTwitter(BaseActivity activity, String inviteLink) {
            shareApp(activity,inviteLink, EventDetailActivity.PACKAGE_NAME_TWITTER);
        }

        private void shareAppEmail(BaseActivity activity, String inviteLink) {
            shareApp(activity,inviteLink, EventDetailActivity.PACKAGE_NAME_EMAIL);
        }

        private void shareAppWhatsApp(BaseActivity activity, String inviteLink) {
            shareApp(activity,inviteLink, EventDetailActivity.PACKAGE_NAME_WHATSAPP);
        }

        private void shareApp(BaseActivity activity, String inviteLink, String packageName) {
            activity.reportActionToAnalytics("shareApp", packageName);

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT,
                    String.format(activity.getString(R.string.share_app_text), inviteLink));
            intent.setType("text/plain");
            intent.setPackage(packageName);
            activity.startActivity(intent);
        }
    }

    private class OfferCard extends ViewHolder {
        private final NetworkImageView imageView;
        private final TextView messageView;
        private final TextView actionView;
        private final View expiredView;

        public OfferCard(View itemView) {
            super(itemView);

            imageView = (NetworkImageView) itemView.findViewById(R.id.offer_image);
            messageView = (TextView) itemView.findViewById(R.id.offer_message);
            actionView = (TextView) itemView.findViewById(R.id.offer_action);
            expiredView = itemView.findViewById(R.id.expired);
        }

        public void populate(final Offer offer) {
            imageView.setDefaultImageResId(R.drawable.eh_default_event);
            imageView.setDefaultImageResId(R.drawable.eh_default_event);
            imageView.setImageUrl(offer.imgUrl.toString(), VolleyHelper.getImageLoader(activity));

            messageView.setText(offer.message);

            if (offer.actionName.isEmpty()) {
                actionView.setVisibility(View.GONE);
            } else {
                actionView.setText(offer.actionName);
                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        claimOffer(offer);
                    }
                });
            }

            expiredView.setVisibility(offer.isExpired() ? View.VISIBLE : View.GONE);
        }

    }

    private void claimOffer(final Offer offer) {
        if (offer.threshold > offersResponse.forClaim) {
            String message = String.format(
                    activity.getString(R.string.offer_threshold_message), offer.threshold);
            Snackbar.make(activity.getViewForSnackbar(), message, Snackbar.LENGTH_LONG).show();
            return;
        }

        if (offer.actionType.equals("event")) {
            activity.showEventDetails(
                EventsHighEndpoints.getEventDetailsURI(City.BANGALORE, offer.actionLink), "offer");
            return;
        }

        if (offer.actionType.equals("uri")) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(offer.actionLink)));
            return;
        }

        if (offer.actionType.equals("refer")) {
            // Verify user has registered phone no.
            Account account = new Account(activity);
            Pair<String, Boolean> phoneNumberStatus = account.getPhoneNumber();
            if (!phoneNumberStatus.second) {
                activity.reportActionToAnalytics("claimOfferNoPhone", offer.id);
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.pref_title_phone_no)
                        .setMessage(R.string.ui_offer_redeem_phone)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                activity.startActivity(new Intent(activity, PhoneLoginActivity.class));
                            }
                        })
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .show();
                return;
            }

            // Send email.
            try {
                String emailMessage = String.format(activity.getString(R.string.offer_email),
                        offer.id, phoneNumberStatus.first, Utils.md5(Utils.getAndroidId(activity)));
                activity.startActivity(new Intent(Intent.ACTION_SENDTO,
                                Uri.parse("mailto:support@eventshigh.com?subject=Redeem%20offer"))
                                .putExtra(Intent.EXTRA_TEXT, emailMessage)
                );
                activity.reportActionToAnalytics("claimOffer", offer.id);
            } catch (Exception e) {
                Crashlytics.logException(e);
                activity.reportActionToAnalytics("claimOfferNoEmail", offer.id);
                Snackbar.make(activity.getViewForSnackbar(),
                        "Send us email at contact@eventshigh.com to redeem the offer",
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }
}
