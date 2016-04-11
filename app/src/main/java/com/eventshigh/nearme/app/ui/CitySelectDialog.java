package com.eventshigh.nearme.app.ui;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.user.Account;

/**
 * Created by umesh on 09/04/16.
 */
public class CitySelectDialog {


    public static void show(final BaseActivity activity, final Account account,
                            final CitySelectionCallback callback){

        final String[] cityNames = City.getValuesAsString();
        int lastCity = -1;
        for(int i=0;i<cityNames.length;i++){
            if(account.getLastCity().name().equalsIgnoreCase(cityNames[i])){
                lastCity = i;
                break;
            }
        }
       new  AlertDialog.Builder(activity).setTitle("Change City").setSingleChoiceItems(cityNames, lastCity,  new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int arg1) {
                City newCity = City.getCity((String) cityNames[arg1]);
                if (newCity != null) {
                    account.setLastCity(newCity);
                    callback.onCityChanged(newCity);
                }

                dialog.dismiss();
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).setCancelable(true).show();


    }

    public interface CitySelectionCallback {
        void onCityChanged(City city);
    }
}
