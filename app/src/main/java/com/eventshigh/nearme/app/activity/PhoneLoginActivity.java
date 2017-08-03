package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.eventshigh.nearme.app.R;

/**
 * This screen allows user to register and verify his phone number.
 */
public class PhoneLoginActivity extends BaseActivity {

    PhoneLoginFragment fragment;

    boolean showSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.empty_layout);
        FrameLayout container = (FrameLayout) findViewById(R.id.container);

        showSkip = getIntent().getBooleanExtra("hide_skip", false);
        boolean isLogout = getIntent().getBooleanExtra("is_logout", false);

        fragment = PhoneLoginFragment.newInstance(showSkip, isLogout, true);

        getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).commit();
    }

    @Override
    public View getViewForSnackbar() {
        if (fragment != null && fragment.getCodeView() != null) {
            return fragment.getCodeView();
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.register_phone_menu, menu);
        MenuItem skipBtn = menu.findItem(R.id.action_skip_register_phone);
        skipBtn.setVisible(showSkip);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_skip_register_phone) {

            if (fragment != null)
                fragment.startNextTask();
        }
        return super.onOptionsItemSelected(item);
    }
}
