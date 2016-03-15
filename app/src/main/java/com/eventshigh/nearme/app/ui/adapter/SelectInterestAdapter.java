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

import com.eventshigh.nearme.app.R;
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
        holder.expandArrow.setVisibility(View.VISIBLE);
        if(selectedGroup == groupPosition){
            holder.expandArrow.setImageResource(R.drawable.ic_keyboard_arrow_up_black_24dp);
        }else{
            holder.expandArrow.setImageResource(R.drawable.ic_keyboard_arrow_down_black_24dp);
        }
        holder.subcategoryName.setVisibility(View.GONE);
        holder.categoryName.setVisibility(View.VISIBLE);
        holder.categoryName.setText(categories[groupPosition].categoryName);
        holder.categoryName.setTypeface(null, Typeface.BOLD);
        holder.followView.setImageResource((account.isFollowing(categories[groupPosition].categoryName)|| checkWhetherAllSelected(categories[groupPosition]))?R.drawable.ic_check_circle_green_24dp:R.drawable.ic_add_circle_outline_black_24dp);
        holder.followView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(account.isFollowing(categories[groupPosition].categoryName)){
                    account.setIsFollowing(categories[groupPosition].categoryName,false);
                    holder.followView.setImageResource(R.drawable.ic_add_circle_outline_black_24dp);
                }else{
                    account.setIsFollowing(categories[groupPosition].categoryName,true);
                    holder.followView.setImageResource(R.drawable.ic_check_circle_green_24dp);
                }
                notifyDataSetChanged();
            }
        });
        return convertView;
    }

    @Override
    public View getChildView(final int groupPosition,final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.select_interest_item_layout,parent,false);
            ViewHolder holder = new ViewHolder(convertView);
            convertView.setTag(holder);

        }
       final ViewHolder holder = new ViewHolder(convertView);
        holder.expandArrow.setVisibility(View.INVISIBLE);
        if(account.isFollowing(categories[groupPosition].categoryName) ||
                account.isFollowing(subcategories.get(categories[groupPosition]).get(childPosition).name)){
            holder.followView.setImageResource(R.drawable.ic_check_circle_green_24dp);
        }else {
            holder.followView.setImageResource(R.drawable.ic_add_circle_outline_gray_24dp);
        }
        holder.categoryName.setVisibility(View.GONE);
        holder.subcategoryName.setVisibility(View.VISIBLE);
        holder.subcategoryName.setText(subcategories.get(categories[groupPosition]).get(childPosition).name);
        holder.parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               /* if(account.isFollowing(categories[groupPosition].categoryName)){
                    account.setIsFollowing(categories[groupPosition].categoryName,false);
                    holder.followView.setImageResource(R.drawable.ic_add_circle_outline_gray_24dp);
                }else*/
                if(account.isFollowing(subcategories.get(categories[groupPosition]).get(childPosition).name)){
                    account.setIsFollowing(subcategories.get(categories[groupPosition]).get(childPosition).name,false);
                    holder.followView.setImageResource(R.drawable.ic_add_circle_outline_gray_24dp);
                }else{
                    account.setIsFollowing(subcategories.get(categories[groupPosition]).get(childPosition).name,true);
                    holder.followView.setImageResource(R.drawable.ic_check_circle_green_24dp);
                }
                account.setIsFollowing(categories[groupPosition].categoryName, checkWhetherAllSelected(categories[groupPosition]));
                notifyDataSetChanged();

            }
        });

        return convertView;
    }

    public boolean checkWhetherAllSelected(EventCategory categoryName){
        List<EventSubcategory> subCategories = subcategories.get(categoryName);
        for(EventSubcategory subCategory : subCategories){
            if(!account.isFollowing(subCategory.name))
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
        private ImageView followView,expandArrow ;
        private LinearLayout parent;
        private TextView subcategoryName;

        public ViewHolder(View view){
            categoryName = (TextView)view.findViewById(R.id.category_name);
            followView = (ImageView)view.findViewById(R.id.follow_icon);
            parent = (LinearLayout)view.findViewById(R.id.parent);
            subcategoryName = (TextView)view.findViewById(R.id.subcategory_name);
            expandArrow = (ImageView)view.findViewById(R.id.expand_arrow);
        }

    }


}
