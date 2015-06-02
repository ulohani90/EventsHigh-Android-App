package com.eventshigh.nearme.app.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.StreamDbHelper;
import com.eventshigh.nearme.app.data.stream.StreamItem;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.NotificationUtils;

import org.json.JSONException;

public class StreamAdapter extends RecyclerView.Adapter<StreamAdapter.ViewHolder> {
    private final Context context;
    private final CursorAdapter cursorAdapter;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
        }
    }

    public StreamAdapter(Context context, Cursor cursor) {
        this.context = context;

        cursorAdapter = new CursorAdapter(context, cursor, 0) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return LayoutInflater.from(context).inflate(R.layout.stream_item_notification,
                    parent, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                TextView timeView = (TextView) view.findViewById(R.id.time);
                TextView titleView = (TextView) view.findViewById(R.id.title);
                TextView messageView = (TextView) view.findViewById(R.id.message);
                NetworkImageView imageView = (NetworkImageView) view.findViewById(R.id.image);

                try {
                    StreamItem streamItem = StreamDbHelper.parseFromCursor(cursor);
                    view.setTag(streamItem);

                    timeView.setText(DateUtils.getRelativeTimeSpanString(streamItem.timestamp));
                    titleView.setText(streamItem.title);
                    messageView.setText(Html.fromHtml(streamItem.message));

                    if (streamItem.imgUrl != null) {
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setDefaultImageResId(R.drawable.eh_default_event);
                        imageView.setImageUrl(streamItem.imgUrl, VolleyHelper.getImageLoader(context));
                    } else {
                        imageView.setVisibility(View.GONE);
                    }
                } catch (JSONException e) {
                    Crashlytics.logException(e);
                    e.printStackTrace();

                    timeView.setText("");
                    timeView.setText("Error");
                    messageView.setText("Error in loading data. sorry!");
                }
            }
        };
    }

    @Override
    public StreamAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = cursorAdapter.newView(context, cursorAdapter.getCursor(), parent);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(StreamAdapter.ViewHolder holder, int position) {
        cursorAdapter.getCursor().moveToPosition(position);
        cursorAdapter.bindView(holder.itemView, context, cursorAdapter.getCursor());
    }

    @Override
    public int getItemCount() {
        return cursorAdapter.getCount();
    }
}
