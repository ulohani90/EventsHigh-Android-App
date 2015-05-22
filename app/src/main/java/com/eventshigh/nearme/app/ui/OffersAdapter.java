package com.eventshigh.nearme.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.EventDetailActivity;
import com.eventshigh.nearme.app.data.Offer;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;

import java.util.List;

/**
 * Adapter used to show offers information in RecyclerView.
 */
public class OffersAdapter extends RecyclerView.Adapter<ViewHolder> {
    private final BaseActivity activity;
    private final List<Offer> offers;

    public OffersAdapter(BaseActivity activity, List<Offer> offers) {
        this.activity = activity;
        this.offers = offers;
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
            ((OffersHeaderCard) card).populate(activity);
        }
        if (position > 0) {
            ((OfferCard) card).populate(offers.get(position - 1), activity);
        }
    }

    @Override
    public int getItemCount() {
        return offers.size() + 1;
    }


    public static class OffersHeaderCard extends ViewHolder {
        private final TextView numPointsView;
        private final TextView inviteLinkView;

        private final View inviteViaFB;
        private final View inviteViaTwitter;
        private final View inviteViaEmail;
        private final View inviteViaWhatsapp;

        public OffersHeaderCard(View itemView) {
            super(itemView);

            numPointsView = (TextView) itemView.findViewById(R.id.num_points);
            inviteLinkView = (TextView) itemView.findViewById(R.id.invite_link);

            inviteViaFB = itemView.findViewById(R.id.share_fb);
            inviteViaTwitter = itemView.findViewById(R.id.share_twitter);
            inviteViaEmail = itemView.findViewById(R.id.share_email);
            inviteViaWhatsapp = itemView.findViewById(R.id.share_whatsapp);
        }

        public void populate(final BaseActivity activity) {
            numPointsView.setText(Preferences.getInstance(activity).getPoints());

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
                    Toast.makeText(activity, R.string.ui_invite_link_copy, Toast.LENGTH_SHORT).show();
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

    public static class OfferCard extends ViewHolder {
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

        public void populate(Offer offer, final BaseActivity activity) {
            imageView.setDefaultImageResId(R.drawable.eh_default_event);
            imageView.setDefaultImageResId(R.drawable.eh_default_event);
            imageView.setImageUrl(offer.imgUrl.toString(), VolleyHelper.getImageLoader(activity));

            messageView.setText(offer.message);

            actionView.setText(offer.actionName);
            actionView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(activity, "To Be Implemented ...", Toast.LENGTH_SHORT).show();
                }
            });
            expiredView.setVisibility(offer.isExpired() ? View.VISIBLE : View.GONE);
        }

    }
}
