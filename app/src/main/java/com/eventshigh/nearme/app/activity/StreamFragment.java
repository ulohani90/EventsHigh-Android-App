package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.StreamDbHelper;
import com.eventshigh.nearme.app.ui.StreamAdapter;

public class StreamFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        StreamAdapter streamAdapter = new StreamAdapter(getActivity(),
                StreamDbHelper.getCursorToStreamItems(getActivity()));
        ListView listView = (ListView) view.findViewById(R.id.list);
        listView.setAdapter(streamAdapter);
        return view;
    }
}
