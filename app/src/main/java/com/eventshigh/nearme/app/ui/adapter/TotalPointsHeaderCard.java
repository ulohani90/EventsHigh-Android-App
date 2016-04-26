package com.eventshigh.nearme.app.ui.adapter;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;

import com.eventshigh.nearme.app.activity.PointsBreakdownActivity;


/**
 * Created by umesh on 18/04/16.
 */
public class TotalPointsHeaderCard extends RecyclerView.ViewHolder{

    TextView pointsCount;
    LinearLayout walletLayout;
    LinearLayout messageLayout;
    TextView contactUs;

    TextView clickDesc;


    public static TotalPointsHeaderCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_total_points_header, parent, false);
        return new TotalPointsHeaderCard(view);
    }

    public TotalPointsHeaderCard(View itemView) {
        super(itemView);
        pointsCount = (TextView)itemView.findViewById(R.id.points);
        walletLayout = (LinearLayout)itemView.findViewById(R.id.wallet_layout);
        messageLayout = (LinearLayout)itemView.findViewById(R.id.message_layout);
        contactUs = (TextView)itemView.findViewById(R.id.contact_us);
        clickDesc = (TextView)itemView.findViewById(R.id.click_desc);
    }

    public void bindTotalPointView(long totalPoints , final BaseContextActivity activity ,boolean showMessage,boolean showCallToAction,boolean showBreakdown ){
        if(showMessage){
            walletLayout.setVisibility(View.GONE);
            messageLayout.setVisibility(View.VISIBLE);
            SpannableString content = new SpannableString("Contact us");
            content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
            contactUs.setVisibility(View.GONE);


        }else {
            walletLayout.setVisibility(View.VISIBLE);
            if(showBreakdown){
                messageLayout.setVisibility(View.VISIBLE);
                SpannableString content = new SpannableString("Contact us");
                content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                contactUs.setText(content);
                contactUs.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(activity, FeedbackActivity.class);
                        activity.startActivity(intent);
                    }
                });
            }else {
                messageLayout.setVisibility(View.GONE);
            }
            pointsCount.setText(totalPoints + " points");

            if(showCallToAction){
            walletLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(activity, PointsBreakdownActivity.class);
                    activity.startActivity(intent);
                }
            });

                clickDesc.setVisibility(View.VISIBLE);
                SpannableString content = new SpannableString("Details");
                content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                clickDesc.setText(content);

            }else{
                clickDesc.setVisibility(View.GONE);
            }
        }
    }
}
