package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.app.Activity;
import android.os.PersistableBundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.ui.adapter.GooglePlacesAutocompleteAdapter;
import com.eventshigh.nearme.app.user.Account;

public class PlacesAutocompleteBoundedActivity extends AppCompatActivity implements TextWatcher {

    private GooglePlacesAutocompleteAdapter dataAdapter;
    EditText etSearchBar;
    ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places_autocomplete_bounded);
        Toolbar tb = (Toolbar) findViewById(R.id.toolbar_et);
        setSupportActionBar(tb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etSearchBar = (EditText) findViewById(R.id.et_search_place);
        Account account = new Account(this);
        dataAdapter = new GooglePlacesAutocompleteAdapter(PlacesAutocompleteBoundedActivity.this, account.getLastCity().name(), account.getLastCity().cityBounds);
        listView = (ListView) findViewById(R.id.lv_search_place_list);
        listView.setAdapter(dataAdapter);
        listView.setTextFilterEnabled(true);
        etSearchBar.addTextChangedListener(this);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void afterTextChanged(Editable s) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        dataAdapter.getFilter().filter(s.toString());
    }

}
