package com.eventshigh.nearme.app.data.stream;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by umesh on 16/04/16.
 */
public class PointsObject {

    public final String pName;

    public final int points;

    public final String pDesc;

    public  PointsObject(String pName, int points,String pDesc){
        this.pName = pName;
        this.points = points;
        this.pDesc = pDesc;
    }

    public static PointsObject parse(JSONObject obj) throws JSONException{
        String pName = obj.getString("pname");
        int  points = obj.getInt("points");
        String pDesc = obj.getString("pdesc");
        return new PointsObject(pName,points,pDesc);
    }

}
