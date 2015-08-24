package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

public class SmallHeaderData implements AdapterData {
    public final String header;

    public SmallHeaderData(String header) {
        this.header = header;
    }

    @Override
    public DataType getType() {
        return DataType.SMALL_HEADER;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        ((SmallHeaderCard) card).bindHeaderView(this);
    }

    @Override
    public String getId() {
        return header;
    }
}
