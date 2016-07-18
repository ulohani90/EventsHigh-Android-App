package com.eventshigh.nearme.app.ui.adapter;

import android.content.Intent;
import android.os.Build;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;

import com.eventshigh.nearme.app.activity.OfferSignUpActivity;
import com.eventshigh.nearme.app.activity.RedeemCouponActivity;
import com.eventshigh.nearme.app.activity.ReferralActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.stream.OfferObject;
import com.eventshigh.nearme.app.data.stream.VoucherObject;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import java.util.ArrayList;

/**
 * Created by umesh on 16/04/16.
 */
public class OfferCard extends RecyclerView.ViewHolder {


    private final TextView offerTitle;
    private final TextView offerDesc;
    private final TextView offerTime;
    private final ImageView offerBg;
    private final TextView callToAction;
    private final LinearLayout offerParent;

    public static OfferCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_offer, parent, false);
        return new OfferCard(view);
    }

    public OfferCard(View itemView) {
        super(itemView);
        this.offerTitle = (TextView) itemView.findViewById(R.id.offer_title);
        this.offerDesc = (TextView) itemView.findViewById(R.id.offer_desc);
        this.offerTime = (TextView) itemView.findViewById(R.id.offer_time);
        this.offerBg = (ImageView) itemView.findViewById(R.id.offer_bg);
        this.callToAction = (TextView) itemView.findViewById(R.id.call_to_action);
        this.offerParent = (LinearLayout) itemView.findViewById(R.id.offer_parent);

    }

    public void bindOfferView(final OfferObject offer, final BaseContextActivity activity, final long totalPoints) {
        Glide.with(activity).load(offer.imgUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                .into(offerBg);
        offerTitle.setText(offer.name);
        offerDesc.setText(offer.desc);
        if(offer.callToAction.equalsIgnoreCase("offer_redeem")){
            if(activity.isOfferActed(offer.id)){
                callToAction.setText("Already Redeemed");
            }else{
                callToAction.setText(offer.actionButtonText);
            }
        }else if(offer.callToAction.equalsIgnoreCase("offer_signup")){
            if(activity.isOfferActed(offer.id)){
                callToAction.setText("Already Signed up");
            }else{
                callToAction.setText(offer.actionButtonText);
            }
        }else{
            callToAction.setText(offer.actionButtonText);
        }

        if (offer.validTill > 0) {
            offerTime.setVisibility(View.VISIBLE);
            offerTime.setText(DateTimeUtils.getRemainingTime(offer.validTill));
        } else {
            offerTime.setVisibility(View.INVISIBLE);
        }
        offerParent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("offerclicked", offer.name);
                if (offer.callToAction.equalsIgnoreCase("offer_redeem")) {

                        if (isValidToUseCoupon(offer.vouchers, totalPoints)) {
                            // activity.showRedeemCouponActivity(offer, totalPoints);

                            Intent intent = new Intent(activity, RedeemCouponActivity.class);
                            intent.putExtra("offer", offer);
                            intent.putExtra("total_points", totalPoints);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                ActivityOptionsCompat options = ActivityOptionsCompat.
                                        makeSceneTransitionAnimation(activity, offerBg, activity.getString(R.string.activity_image_trans));

                                activity.startActivity(intent, options.toBundle());
                            } else {
                                activity.startActivity(intent);
                            }


                        } else {
                            activity.showMessage("You don't have enough points to claim this offer");

                        }


                } else if (offer.callToAction.equalsIgnoreCase("offer_signup")) {
                    // activity.showOfferSignUpActivity(offer);
                    if(activity.isOfferActed(offer.id)){
                        activity.showMessage("You have already signed up for this offer.");
                    }else {
                        Intent intent = new Intent(activity, OfferSignUpActivity.class);
                        intent.putExtra("offer", offer);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            ActivityOptionsCompat options = ActivityOptionsCompat.
                                    makeSceneTransitionAnimation(activity, offerBg, activity.getString(R.string.activity_image_trans));

                            activity.startActivity(intent, options.toBundle());
                        } else {
                            activity.startActivity(intent);
                        }
                    }
                    // activity.overridePendingTransition(R.anim.animate_bottom_up, R.anim.stay);
                } else if(offer.callToAction.startsWith("br")){
                    String query = offer.callToAction.substring(3, offer.callToAction.length()).replace("+"," ");
                    activity.showSearchView(query);
                }else if(offer.callToAction.startsWith("detail")){
                    String eventId = offer.callToAction.substring(7,offer.callToAction.length());
                    activity.showEventDetails(
                            EventsHighEndpoints.getEventDetailsURI(City.BANGALORE,eventId ), null);
                }
            }
        });
    }

    public boolean isValidToUseCoupon(ArrayList<VoucherObject> vouchers, long totalPoints) {
        for (VoucherObject voucher : vouchers) {
            if (voucher.pointsReq <= totalPoints) {
                return true;
            }
        }
        return true;
    }
}
