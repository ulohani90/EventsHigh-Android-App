package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GuestDetailActivity extends AppCompatActivity implements View.OnClickListener{

    private static int no_of_guest = 5;
    List<LinearLayout> listGuestDetailLayout;

    String[] tsize = {"S","M","L","XL","XXL"};
    int [] ntsize;

    LinearLayout ll_guest_detail_layout_container;
    Button btnNextGuestDetails;
    ScrollView svDetailCards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        svDetailCards = (ScrollView)findViewById(R.id.sv_guest_detail_cards);
        ll_guest_detail_layout_container = (LinearLayout)findViewById(R.id.ll_guest_detail_layout_container);
        listGuestDetailLayout = new ArrayList<LinearLayout>();
        btnNextGuestDetails = (Button)findViewById(R.id.btn_guest_detail_next);
        ntsize = new int[no_of_guest];

        for(int i=0;i<no_of_guest;i++){
            LinearLayout llGuestLayout = (LinearLayout)getLayoutInflater().inflate(R.layout.card_guest_detail,null);
            i++;((TextView)llGuestLayout.findViewById(R.id.tv_guest_no)).setText("Guest "+i);i--;
            final TextView tvTsize = (TextView)llGuestLayout.findViewById(R.id.tv_tsize);
            ImageButton ibLeftArrow = (ImageButton)llGuestLayout.findViewById(R.id.ib_left_arrow_tsize);
            ImageButton ibRightArrow = (ImageButton)llGuestLayout.findViewById(R.id.ib_right_arrow_tsize);
            tvTsize.setText(tsize[0]);
            ntsize[i]=0;
            ibLeftArrow.setTag(i + "");
            ibLeftArrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int i = Integer.parseInt(v.getTag().toString());
                    ntsize[i] = (ntsize[i]+4)%5;
                    tvTsize.setText(tsize[ntsize[i]]);
                }
            });
            ibRightArrow.setTag(i + "");
                ibRightArrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int i = Integer.parseInt(v.getTag().toString());
                    ntsize[i] = (ntsize[i]+1)%5;
                    tvTsize.setText(tsize[ntsize[i]]);
                }
            });
            ll_guest_detail_layout_container.addView(llGuestLayout);
            listGuestDetailLayout.add(llGuestLayout);
        }

        btnNextGuestDetails.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btn_guest_detail_next:
                JSONArray jsonArray = new JSONArray();
                boolean is_details_complete = true;
                for(LinearLayout ll_guest_detail:listGuestDetailLayout){
                    JSONObject jsonObject = new JSONObject();
                    try {
                        if(Utils.checkIfStringEmpty(((TextView) ll_guest_detail.findViewById(R.id.et_guest_name)).getText().toString())){
                            jsonObject.put("name", ((TextView) ll_guest_detail.findViewById(R.id.et_guest_name)).getText().toString());
                        }else{
                            is_details_complete = false;
                            focusOnView(ll_guest_detail.findViewById(R.id.et_guest_name));
                            break;
                        }
                        if(Utils.checkIfStringEmpty(((TextView) ll_guest_detail.findViewById(R.id.et_guest_email)).getText().toString())){
                            jsonObject.put("email", ((TextView) ll_guest_detail.findViewById(R.id.et_guest_email)).getText().toString());
                        }else{
                            is_details_complete = false;
                            focusOnView(ll_guest_detail.findViewById(R.id.et_guest_email));
                            break;
                        }
                        if(Utils.checkIfStringEmpty(((TextView) ll_guest_detail.findViewById(R.id.et_guest_phone)).getText().toString())){
                            jsonObject.put("phone", ((TextView) ll_guest_detail.findViewById(R.id.et_guest_phone)).getText().toString());
                        }else{
                            is_details_complete = false;
                            focusOnView(ll_guest_detail.findViewById(R.id.et_guest_phone));
                            break;
                        }
                    }catch (JSONException e){
                        is_details_complete = false;
                    }
                    jsonArray.put(jsonObject);
                }

                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final void focusOnView(final View view) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                int vTop = view.getTop();
                int vBottom = view.getBottom();
                int sHeight = svDetailCards.getHeight();
                svDetailCards.smoothScrollTo(((vTop + vBottom - sHeight) / 2), 0);
            }
        });
    }


}
