package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.stream.PointsObject;

/**
 * Created by umesh on 16/04/16.
 */
public class PointsCard extends RecyclerView.ViewHolder{

    public final LinearLayout pointCard;
    public final ImageView pointCharacter;
    public final TextView pointName;
    public final TextView pointCount;
    public final TextView pointDesc;
    public final TextView pointActionButton;
    public final TextView pointTextImage;

    public PointsCard(View itemView) {
        super(itemView);
        pointCharacter = (ImageView)itemView.findViewById(R.id.point_character);
        pointName = (TextView)itemView.findViewById(R.id.point_name);
        pointCount = (TextView)itemView.findViewById(R.id.point_count);
        pointCard = (LinearLayout)itemView.findViewById(R.id.point_card);
        pointDesc =(TextView)itemView.findViewById(R.id.point_desc);
        pointActionButton = (TextView)itemView.findViewById(R.id.point_action_btn);
        pointTextImage = (TextView)itemView.findViewById(R.id.point_text_img);

    }

    public static PointsCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_point, parent, false);
        return new PointsCard(view);
    }


    public void bindView(final PointsObject obj, final BaseContextActivity activity){
        int resourceId = -1;
        if(obj.pName.equalsIgnoreCase("Refer & Earn")){
            pointCharacter.setVisibility(View.VISIBLE);
            resourceId = R.drawable.ic_refer_action;
            pointTextImage.setVisibility(View.GONE);
        }else if(obj.pName.equalsIgnoreCase("Share an event")){
            pointCharacter.setVisibility(View.VISIBLE);
            resourceId = R.drawable.ic_share_action;
            pointTextImage.setVisibility(View.GONE);
        }else if(obj.pName.equalsIgnoreCase("Refer & Earn")){
            pointCharacter.setVisibility(View.VISIBLE);
            resourceId = R.drawable.ic_refer_action;
            pointTextImage.setVisibility(View.GONE);
        }else if(obj.pName.equalsIgnoreCase("Book a ticket")){
            pointCharacter.setVisibility(View.VISIBLE);
            resourceId = R.drawable.ic_book_ticket_action;
            pointTextImage.setVisibility(View.GONE);
        }else if(obj.pName.equalsIgnoreCase("Favorite an event")){
            pointCharacter.setVisibility(View.VISIBLE);
            resourceId = R.drawable.ic_fav_action;
            pointTextImage.setVisibility(View.GONE);
        }else{
            pointCharacter.setVisibility(View.INVISIBLE);
            pointTextImage.setText(obj.pName.charAt(0));
        }
        if(resourceId != -1)
        Glide.with(activity).load(resourceId)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_launcher).crossFade()
                .into(pointCharacter);

        pointName.setText(obj.pName);
        pointCount.setText(obj.points + " points");
        pointDesc.setVisibility(View.VISIBLE);
        pointDesc.setText(obj.pDesc);
        if(obj.pName.equalsIgnoreCase("Refer & Earn")){
            pointActionButton.setVisibility(View.VISIBLE);
            pointActionButton.setText("Refer Friend Now!");
            pointActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                 //   Intent intent = new Intent(activity, ReferralActivity.class);
                  //  activity.startActivity(intent);
                }
            });

        }else{
            pointActionButton.setVisibility(View.GONE);
        }
        /*pointCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pointDesc.isShown()){
                    pointDesc.setVisibility(View.GONE);
                    pointActionButton.setVisibility(View.GONE);
                }else{
                    pointDesc.setVisibility(View.VISIBLE);
                    pointDesc.setText(obj.pDesc);
                    if(obj.pName.equalsIgnoreCase("Refer & Earn")){
                        pointActionButton.setVisibility(View.VISIBLE);
                        pointActionButton.setText("Refer Friend Now!");
                        pointActionButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(activity, ReferralActivity.class);
                                activity.startActivity(intent);
                            }
                        });

                    }else{
                        pointActionButton.setVisibility(View.GONE);
                    }

                }
            }
        });*/



    }


}
