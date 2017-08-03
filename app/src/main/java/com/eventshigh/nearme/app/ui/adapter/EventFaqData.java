package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.EventZendeskTicketObject;

/**
 * Created by umesh on 15/03/17.
 */

public class EventFaqData implements AdapterData {

    EventZendeskTicketObject ticketObj;
    BaseActivity activity;

    public EventFaqData(EventZendeskTicketObject ticketObj, BaseActivity activity) {
        this.ticketObj = ticketObj;
        this.activity = activity;
    }

    @Override
    public DataType getType() {
        return DataType.EVENT_FAQ_CARD;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((EventFaqCard) card).bindData(ticketObj, activity);
    }

    @Override
    public String getId() {
        return ticketObj.question;
    }
}
