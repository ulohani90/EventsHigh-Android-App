package com.eventshigh.nearme.app.ui.adapter;

import android.content.Intent;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BlogEntryActivity;
import com.eventshigh.nearme.app.data.BlogEntry;
import com.eventshigh.nearme.app.network.VolleyHelper;

public class BlogEntryCard extends ViewHolder {
    private final NetworkImageView imageView;
    private final TextView titleView;
    private final TextView dateView;

    public static BlogEntryCard newInstance(BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_blog_entry, parent, false);
        return new BlogEntryCard(view);
    }

    public BlogEntryCard(View itemView) {
        super(itemView);

        imageView = (NetworkImageView) itemView.findViewById(R.id.blog_image);
        titleView = (TextView) itemView.findViewById(R.id.blog_title);
        dateView = (TextView) itemView.findViewById(R.id.blog_date);
    }

    public void bindView(final BlogEntry blogEntry, final BaseActivity activity) {
        imageView.setDefaultImageResId(R.drawable.eh_default_event);
        imageView.setErrorImageResId(R.drawable.eh_default_event);
        imageView.setImageUrl(blogEntry.thumbnail, VolleyHelper.getImageLoader(activity));
        titleView.setText(blogEntry.title);
        dateView.setText(DateUtils.getRelativeTimeSpanString(blogEntry.pubDate.getTime()));
        itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, BlogEntryActivity.class);
                intent.putExtra(BlogEntryActivity.EXTRA_BLOG_ENTRY_PARAM, blogEntry);
                activity.startActivity(intent);
            }
        });
    }
}
