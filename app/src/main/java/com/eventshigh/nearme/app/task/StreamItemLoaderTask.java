package com.eventshigh.nearme.app.task;

import android.content.Context;

import com.eventshigh.nearme.app.data.StreamDbHelper;
import com.eventshigh.nearme.app.data.stream.StreamItem;

import java.util.List;

import io.fabric.sdk.android.services.concurrency.AsyncTask;

/**
 * AsyncTask which can be used to load the StreamItems.
 */
public class StreamItemLoaderTask extends AsyncTask<Void, Void, List<StreamItem>>  {
    public interface StreamItemsCallback {
        void onContactLoad(List<StreamItem> streamItems);
    }

    private final Context context;
    private final StreamItemsCallback callback;

    public StreamItemLoaderTask(Context context, StreamItemsCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected
    List<StreamItem> doInBackground(Void... params) {
        return StreamDbHelper.getStreamItems(context);
    }

    @Override
    protected void onPostExecute(List<StreamItem> streamItems) {
        callback.onContactLoad(streamItems);
    }
}
