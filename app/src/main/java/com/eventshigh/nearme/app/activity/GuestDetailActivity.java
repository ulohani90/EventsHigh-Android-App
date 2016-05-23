package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class GuestDetailActivity extends AppCompatActivity implements View.OnClickListener{

    private static int no_of_guest = 5;
    List<LinearLayout> listGuestDetail;

    String[] tsize = {"S","M","L","XL","XXL"};
    int [] ntsize;

    LinearLayout ll_guest_detail_layout_container;
    Button btnNextGuestDetails;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ll_guest_detail_layout_container = (LinearLayout)findViewById(R.id.ll_guest_detail_layout_container);
        listGuestDetail = new ArrayList<LinearLayout>();
        btnNextGuestDetails = (Button)findViewById(R.id.btn_guest_detail_next);
        ntsize = new int[no_of_guest];

        for(int i=0;i<no_of_guest;i++){
            LinearLayout llGuestLayout = (LinearLayout)getLayoutInflater().inflate(R.layout.guest_detail_layout,null);
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
            listGuestDetail.add(llGuestLayout);
        }

        btnNextGuestDetails.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

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


}
