package com.eventshigh.nearme.app.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.StreamDbHelper;
import com.eventshigh.nearme.app.data.stream.StreamItem;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.StreamAdapter.NotificationCard;

import org.json.JSONException;

public class StreamAdapter extends RecyclerView.Adapter<NotificationCard> {
    private final BaseContextActivity activity;
    private final CursorAdapter cursorAdapter;

    public StreamAdapter(BaseContextActivity activity, Cursor cursor) {
        this.activity = activity;

        cursorAdapter = new CursorAdapter(StreamAdapter.this.activity, cursor, 0) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return LayoutInflater.from(context).inflate(R.layout.stream_item_notification,
                    parent, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                // not used.
            }
        };
    }

    @Override
    public NotificationCard onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = cursorAdapter.newView(activity, cursorAdapter.getCursor(), parent);
        return new NotificationCard(view);
    }

    @Override
    public void onBindViewHolder(NotificationCard card, int position) {
        try {
            cursorAdapter.getCursor().moveToPosition(position);
            StreamItem streamItem = StreamDbHelper.parseFromCursor(cursorAdapter.getCursor());
            card.bindView(streamItem, activity);
        } catch (JSONException e) {
            Crashlytics.logException(e);
            card.showErrorView();
        }
    }

    @Override
    public int getItemCount() {
        return cursorAdapter.getCount();
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

        public void showErrorView() {
            timeView.setText("");
            timeView.setText("Error");
            messageView.setText("Error in loading data. sorry!");
            itemView.setClickable(false);
        }

        public void bindView(final StreamItem streamItem, final BaseContextActivity activity) {
            timeView.setText(DateUtils.getRelativeTimeSpanString(streamItem.timestamp));
            titleView.setText(streamItem.title);
            messageView.setText(Html.fromHtml(streamItem.message));

            if (streamItem.imgUrl != null) {
                imageView.setVisibility(View.VISIBLE);
                imageView.setDefaultImageResId(R.drawable.eh_default_event);
                imageView.setImageUrl(streamItem.imgUrl, VolleyHelper.getImageLoader(activity));
            } else {
                imageView.setVisibility(View.GONE);
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
