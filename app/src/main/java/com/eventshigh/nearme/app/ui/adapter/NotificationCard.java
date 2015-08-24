package com.eventshigh.nearme.app.ui.adapter;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.data.stream.StreamItem;
import com.eventshigh.nearme.app.utils.ContactUtils;

public class NotificationCard extends RecyclerView.ViewHolder {
    private final ImageView imageView;
    private final TextView titleView;
    private final TextView subtitleView;

    public NotificationCard(View itemView) {
        super(itemView);

        imageView = (ImageView) itemView.findViewById(R.id.stream_image);
        titleView = (TextView) itemView.findViewById(R.id.stream_title);
        subtitleView = (TextView) itemView.findViewById(R.id.stream_subtitle);
    }

    public void bindView(final StreamItem streamItem, final BaseContextActivity activity) {
        titleView.setText(streamItem.title);
        subtitleView.setText(DateUtils.getRelativeTimeSpanString(streamItem.timestamp));

        UserContact contact = streamItem.mobileNo == null ? null :
                ContactUtils.getContactForServerPhone(activity, streamItem.mobileNo);
        Drawable drawable = contact == null ? null : contact.getDrawable(activity,
                imageView.getLayoutParams().height);

        if (drawable == null) {
            imageView.setImageResource(R.drawable.ic_launcher);
        } else {
            imageView.setImageDrawable(drawable);
        }

        itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                streamItem.launch(activity);
            }
        });
    }
}
