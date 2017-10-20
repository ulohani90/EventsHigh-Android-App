package com.eventshigh.nearme.app.ui.adapter;


import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventSession;

/**
 * Created by umesh on 16/09/16.
 */
public class EventSessionData implements AdapterData {

    EventSession session;
    String city;
    BaseContextActivity activity;

    public EventSessionData(EventSession session, BaseContextActivity activity, String city) {
        this.session = session;
        this.activity = activity;
        this.city = city;
    }

    @Override
    public DataType getType() {
        return DataType.EVENT_SESSION_CARD;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((EventSessionCard) card).bindData(session, activity, city);
    }

    @Override
    public String getId() {
        return session.getPkId() + "";
    }
}
