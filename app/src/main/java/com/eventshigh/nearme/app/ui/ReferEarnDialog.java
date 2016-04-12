package com.eventshigh.nearme.app.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.ReferralActivity;
import com.eventshigh.nearme.app.user.Preferences;

/**
 * Created by umesh on 09/04/16.
 */
public class ReferEarnDialog {

    public static void doNeedFull(BaseActivity activity ){
        Preferences preferences = Preferences.getInstance(activity);
         if ((preferences.getLastTimeReferShown() < System.currentTimeMillis() - (3*DateUtils.DAY_IN_MILLIS)) && preferences.shouldShowReferral()) {
             show(activity);
             preferences.setLastTimeReferShown();
        }
    }

    public static void showDialog(BaseActivity activity ){
            Preferences preferences = Preferences.getInstance(activity);
            preferences.setLastTimeReferShown();
            show(activity);
    }

    public static void show(final BaseActivity activity){

        activity.reportActionToAnalytics("referralbannershown");
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        View view = LayoutInflater.from(activity).inflate(R.layout.home_banner_layout,null);
        ImageView bannerImage = (ImageView)view.findViewById(R.id.banner);
        // Set the background image.
        Glide.with(activity).load(R.drawable.refer)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                .into(bannerImage);
        builder.setView(view);
        final Dialog dialog = builder.create();
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
