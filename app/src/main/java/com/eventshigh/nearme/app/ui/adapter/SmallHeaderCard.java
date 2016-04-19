package com.eventshigh.nearme.app.ui.adapter;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.LaunchActivity;

public class SmallHeaderCard extends ViewHolder {
    private final TextView titleView;
    private final ImageView editView;

    public static SmallHeaderCard newInstance(Activity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_header_small, parent, false);
        return new SmallHeaderCard(view);
    }

    public SmallHeaderCard(View cardView) {
        super(cardView);
        this.titleView = (TextView) cardView.findViewById(R.id.header);
        this.editView = (ImageView)cardView.findViewById(R.id.edit);
    }



    public void bindHeaderView(BaseContextActivity activity , final SmallHeaderData header,@Nullable final EventsAdapter.OnEditClickListener listener) {
        titleView.setText(header.header);
        if(header.isEditAllowed){
            editView.setVisibility(View.VISIBLE);
            editView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener!=null)
                        listener.onEditcliked();
                }
            });
        }else{
            editView.setVisibility(View.GONE);
        }
        if(header.drawableLeftResourceId!=0) {
            Drawable drawable = activity.getResources().getDrawable(header.drawableLeftResourceId);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            titleView.setCompoundDrawables(drawable, null, null, null);
        }else{
            titleView.setCompoundDrawables(null, null, null, null);
        }

    }
}
