package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;

import org.apmem.tools.layouts.FlowLayout;

import java.util.ArrayList;

/**
 * Shows the filters screen showing various categories and let user select few
 * and go back to event list where filter is applied.
 */
public class ShowFiltersActivity extends BaseActivity {
    public static final String PARAM_FILTERS = ShowFiltersActivity.class.getName() + ".filters";

    private ArrayList<String> selectedTags = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_filters);

        if (getIntent().hasExtra(PARAM_FILTERS)) {
            selectedTags = getIntent().getStringArrayListExtra(PARAM_FILTERS);
        }
        FlowLayout categoryContainer = (FlowLayout) findViewById(R.id.category_container);
        for (final String tag : LaunchActivity.EXPLORE_TAGS) {
            if (tag.equalsIgnoreCase("All")) {
                continue;
            }

            FrameLayout card = (FrameLayout) getLayoutInflater().inflate(
                    R.layout.filter_card, categoryContainer, false);
            final TextView textView = (TextView) card.getChildAt(0);
            textView.setText(tag);
            textView.setSelected(selectedTags.contains(tag));
            categoryContainer.addView(card);

            card.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (textView.isSelected()) {
                        selectedTags.remove(tag);
                        textView.setSelected(false);
                    } else {
                        selectedTags.add(tag);
                        textView.setSelected(true);
                    }
                }
            });
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void showAll(View view) {
        selectedTags.clear();
        applyFilters(view);
    }

    public void applyFilters(View view) {
        Intent result = new Intent();
        result.putExtra(PARAM_FILTERS, selectedTags);
        setResult(RESULT_OK, result);
        finish();
    }
}
