package com.eventshigh.nearme.app.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.eventshigh.nearme.app.data.City;

/**
* An @{link ArrayAdapter} which can be used to show City selection list.
*/
public class CityListAdapter extends ArrayAdapter<City> {
    public interface OnCitySelectionListener {
        void onCitySelection(City city);
    }

    private final OnCitySelectionListener citySelectionListener;
    public CityListAdapter(Context context, OnCitySelectionListener citySelectionListener) {
        super(context, android.R.layout.simple_list_item_1, android.R.id.text1);
        addAll(City.values());
        this.citySelectionListener = citySelectionListener;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                City city = getItem(position);
                citySelectionListener.onCitySelection(city);
            }
        });

        return view;
    }
}
