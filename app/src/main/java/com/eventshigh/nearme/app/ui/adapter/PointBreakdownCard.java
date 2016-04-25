package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.network.MyPointsBreakdownRequest.PointBreakDown;
import com.eventshigh.nearme.app.utils.DateTimeUtils;

/**
 * Created by umesh on 22/04/16.
 */
public class PointBreakdownCard extends RecyclerView.ViewHolder {


    TextView pointCount;
    TextView pointName;
    TextView pointTime;
    ImageView pointIcon;
    TextView pointHeader;

    public PointBreakdownCard(View itemView) {
        super(itemView);
        pointCount = (TextView) itemView.findViewById(R.id.point_count);
        pointName = (TextView) itemView.findViewById(R.id.point_name);
        pointTime = (TextView) itemView.findViewById(R.id.point_date);
        pointIcon = (ImageView) itemView.findViewById(R.id.point_logo);
        pointHeader = (TextView)itemView.findViewById(R.id.point_header);

    }


    public static PointBreakdownCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_point_breakdown, parent, false);
        return new PointBreakdownCard(view);
    }


    public void bindView(PointBreakDown obj, BaseContextActivity activity) {
        pointCount.setText(obj.points +((obj.points>1)? " Points":" Point"));
        pointName.setText(obj.message);
        pointTime.setText(DateTimeUtils.getPointAddedOnString(obj.addedOn));

        if (obj.type.equalsIgnoreCase("credit")) {
            pointIcon.setBackgroundResource(R.drawable.credit_icon_bg);
        } else {
            pointIcon.setBackgroundResource(R.drawable.debit_icon_bg);
        }

        int resourceId = 0;
        if (obj.action.equalsIgnoreCase("favorite")) {
            resourceId = R.drawable.ic_fav_point;
        } else if (obj.action.equalsIgnoreCase("share")) {
            resourceId = R.drawable.ic_share_point;
        } else if (obj.action.equalsIgnoreCase("Welcome to EH")) {
            resourceId = R.drawable.ic_refer_point;
        } else if (obj.action.equalsIgnoreCase("ticket")) {
            resourceId = R.drawable.ic_ticket_point;
        } else if (obj.action.equalsIgnoreCase("review")) {
            resourceId = R.drawable.ic_review_point;
        } else if (obj.action.equalsIgnoreCase("redeem")) {
            resourceId = R.drawable.ic_redeem_point;
        }else{
            if(obj.type.equalsIgnoreCase("credit")){
                resourceId = R.drawable.ic_credit_default;
            }else{
                resourceId = R.drawable.ic_debit_default;
            }

        }

        pointIcon.setImageResource(resourceId);
        pointHeader.setText(obj.action);

    }

}
