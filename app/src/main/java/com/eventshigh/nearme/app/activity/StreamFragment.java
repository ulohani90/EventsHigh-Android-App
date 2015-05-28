package com.eventshigh.nearme.app.activity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
  private ListView listView;
  private StreamAdapter streamAdapter;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    StreamDbHelper dbHelper = new StreamDbHelper(getActivity());
    SQLiteDatabase database = dbHelper.getWritableDatabase();
    Cursor cursor = database.rawQuery("select * from " + StreamDbHelper.TABLE_NAME + " order by "
        + StreamDbHelper.COLUMN_TIMESTAMP + " desc;", null);

    View view = inflater.inflate(R.layout.fragment_notifications, container, false);
    streamAdapter = new StreamAdapter(getActivity(), cursor);
    listView = (ListView) view.findViewById(R.id.list);
    listView.setAdapter(streamAdapter);
    return view;
  }
}
