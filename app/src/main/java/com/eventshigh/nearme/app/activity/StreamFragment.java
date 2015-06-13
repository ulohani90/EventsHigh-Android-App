package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.StreamDbHelper;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.ui.StreamAdapter;

public class StreamFragment extends Fragment {
    private BaseContextActivity activity;
    private RecyclerView gridView;
    private StreamAdapter streamAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseContextActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        gridView = (RecyclerView) view.findViewById(R.id.grid);

        streamAdapter = new StreamAdapter(activity);
        gridView.setAdapter(streamAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        gridView.setOnScrollListener(new HideActionBarOnScroll(activity));
        streamAdapter.setStreamItems(StreamDbHelper.getStreamItems(activity));
    }
}
