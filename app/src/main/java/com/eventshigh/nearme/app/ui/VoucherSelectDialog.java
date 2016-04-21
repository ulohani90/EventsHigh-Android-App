package com.eventshigh.nearme.app.ui;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.stream.VoucherObject;
import com.eventshigh.nearme.app.user.Account;

import java.util.ArrayList;

/**
 * Created by umesh on 17/04/16.
 */
public class VoucherSelectDialog {

    public static void show(final BaseActivity activity, ArrayList<VoucherObject> vouchers,int lastPosition,
                            final VoucherSelectionCallback callback){




        new  AlertDialog.Builder(activity).setTitle("Select Coupon").setSingleChoiceItems(getVoucherNames(vouchers), lastPosition,  new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int arg1) {
                callback.onCouponSelected(arg1);
                dialog.dismiss();
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).setCancelable(true).show();


    }

    public interface VoucherSelectionCallback {
        void onCouponSelected(int pos);
    }

    public static String[] getVoucherNames(ArrayList<VoucherObject> vouchers){
        String[] names = new String[vouchers.size()];
        for(int i=0;i<vouchers.size();i++){
            names[i] = vouchers.get(i).voucherName;
        }
        return names;
    }
}
