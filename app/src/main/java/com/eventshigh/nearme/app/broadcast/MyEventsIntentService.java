package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by umesh on 12/03/16.
 */
public class MyEventsIntentService extends IntentService{
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public MyEventsIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }
}
