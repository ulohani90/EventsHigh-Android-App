package com.eventshigh.nearme.app.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.SelectInterestsActivity;
import com.eventshigh.nearme.app.data.stream.ZoneLocalityMapObject;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by umesh on 22/11/17.
 */

public class ZonesExpandableListAdapter extends BaseExpandableListAdapter {

    Activity activity;
    ArrayList<ZoneLocalityMapObject> zones;

    ArrayList<String> selectedLocalities;

    int selectedGroup = -1;


    public ZonesExpandableListAdapter(Activity activity, ArrayList<ZoneLocalityMapObject> zones, ArrayList<String> selectedLocalities) {
        this.activity = activity;
        this.zones = zones;
        if (selectedLocalities != null) {
            this.selectedLocalities = selectedLocalities;
        } else {
            this.selectedLocalities = new ArrayList<>();
        }
    }

    public ArrayList<String> getSelectedLocalities() {
        return selectedLocalities;
    }

    public void setSelectedGroup(int selectedGroup) {
        this.selectedGroup = selectedGroup;
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        if (zones != null) {
            return zones.size();
        }
        return 0;
    }

    @Override
    public int getChildrenCount(int i) {
        return zones.get(i).getLocalities().size();
    }

    @Override
    public Object getGroup(int i) {
        return zones.get(i);
    }

    @Override
    public Object getChild(int groupIndex, int childIndex) {
        return zones.get(groupIndex).getLocalities().get(childIndex);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return groupPosition + childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.select_interest_item_layout, parent, false);
            ZonesExpandableListAdapter.ViewHolder holder = new ZonesExpandableListAdapter.ViewHolder(convertView);
            convertView.setTag(holder);

        }
        final String zoneName = zones.get(groupPosition).getZone();
        final ZonesExpandableListAdapter.ViewHolder holder = new ZonesExpandableListAdapter.ViewHolder(convertView);
        holder.categoryImage.setVisibility(View.GONE);

        holder.expandArrow.setVisibility(View.VISIBLE);
        if (zones.get(groupPosition).getLocalities() != null && zones.get(groupPosition).getLocalities().size() > 0) {
            holder.expandArrow.setVisibility(View.VISIBLE);
            if (selectedGroup == groupPosition) {
                holder.expandArrow.setImageResource(R.drawable.ic_keyboard_arrow_up_black_24dp);
            } else {
                holder.expandArrow.setImageResource(R.drawable.ic_keyboard_arrow_down_black_24dp);
            }
        } else {
            holder.expandArrow.setVisibility(View.GONE);
        }
        holder.categoryName.setVisibility(View.VISIBLE);
        holder.categoryName.setText(Utils.capitalize(zoneName));
        holder.categoryName.setTypeface(null, Typeface.BOLD);
        holder.subcategoryName.setVisibility(View.GONE);

        if (selectedLocalities != null && (selectedLocalities.contains(zoneName) || checkIfAllChildrenPresent(zones.get(groupPosition).getLocalities()))) {
            holder.followView.setImageResource(R.drawable.ic_check_circle_green_24dp);
        } else {
            holder.followView.setImageResource(R.drawable.ic_add_circle_outline_gray_24dp);
        }

        holder.followView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedLocalities == null) {
                    selectedLocalities = new ArrayList<>();
                }
                if (selectedLocalities.contains(zoneName) || checkIfAllChildrenPresent(zones.get(groupPosition).getLocalities())) {
                    selectedLocalities.remove(zoneName);
                    removeChildsIfPresent(zones.get(groupPosition).getLocalities());
                    holder.followView.setImageResource(R.drawable.ic_add_circle_outline_gray_24dp);
                } else {
                    if (!selectedLocalities.contains(zoneName))
                        selectedLocalities.add(zoneName);
                    holder.followView.setImageResource(R.drawable.ic_check_circle_green_24dp);
                }
                notifyDataSetChanged();
            }
        });

        return convertView;
    }

    public void removeChildsIfPresent(ArrayList<String> localities) {
        for (String locality : localities) {
            if (selectedLocalities.contains(locality)) {
                selectedLocalities.remove(locality);
            }
        }

    }

    public boolean checkIfAllChildrenPresent(ArrayList<String> localities) {
        if (localities.size() > 0) {
            for (String locality : localities) {
                if (!selectedLocalities.contains(locality)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.select_interest_child_layout, parent, false);
            ZonesExpandableListAdapter.ViewHolder holder = new ZonesExpandableListAdapter.ViewHolder(convertView);
            convertView.setTag(holder);

        }
        final String localityName = zones.get(groupPosition).getLocalities().get(childPosition);
        final ZonesExpandableListAdapter.ViewHolder holder = new ZonesExpandableListAdapter.ViewHolder(convertView);
        holder.expandArrow.setVisibility(View.GONE);
        holder.categoryImage.setVisibility(View.GONE);
        holder.categoryName.setVisibility(View.GONE);
        holder.subcategoryName.setVisibility(View.VISIBLE);
        holder.subcategoryName.setText(Utils.capitalize(localityName));

        holder.subcategoryName.setPadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, activity.getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, activity.getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, activity.getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, activity.getResources().getDisplayMetrics()));

        if (selectedLocalities != null && (selectedLocalities.contains(localityName) || selectedLocalities.contains(zones.get(groupPosition).getZone()))) {
            holder.followView.setImageResource(R.drawable.ic_check_circle_green_24dp);
        } else {
            holder.followView.setImageResource(R.drawable.ic_add_circle_outline_gray_24dp);
        }

        holder.parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedLocalities == null) {
                    selectedLocalities = new ArrayList<>();
                }
                if (selectedLocalities.contains(localityName) || selectedLocalities.contains(zones.get(groupPosition).getZone())) {
                    if (selectedLocalities.contains(zones.get(groupPosition).getZone())) {
                        addAllLocalitiesToSelectedLocalities(zones.get(groupPosition).getLocalities());
                        selectedLocalities.remove(zones.get(groupPosition).getZone());
                    }
                    selectedLocalities.remove(localityName);

                    holder.followView.setImageResource(R.drawable.ic_add_circle_outline_gray_24dp);
                } else {
                   /* if (!selectedLocalities.contains(localityName))
                        selectedLocalities.add(localityName);*/
                    addLocalityToSelectedMap(zones.get(groupPosition).getZone(), localityName, zones.get(groupPosition).getLocalities());
                    holder.followView.setImageResource(R.drawable.ic_check_circle_green_24dp);
                }
                notifyDataSetChanged();
            }
        });

        return convertView;
    }

    public void addAllLocalitiesToSelectedLocalities(ArrayList<String> localities) {
        for (String locality : localities) {
            if (!selectedLocalities.contains(locality))
                selectedLocalities.add(locality);
        }
    }

    public void addLocalityToSelectedMap(String zoneName, String localityName, ArrayList<String> localities) {
        boolean areAllValuesFound = true;
        for (String locality : localities) {
            if (!selectedLocalities.contains(locality)) {
                areAllValuesFound = false;
                break;
            }
        }
        if (areAllValuesFound) {
            if (!selectedLocalities.contains(zoneName))
                selectedLocalities.add(zoneName);
            removeChildsIfPresent(localities);
        } else {
            if (!selectedLocalities.contains(localityName))
                selectedLocalities.add(localityName);
        }
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    public class ViewHolder {
        private TextView categoryName;
        private ImageView followView, expandArrow, categoryImage;
        private LinearLayout parent;
        private TextView subcategoryName;

        public ViewHolder(View view) {
            categoryName = (TextView) view.findViewById(R.id.category_name);
            followView = (ImageView) view.findViewById(R.id.follow_icon);
            parent = (LinearLayout) view.findViewById(R.id.parent);
            subcategoryName = (TextView) view.findViewById(R.id.subcategory_name);
            expandArrow = (ImageView) view.findViewById(R.id.expand_arrow);
            categoryImage = (ImageView) view.findViewById(R.id.cat_image);
        }

    }


    public interface OnFollowUnfollowOptionClick {
        void onInterestClick(String tag, boolean isAdding);
    }
}
