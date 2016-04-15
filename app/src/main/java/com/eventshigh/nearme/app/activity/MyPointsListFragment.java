package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * Created by umesh on 15/04/16.
 */
public class MyPointsListFragment  extends Fragment {

    public static MyPointsListFragment newInstance(Bundle args) {
        MyPointsListFragment fragment = new MyPointsListFragment();
        fragment.setArguments(args);
        return fragment;
    }

}
