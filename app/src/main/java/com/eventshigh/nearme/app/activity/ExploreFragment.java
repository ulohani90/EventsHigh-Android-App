package com.eventshigh.nearme.app.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventsCollection.TagInfo;

import java.util.List;

/**
 * The explore screen which allows users to quickly see events by category.
 */
public class ExploreFragment extends Fragment {
    public static final String TAGS_LIST_PARAMETER = "tags";

    private GridView gridView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        gridView = (GridView) inflater.inflate(R.layout.fragment_explore, container, false);
        return gridView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ExploreCategoriesAdapter adapter = new ExploreCategoriesAdapter(getActivity(), R.id.explore_name);
        List<TagInfo> tagInfoList = getArguments().getParcelableArrayList(TAGS_LIST_PARAMETER);
        adapter.addAll(tagInfoList);
        gridView.setAdapter(adapter);
    }

    private class ExploreCategoriesAdapter extends ArrayAdapter<TagInfo> {
        public ExploreCategoriesAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null ? convertView :
                getActivity().getLayoutInflater().inflate(R.layout.explore_card, parent, false);
            final TagInfo tagInfo = getItem(position);
            ((TextView) view.findViewById(R.id.explore_name)).setText(tagInfo.toString());
            ((ImageView) view.findViewById(R.id.explore_image)).setImageResource(
                    getInfoGraphId(tagInfo.tagName));

            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((BaseEventsActivity) getActivity()).showSearchView(tagInfo.tagName);
                }
            });
            return view;
        }
    }

    private int getInfoGraphId(String tag) {
        try {
            return R.drawable.class.getField("infograph_" +
                    EventCategory.toCategoryParsableString(tag).toLowerCase()).getInt(null);
        } catch (IllegalAccessException| NoSuchFieldException e) {
            // Ignore
        }

        return R.drawable.eh_default_event_list;
    }
}
