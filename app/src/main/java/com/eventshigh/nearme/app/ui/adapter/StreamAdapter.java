package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.stream.QueryNotificationStreamItem;
import com.eventshigh.nearme.app.data.stream.StreamItem;

import java.util.ArrayList;
import java.util.List;

public class StreamAdapter extends RecyclerView.Adapter<NotificationCard> {
    private final BaseContextActivity activity;
    private List<StreamItem> streamItems;

    public StreamAdapter(BaseContextActivity activity) {
        this.activity = activity;

        streamItems = new ArrayList<>();
        streamItems.add(new QueryNotificationStreamItem(System.currentTimeMillis(),
            "What's Happening Today", "Check out whats happending in city today", null, null, "today"));
    }

    public void setStreamItems(List<StreamItem> streamItems) {
        if (! streamItems.isEmpty()) {
            this.streamItems = streamItems;
            notifyDataSetChanged();
        }
    }

    @Override
    public NotificationCard onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_stream, parent, false);
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
}
