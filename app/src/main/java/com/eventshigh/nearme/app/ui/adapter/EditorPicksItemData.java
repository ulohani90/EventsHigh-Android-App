package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.Event;

/**
 * Created by umesh on 09/11/17.
 */

public class EditorPicksItemData implements AdapterData {
    Event event;

    BaseContextActivity activity;

    int width;

    public EditorPicksItemData(Event event, BaseContextActivity activity, int width) {
        this.event = event;
        this.activity = activity;
        this.width = width;
    }

    @Override
    public DataType getType() {
        return DataType.EDITOR_PICKS_ITEM_CARD;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((EditorPicksItemCard) card).populate(activity, event, width);
    }

    @Override
    public String getId() {
        return event.title;
    }
}
