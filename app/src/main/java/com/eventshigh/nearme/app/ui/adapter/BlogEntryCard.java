package com.eventshigh.nearme.app.ui.adapter;

import android.content.Intent;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BlogEntryActivity;
import com.eventshigh.nearme.app.data.BlogEntry;
import com.eventshigh.nearme.app.ui.CircleTransform;

public class BlogEntryCard extends ViewHolder {
    private final ImageView imageView;
    private final TextView titleView;
    private final TextView dateView;

    public static BlogEntryCard newInstance(BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_blog_entry, parent, false);
        return new BlogEntryCard(view);
    }

    public BlogEntryCard(View itemView) {
        super(itemView);

        imageView = (ImageView) itemView.findViewById(R.id.blog_image);
        titleView = (TextView) itemView.findViewById(R.id.blog_title);
        dateView = (TextView) itemView.findViewById(R.id.blog_date);
    }

    public void bindView(final BlogEntry blogEntry, final BaseActivity activity) {
        Glide.with(activity).load(blogEntry.thumbnailSmall)
                .placeholder(R.drawable.ic_launcher).crossFade().centerCrop().transform(new CircleTransform(activity))
                .into(imageView);
        titleView.setText(Html.fromHtml(blogEntry.title));
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
