package com.eventshigh.nearme.app.activity;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;

/**
 * About Us Page.
 */
public class AboutUsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);
        TextView appVersion = (TextView) findViewById(R.id.app_version_text);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            appVersion.setText(version);
        } catch (PackageManager.NameNotFoundException e) {

        }
    }
}
