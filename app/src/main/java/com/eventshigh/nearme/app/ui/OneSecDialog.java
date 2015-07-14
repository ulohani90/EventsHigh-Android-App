package com.eventshigh.nearme.app.ui;

import android.app.ProgressDialog;
import android.content.Context;

public class OneSecDialog {
    public static ProgressDialog show(Context context) {
        final ProgressDialog dialog = new ProgressDialog(context);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("One sec ...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        return dialog;
    }
}
