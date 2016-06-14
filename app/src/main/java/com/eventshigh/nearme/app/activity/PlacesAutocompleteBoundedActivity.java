package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.app.Activity;
import android.os.PersistableBundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.ui.adapter.GooglePlacesAutocompleteAdapter;

public class PlacesAutocompleteBoundedActivity extends AppCompatActivity implements TextWatcher{

    private GooglePlacesAutocompleteAdapter dataAdapter;
    EditText etSearchBar;
    ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.activity_places_autocomplete_bounded);
        Toolbar tb = (Toolbar) findViewById(R.id.toolbar_et);
        setSupportActionBar(tb);
        final ActionBar ab = getSupportActionBar();
        ab.setDisplayShowHomeEnabled(true); // show or hide the default home button
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowCustomEnabled(true); // enable overriding the default toolbar layout
        ab.setDisplayShowTitleEnabled(false); // disable the default title element here (for centered title)

        etSearchBar = (EditText)findViewById(R.id.et_search_place);

        dataAdapter = new  GooglePlacesAutocompleteAdapter(PlacesAutocompleteBoundedActivity.this,
                R.id.lv_search_place_list);
        listView = (ListView) findViewById(R.id.lv_search_place_list);
        listView.setAdapter(dataAdapter);
        listView.setTextFilterEnabled(true);
        etSearchBar.addTextChangedListener(this);
    }



    public void afterTextChanged(Editable s) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        dataAdapter.getFilter().filter(s.toString());
    }

}
