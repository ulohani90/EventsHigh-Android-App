package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;

/**
 * Created by umesh on 18/04/16.
 */
public class TotalPointsHeaderCard extends RecyclerView.ViewHolder{

    TextView pointsCount;


    public static TotalPointsHeaderCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_total_points_header, parent, false);
        return new TotalPointsHeaderCard(view);
    }

    public TotalPointsHeaderCard(View itemView) {
        super(itemView);
        pointsCount = (TextView)itemView.findViewById(R.id.points);
    }

    public void bindTotalPointView(long totalPoints , BaseContextActivity activity  ){
        pointsCount.setText(totalPoints+" points");
    }
}
