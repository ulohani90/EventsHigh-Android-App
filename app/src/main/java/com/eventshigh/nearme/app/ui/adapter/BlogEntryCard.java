package com.eventshigh.nearme.app.ui.adapter;

import android.net.Uri;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.BlogEntry;

public class BlogEntryCard extends ViewHolder {
    private final ImageView imageView;
    private final TextView titleView;
    private final TextView subtitleView;

    public BlogEntryCard(View itemView) {
        super(itemView);

        imageView = (ImageView) itemView.findViewById(R.id.stream_image);
        titleView = (TextView) itemView.findViewById(R.id.stream_title);
        subtitleView = (TextView) itemView.findViewById(R.id.stream_subtitle);
    }

    public void bindView(final BlogEntry blogEntry, final BaseActivity activity) {
        titleView.setText(blogEntry.title);
        subtitleView.setText(DateUtils.getRelativeTimeSpanString(blogEntry.pubDate.getTime()));
        imageView.setImageURI(Uri.parse(blogEntry.imgUrl));

        itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // blogEntry.launch(activity);
            }
        });
    }
}
