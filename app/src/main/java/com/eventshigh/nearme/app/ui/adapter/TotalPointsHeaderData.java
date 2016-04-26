package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseContextActivity;

/**
 * Created by umesh on 18/04/16.
 */
public class TotalPointsHeaderData implements AdapterData {

    private final long totalPoints;
    private final BaseContextActivity activity;
    final boolean showMessage;
    final boolean showCallToAction;
    boolean showBreakdown;


    public TotalPointsHeaderData(long totalPoints, BaseContextActivity activity,boolean showMessage,boolean showCallToAction,boolean showBreakdown) {
        this.totalPoints = totalPoints;
        this.activity = activity;
        this.showMessage = showMessage;
        this.showCallToAction = showCallToAction;
        this.showBreakdown = showBreakdown;
    }

    @Override
    public DataType getType() {
        return DataType.TOTAL_POINT_HEADER;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((TotalPointsHeaderCard) card).bindTotalPointView(totalPoints, activity,showMessage,showCallToAction,showBreakdown);
    }

    @Override
    public String getId() {
        return totalPoints + "";
    }
}
