package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.Adapter;
import android.view.View;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.BlogEntry;

import java.util.ArrayList;
import java.util.List;

public class BlogEntriesAdapter extends Adapter<BlogEntryCard> {
    private final BaseActivity activity;
    private List<BlogEntry> blogEntries;

    public BlogEntriesAdapter(BaseActivity activity) {
        this.activity = activity;
        blogEntries = new ArrayList<>();
    }

    public void setBlogEntries(List<BlogEntry> blogEntries) {
        if (! blogEntries.isEmpty()) {
            this.blogEntries = blogEntries;
            notifyDataSetChanged();
        }
    }

    @Override
    public BlogEntryCard onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_stream, parent, false);
        return new BlogEntryCard(view);
    }

    @Override
    public void onBindViewHolder(BlogEntryCard card, int position) {
        card.bindView(blogEntries.get(position), activity);
    }

    @Override
    public int getItemCount() {
        return blogEntries.size();
    }
}
