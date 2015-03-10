package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Intent;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.utils.IntentUtils;

public class DownloadEventsIntentService extends IntentService
        implements Response.ErrorListener, Response.Listener<MyEventsRequest.MyEvents> {
    public DownloadEventsIntentService() {
        super("DownloadEventsIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        EventsContext eventsContext = new EventsContext(null, "");
//        new MyEventsRequest(this, eventsContext, Request.Priority.IMMEDIATE,
//                true /* shouldBypassCache */, true /* includeWithoutLocation */,
//                this, this).execute();

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        DownloadEventsBroadcastReceiver.completeWakefulIntent(intent);
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {

    }

    @Override
    public void onResponse(MyEventsRequest.MyEvents pairs, boolean b) {

    }
}
