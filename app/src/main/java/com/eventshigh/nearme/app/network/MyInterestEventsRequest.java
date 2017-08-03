package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by umesh on 22/04/16.
 */
public class MyInterestEventsRequest extends AsyncTask<Void, Void, List<MyEventsRequest.TopicEvents>> {


    private final Context context;
    private final EventsContext eventsContext;
    private final Request.Priority priority;
    private final boolean shouldBypassCache;
    private final boolean includeWithoutLocation;
    private final Response.Listener<List<TopicEvents>> listener;
    private final Response.ErrorListener errorListener;
    private final Object tag;
    private boolean isRequestCancelled = false;

    public MyInterestEventsRequest(Context context, EventsContext eventsContext, Request.Priority priority,
                                   Object tag, boolean shouldBypassCache, boolean includeWithoutLocation,
                                   Response.Listener<List<TopicEvents>> listener, Response.ErrorListener errorListener) {
        this.context = context;
        this.eventsContext = eventsContext;
        this.priority = priority;
        this.shouldBypassCache = shouldBypassCache;
        this.includeWithoutLocation = includeWithoutLocation;
        this.listener = listener;
        this.errorListener = errorListener;
        this.tag = tag;
    }

    public
    @Nullable
    MyEventsRequest.TopicEvents getNonEmptyInterest() {
        List<TopicEvents> topicEventsList = doInBackground();
        for (TopicEvents events : topicEventsList) {
            if (events.numEvents > 0) {
                return events;
            }
        }

        return null;
    }


    @Override
    protected List<MyEventsRequest.TopicEvents> doInBackground(Void... params) {
        List<TopicEvents> result = new ArrayList<>();

        RequestFuture<List<TopicEvents>> eventsFuture = RequestFuture.newFuture();
        MobileUserEventsRequest.submit(context, eventsContext,
                priority, tag, shouldBypassCache, includeWithoutLocation, eventsFuture, eventsFuture);
        try {
            addTopicEventsToResult(result, eventsFuture);
            return result;
        } catch (RequestCancelledException e) {
            e.printStackTrace();
            isRequestCancelled = true;
            return null;
        }

    }

    @Override
    protected void onPostExecute(List<TopicEvents> result) {
        if (isRequestCancelled) {
            return;
        }
        if (result != null) {
            listener.onResponse(result, false);
        } else {
            errorListener.onErrorResponse(new VolleyError("no response"));
        }

    }

    private static void addTopicEventsToResult(List<TopicEvents> result, RequestFuture<List<TopicEvents>> eventsFuture) throws RequestCancelledException {

        try {
            result.addAll(eventsFuture.get(10, TimeUnit.SECONDS));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            if (eventsFuture.isCancelled()) {
                throw new RequestCancelledException();
            }
            Crashlytics.getInstance().core.logException(e);
        }

    }

    private static class RequestCancelledException extends Exception {

    }
}
