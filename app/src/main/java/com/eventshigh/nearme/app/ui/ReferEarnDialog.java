package com.eventshigh.nearme.app.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.ReferralActivity;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.user.UserContactsUploader;

/**
 * Created by umesh on 09/04/16.
 */
public class ReferEarnDialog {

    public static final String REFER_IMAGE_URL = "https://assets.eventshigh.com/refer.jpg";
    public static void doNeedFull(BaseActivity activity ){
        Preferences preferences = Preferences.getInstance(activity);
         if ((preferences.getLastTimeReferShown() < System.currentTimeMillis() - (3*DateUtils.DAY_IN_MILLIS)) && preferences.shouldShowReferral()) {
             showDialog(activity);
        }
    }

    public static void showDialog(final BaseActivity activity ){
            Preferences preferences = Preferences.getInstance(activity);
            preferences.setLastTimeReferShown();

        VolleyHelper.addToRequestQueue(activity, new ImageRequest(REFER_IMAGE_URL,
            new Response.Listener<Bitmap>() {
                @Override
                public void onResponse(Bitmap bitmap, boolean isIntermediate) {
                    if (!isIntermediate && activity.isRunning()) {
                        show(activity);
                    }
                }
            }, 600, 900, ImageView.ScaleType.CENTER_CROP, null,
            new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

                Log.i("Error", "Something wrong");
                // do nothing.
            }
        }));
    }

    public static void show(final BaseActivity activity) {
        activity.reportActionToAnalytics("referralbannershown");
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Set the background image.
        View view = LayoutInflater.from(activity).inflate(R.layout.home_banner_layout, null);
        ImageView bannerImage = (ImageView)view.findViewById(R.id.banner);
        Glide.with(activity).load(REFER_IMAGE_URL)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .fitCenter()
                .into(bannerImage);
        builder.setView(view);

        final AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;

        (view.findViewById(R.id.banner_parent)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("referralbannerclick");
                Intent intent = new Intent(activity, ReferralActivity.class);
                activity.startActivity(intent);
                dialog.dismiss();
            }
        });
        (view.findViewById(R.id.cross_banner)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                activity.reportActionToAnalytics("referralbannerdismiss");
            }
        });
        dialog.show();
    }

    
}
