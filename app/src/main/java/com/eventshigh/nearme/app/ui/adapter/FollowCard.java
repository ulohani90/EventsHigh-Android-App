package com.eventshigh.nearme.app.ui.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.ReferralActivity;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.user.Account;

import java.text.MessageFormat;

public class FollowCard extends ViewHolder {
    private TextView titleView;
    private TextView subtitleView;
    private View followButton;
    private View followingButton;

    private View discountCoupon;

    private TextView couponCode;
    private TextView discountValue;
    private TextView discountValidty;
    private TextView viewAll;


    public static FollowCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_follow, parent, false);
        return new FollowCard(view);
    }

    public FollowCard(View itemView) {
        super(itemView);

        titleView = (TextView) itemView.findViewById(R.id.cat_title);
        subtitleView = (TextView) itemView.findViewById(R.id.subtitle);
        followButton = itemView.findViewById(R.id.follow_button);
        followingButton = itemView.findViewById(R.id.following_button);

        discountCoupon = itemView.findViewById(R.id.discount_coupon);
        couponCode = (TextView)itemView.findViewById(R.id.discount_code);
        discountValue = (TextView)itemView.findViewById(R.id.discount_value);
        discountValidty = (TextView)itemView.findViewById(R.id.discount_validity);
        viewAll = (TextView)itemView.findViewById(R.id.view_all);

    }

    public void populate(final FollowData data) {
        titleView.setText(data.title);
        subtitleView.setText(MessageFormat.format(
                data.activity.getString(R.string.num_events), data.numFollowers, data.numEvents));

        final Account account = new Account(data.activity);
        setFollowButtons(account.isFollowing(data.title));
        followButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                data.activity.reportActionToAnalytics("addFollowing", data.title);
                if (!account.getUserInfo().isVerified) {
                    PhoneVerificationDialog.show(data.activity,
                            R.string.ui_verify_phone, R.string.ui_phone_verify_pa);
                }
                account.setIsFollowing(data.title, true);
                setFollowButtons(true);
            }
        });
        followingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                data.activity.reportActionToAnalytics("removeFollowing", data.title);
                account.setIsFollowing(data.title, false);
                setFollowButtons(false);
            }
        });

        if(data.special!=null){
            discountCoupon.setVisibility(View.VISIBLE);
            discountValue.setText("₹ " + data.special.coupon.amount);
            couponCode.setText(data.special.coupon.code);

                discountValidty.setVisibility(View.VISIBLE);
                discountValidty.setText("Click to copy coupon code to clipboard");
                viewAll.setVisibility(View.GONE);
               /* viewAll.setVisibility(View.VISIBLE);
                SpannableString content = new SpannableString(data.activity.getResources().getString(R.string.view_all));
                content.setSpan(new UnderlineSpan(), 0, content.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                viewAll.setText(content);

                viewAll.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(data.activity, ReferralActivity.class);
                        data.activity.startActivity(intent);
                    }
                });
            }else{
                discountValidty.setVisibility(View.GONE);
                viewAll.setVisibility(View.GONE);
            }*/

            discountCoupon.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) data.activity.getSystemService(data.activity.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Coupon Code", data.special.coupon.code);
                    clipboard.setPrimaryClip(clip);
                    data.activity.showMessage(data.special.coupon.code + " coupon code copied to Clipboard");
                }
            });



        } else {
            discountCoupon.setVisibility(View.GONE);
        }


    }

    public void setFollowButtons(boolean isFollowing) {
        followButton.setVisibility(isFollowing ? View.GONE : View.VISIBLE);
        followingButton.setVisibility(isFollowing ? View.VISIBLE : View.GONE);
        followingButton.setSelected(true);
    }


}
