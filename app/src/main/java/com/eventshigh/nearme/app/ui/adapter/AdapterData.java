package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

public interface AdapterData {
    DataType getType();

    void onBindViewHolder(ViewHolder card, int position);

    String getId();
}
