package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.MovieShowTimeObject;
import com.eventshigh.nearme.app.data.ShowDates;
import com.eventshigh.nearme.app.view.ZCustomFlowLayout;

import java.util.ArrayList;

/**
 * Created by umesh on 05/05/16.
 */
public class ShowTimeCard extends RecyclerView.ViewHolder {

    TextView date1;
    TextView date2;
    ZCustomFlowLayout timeLayout;
    TextView venueName;


    public static ShowTimeCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_movie_showtime, parent, false);
        return new ShowTimeCard(view);
    }

    public ShowTimeCard(View itemView) {
        super(itemView);
        date1 = (TextView) itemView.findViewById(R.id.date1);
        date2 = (TextView) itemView.findViewById(R.id.date2);
        timeLayout = (ZCustomFlowLayout) itemView.findViewById(R.id.showtimes);
        venueName = (TextView) itemView.findViewById(R.id.venue_name);
    }

    public void bindData(final MovieShowTimeObject obj, BaseActivity activity) {
        //int nearestDate  = getNearestDate(obj.getShowDates());
        timeLayout.setReceipentsForShowTimes(obj.getShowDates().get(0).getShowTimes(), true);

        venueName.setText(obj.getVenueName());
    }

    public int getNearestDate(ArrayList<ShowDates> dates) {
        int smallest = 0;
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).getDate() < dates.get(smallest).getDate()) {

            }
        }
        return smallest;
    }

}
