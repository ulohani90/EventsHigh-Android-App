package com.eventshigh.nearme.app.ui.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.SelectInterestsActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.stream.EventSubcategory;
import com.eventshigh.nearme.app.user.Account;

import java.util.HashMap;
import java.util.List;

/**
 * Created by umesh on 15/03/16.
 */
public class SelectInterestAdapter extends BaseExpandableListAdapter{


    EventCategory[] categories;

    HashMap<EventCategory , List<EventSubcategory> > subcategories;

    Context mContext;

    int selectedGroup = -1;

    Account account;


    public SelectInterestAdapter(Context context , EventCategory[] categories,HashMap<EventCategory , List<EventSubcategory> > subcategories){
            this.mContext  =context;
            this.account = new Account(context);
            this.categories = categories;
            this.subcategories = subcategories;
    }

    @Override
    public int getGroupCount() {
        return categories.length;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return subcategories.get(categories[groupPosition]).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return subcategories.get(categories[groupPosition]);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return subcategories.get(categories[groupPosition]).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }


    public void setSelectedGroup(int selectedGroup) {
        this.selectedGroup = selectedGroup;
        notifyDataSetChanged();
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.select_interest_item_layout,parent,false);
            ViewHolder holder = new ViewHolder(convertView);
            convertView.setTag(holder);

        }

        final ViewHolder holder = new ViewHolder(convertView);
        Glide.with(mContext).load(categories[groupPosition].getInterestIconResourceId())
                .crossFade().centerCrop()
                .into(holder.categoryImage);
        holder.categoryImage.setBackgroundResource(getBackgroundResource(categories[groupPosition]));
        holder.expandArrow.setVisibility(View.VISIBLE);
        if(subcategories.get(categories[groupPosition]).size()>0) {
            holder.expandArrow.setVisibility(View.VISIBLE);
            if (selectedGroup == groupPosition) {
                holder.expandArrow.setImageResource(R.drawable.ic_keyboard_arrow_up_black_24dp);
            } else {
                holder.expandArrow.setImageResource(R.drawable.ic_keyboard_arrow_down_black_24dp);
            }
        }else{
            holder.expandArrow.setVisibility(View.GONE);
        }
        holder.subcategoryName.setVisibility(View.GONE);
        holder.categoryName.setVisibility(View.VISIBLE);
        holder.categoryName.setText(categories[groupPosition].categoryName);
        holder.categoryName.setTypeface(null, Typeface.BOLD);
        if(account.isFollowing(categories[groupPosition].categoryName)){
            holder.followView.setVisibility(View.VISIBLE);
            holder.followView.setImageResource(R.drawable.ic_check_circle_green_24dp);
        }else{
            if(subcategories.get(categories[groupPosition]).size()==0){
                holder.followView.setVisibility(View.VISIBLE);
                holder.followView.setImageResource(R.drawable.ic_add_circle_outline_gray_24dp);
            }else{
                holder.followView.setVisibility(View.GONE);
            }

        }

        holder.followView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (account.isFollowing(categories[groupPosition].categoryName) && subcategories.get(categories[groupPosition]).size() == 0) {
                    account.setIsFollowing(categories[groupPosition].categoryName, false);
                    holder.followView.setImageResource(R.drawable.ic_add_circle_outline_black_24dp);
                    if(((SelectInterestsActivity)mContext).isFromNotification()){
                        ((SelectInterestsActivity) mContext).reportActionToAnalytics("removeFollowingPNotif", categories[groupPosition].categoryName);
                    }else {
                        ((SelectInterestsActivity) mContext).reportActionToAnalytics("removeFollowingP", categories[groupPosition].categoryName);
                    }
                    ((SelectInterestsActivity)mContext).showMessage(categories[groupPosition].categoryName+" Unfollowed");

                } else {
                    account.setIsFollowing(categories[groupPosition].categoryName, true);
                    holder.followView.setImageResource(R.drawable.ic_check_circle_green_24dp);
                    if(((SelectInterestsActivity)mContext).isFromNotification()){
                        ((SelectInterestsActivity) mContext).reportActionToAnalytics("addFollowingPNotif", categories[groupPosition].categoryName);
                    }else {
                        ((SelectInterestsActivity) mContext).reportActionToAnalytics("addFollowingP", categories[groupPosition].categoryName);
                    }
                    ((SelectInterestsActivity)mContext).showMessage("You are now following " + categories[groupPosition].categoryName);
                }
                notifyDataSetChanged();
            }
        });
       // holder.followView.setImageResource((account.isFollowing(categories[groupPosition].categoryName) || checkWhetherAllSelected(categories[groupPosition])) ? R.drawable.ic_check_circle_green_24dp : R.drawable.ic_add_circle_outline_black_24dp);
        return convertView;
    }

    public int getBackgroundResource(EventCategory category) {
        if (category == EventCategory.NIGHTLIFE){
            return R.drawable.green_circle_bg;
        }else if(category == EventCategory.OUTDOORS){
            return R.drawable.golden_circle_bg;
        }else if(category == EventCategory.WORKSHOP){
            return R.drawable.violet_circle_bg;
        }else if(category == EventCategory.LIVE_PERFORMANCES){
            return R.drawable.pink_circle_bg;
        }else if(category == EventCategory.SPORTS){
            return R.drawable.blue_circle_bg;
        }else if(category == EventCategory.FOOD){
            return R.drawable.orange_cirlce_bg;
        }else if(category == EventCategory.HEALTH_WELLNESS){
            return R.drawable.yellow_circle_bg;
        }else if(category == EventCategory.LITERATURE){
            return R.drawable.purple_circle_bg;
        }else if(category == EventCategory.KIDS_ENTERTAINMENT){
            return R.drawable.red_circle_bg;
        }else if(category == EventCategory.EDITOR_PICKS){
            return R.drawable.grey_circle_bg;
        }else if(category == EventCategory.FREE_EVENTS){
            return R.drawable.slate_circle_bg;
        }else{
            return R.drawable.light_blue_circle_bg;
        }

    }

    @Override
    public View getChildView(final int groupPosition,final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.select_interest_child_layout,parent,false);
            ViewHolder holder = new ViewHolder(convertView);
            convertView.setTag(holder);

        }
       final ViewHolder holder = new ViewHolder(convertView);
        holder.expandArrow.setVisibility(View.GONE);
        holder.categoryImage.setVisibility(View.GONE);
        if((childPosition == 0 && account.isFollowing(categories[groupPosition].categoryName) )||
                account.isFollowing(subcategories.get(categories[groupPosition]).get(childPosition).name)){
            holder.followView.setImageResource(R.drawable.ic_check_circle_green_24dp);
        }else {
            holder.followView.setImageResource(R.drawable.ic_add_circle_outline_gray_24dp);
        }
        holder.categoryName.setVisibility(View.GONE);
        if(childPosition == 0){
            holder.subcategoryName.setTypeface(null,Typeface.BOLD);
        }else{
            holder.subcategoryName.setTypeface(null,Typeface.NORMAL);
        }
        holder.subcategoryName.setVisibility(View.VISIBLE);
        holder.subcategoryName.setText(subcategories.get(categories[groupPosition]).get(childPosition).name);
        holder.parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               /* if(account.isFollowing(categories[groupPosition].categoryName)){
                    account.setIsFollowing(categories[groupPosition].categoryName,false);
                    holder.followView.setImageResource(R.drawable.ic_add_circle_outline_gray_24dp);
                }else*/
                if (childPosition == 0) {
                    if (account.isFollowing(categories[groupPosition].categoryName)) {
                        account.setIsFollowing(categories[groupPosition].categoryName, false);
                        holder.followView.setImageResource(R.drawable.ic_add_circle_outline_gray_24dp);
                        if(((SelectInterestsActivity)mContext).isFromNotification()){
                            ((SelectInterestsActivity) mContext).reportActionToAnalytics("removeFollowingPNotif", categories[groupPosition].categoryName);
                        }else {
                            ((SelectInterestsActivity) mContext).reportActionToAnalytics("removeFollowingP", categories[groupPosition].categoryName);
                        }
                        ((SelectInterestsActivity)mContext).showMessage(categories[groupPosition].categoryName+" Unfollowed");
                    } else {
                        if(((SelectInterestsActivity)mContext).isFromNotification()){
                            ((SelectInterestsActivity) mContext).reportActionToAnalytics("addFollowingPNotif", categories[groupPosition].categoryName);
                        }else {

                            ((SelectInterestsActivity) mContext).reportActionToAnalytics("addFollowingP", categories[groupPosition].categoryName);
                        }
                        account.setIsFollowing(categories[groupPosition].categoryName, true);
                        holder.followView.setImageResource(R.drawable.ic_check_circle_green_24dp);
                        ((SelectInterestsActivity)mContext).showMessage("You are now following " + categories[groupPosition].categoryName);
                    }
                } else if (account.isFollowing(subcategories.get(categories[groupPosition]).get(childPosition).name)) {
                    account.setIsFollowing(subcategories.get(categories[groupPosition]).get(childPosition).name, false);
                    holder.followView.setImageResource(R.drawable.ic_add_circle_outline_gray_24dp);
                    if(((SelectInterestsActivity)mContext).isFromNotification()){
                        ((SelectInterestsActivity) mContext).reportActionToAnalytics("removeFollowingNotif", subcategories.get(categories[groupPosition]).get(childPosition).name);
                    }else {
                        ((SelectInterestsActivity) mContext).reportActionToAnalytics("removeFollowingP", subcategories.get(categories[groupPosition]).get(childPosition).name);
                    }

                    ((SelectInterestsActivity)mContext).showMessage(subcategories.get(categories[groupPosition]).get(childPosition).name + " Unfollowed");
                } else {
                    if(((SelectInterestsActivity)mContext).isFromNotification()){
                        ((SelectInterestsActivity) mContext).reportActionToAnalytics("addFollowingPNotif", subcategories.get(categories[groupPosition]).get(childPosition).name);
                    }else {
                        ((SelectInterestsActivity) mContext).reportActionToAnalytics("addFollowingP", subcategories.get(categories[groupPosition]).get(childPosition).name);
                    }

                    account.setIsFollowing(subcategories.get(categories[groupPosition]).get(childPosition).name, true);
                    holder.followView.setImageResource(R.drawable.ic_check_circle_green_24dp);
                    ((SelectInterestsActivity)mContext).showMessage("You are now following " + subcategories.get(categories[groupPosition]).get(childPosition).name);
                }
                // account.setIsFollowing(categories[groupPosition].categoryName, checkWhetherAllSelected(categories[groupPosition]));
                notifyDataSetChanged();

            }
        });

        return convertView;
    }

    public boolean checkWhetherAllSelected(EventCategory categoryName){
        List<EventSubcategory> subCategories = subcategories.get(categoryName);
        for(int i=1;i<subCategories.size();i++){
            if(!account.isFollowing(subCategories.get(i).name))
                return false;
        }
        if(subCategories.size() == 0){
            return false;
        }else {
            return true;
        }
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public class ViewHolder{
        private TextView categoryName;
        private ImageView followView,expandArrow,categoryImage;
        private LinearLayout parent;
        private TextView subcategoryName;

        public ViewHolder(View view){
            categoryName = (TextView)view.findViewById(R.id.category_name);
            followView = (ImageView)view.findViewById(R.id.follow_icon);
            parent = (LinearLayout)view.findViewById(R.id.parent);
            subcategoryName = (TextView)view.findViewById(R.id.subcategory_name);
            expandArrow = (ImageView)view.findViewById(R.id.expand_arrow);
            categoryImage = (ImageView)view.findViewById(R.id.cat_image);
        }

    }


}
