package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.TopCropImageView;

/**
 * Created by umesh on 09/11/17.
 */

public class EditorPicksItemCard extends RecyclerView.ViewHolder {


    ImageView eventImg;
    TextView eventTitle;
    LinearLayout cardParent;
    TextView eventVenue;

    public static EditorPicksItemCard newInstance(BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.editor_picks_card_layout, parent, false);
        return new EditorPicksItemCard(view);
    }

    public EditorPicksItemCard(View itemView) {
        super(itemView);
        eventImg = (ImageView) itemView.findViewById(R.id.event_img);
        eventTitle = (TextView) itemView.findViewById(R.id.event_title);
        cardParent = (LinearLayout) itemView.findViewById(R.id.item_card_parent);
        eventVenue = (TextView) itemView.findViewById(R.id.event_venue);

    }

    public void populate(final BaseContextActivity activity, final Event event, int width) {
        Glide.with(activity).load(event.imgUrl).diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(R.drawable.eh_default_event)
                .crossFade().centerCrop().into(eventImg);
        eventTitle.setText(event.title);
        eventImg.getLayoutParams().height = (6 * width) / 10;
        eventVenue.setText(event.venue);
        cardParent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.showEventDetails(event, null, null);
            }
        });
    }
}
