package com.eventshigh.nearme.app.ui.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseEventsFragment;
import com.eventshigh.nearme.app.utils.FontUtils;

import java.text.MessageFormat;

public class HeaderCard extends ViewHolder {
    private final TextView titleView;
    private final TextView numEventsView;
    private final View moreView;

    public static HeaderCard newInstance(Activity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_header, parent, false);
        return new HeaderCard(view);
    }

    public HeaderCard(View cardView) {
        super(cardView);
        this.titleView = (TextView) cardView.findViewById(R.id.header);
        this.numEventsView = (TextView) cardView.findViewById(R.id.num_events);
        this.moreView = cardView.findViewById(R.id.header_more);
    }

    public void bindHeaderView(final BaseEventsFragment eventsFragment, final HeaderData header) {
        titleView.setText(header.header);
        FontUtils.setTypefaceQuicksandBold(titleView);
        if (header.numEvents <= 0) {
            numEventsView.setVisibility(View.GONE);
        } else {
            numEventsView.setVisibility(View.VISIBLE);
            numEventsView.setText(MessageFormat.format(
                    eventsFragment.getContextActivity().getString(R.string.num_events),
                    0, header.numEvents));
        }

        if (header.showMore()) {
            moreView.setVisibility(View.VISIBLE);
            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    eventsFragment.showSearchView(header.header);
                }
            });
        } else {
            moreView.setVisibility(View.GONE);
            itemView.setClickable(false);
        }
    }
}
