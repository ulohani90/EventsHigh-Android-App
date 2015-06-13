package com.eventshigh.nearme.app.ui;

import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.stream.QueryNotificationStreamItem;
import com.eventshigh.nearme.app.data.stream.StreamItem;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.StreamAdapter.NotificationCard;

import java.util.ArrayList;
import java.util.List;

public class StreamAdapter extends RecyclerView.Adapter<NotificationCard> {
    private final BaseContextActivity activity;
    private List<StreamItem> streamItems;

    public StreamAdapter(BaseContextActivity activity) {
        this.activity = activity;

        streamItems = new ArrayList<>();
        streamItems.add(new QueryNotificationStreamItem(System.currentTimeMillis(),
            "What's Happening Today", "Check out whats happending in city today", null, "today"));
    }

    public void setStreamItems(List<StreamItem> streamItems) {
        if (! streamItems.isEmpty()) {
            this.streamItems = streamItems;
            notifyDataSetChanged();
        }
    }

    @Override
    public NotificationCard onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.stream_item_notification, parent, false);
        return new NotificationCard(view);
    }

    @Override
    public void onBindViewHolder(NotificationCard card, int position) {
        card.bindView(streamItems.get(position), activity);
    }

    @Override
    public int getItemCount() {
        return streamItems.size();
    }

    public class NotificationCard extends RecyclerView.ViewHolder {
        private final TextView timeView;
        private final TextView titleView;
        private final TextView messageView;
        private final NetworkImageView imageView;

        public NotificationCard(View itemView) {
            super(itemView);

            timeView = (TextView) itemView.findViewById(R.id.time);
            titleView = (TextView) itemView.findViewById(R.id.title);
            messageView = (TextView) itemView.findViewById(R.id.message);
            imageView = (NetworkImageView) itemView.findViewById(R.id.image);
        }

        public void bindView(final StreamItem streamItem, final BaseContextActivity activity) {
            timeView.setText(DateUtils.getRelativeTimeSpanString(streamItem.timestamp));
            titleView.setText(streamItem.title);
            messageView.setText(Html.fromHtml(streamItem.message));

            imageView.setDefaultImageResId(R.drawable.eh_default_event);
            imageView.setErrorImageResId(R.drawable.eh_default_event);
            if (streamItem.imgUrl == null) {
                imageView.setVisibility(View.GONE);
            } else {
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageUrl(streamItem.imgUrl, VolleyHelper.getImageLoader(activity));
            }

            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    streamItem.launch(activity);
                }
            });
        }
    }
}
