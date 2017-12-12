package com.eventshigh.nearme.app.utils;

import com.eventshigh.nearme.app.data.HotDealsObject;

import java.util.Comparator;

/**
 * Created by umesh on 13/12/17.
 */

public class DealsComaprator implements Comparator<HotDealsObject> {
    @Override
    public int compare(HotDealsObject dealsObject, HotDealsObject t1) {
        return Integer.compare(dealsObject.getRank(), t1.getRank());
    }
}
