package com.eventshigh.nearme.app.ui.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.ArrayList;


/**
 * Created by umesh on 12/07/16.
 */
public class SearchInterestResultAdapter extends BaseAdapter implements Filterable {

    Context mContext;
    Account mAccount;

    public SearchInterestResultAdapter(Context context, Account account, ArrayList<String> allTags) {
        this.mContext = context;
        this.mAccount = account;
        this.tags = allTags;
        filteredTags = new ArrayList<>();
    }

    OnTagFollowedListener mListener;

    ArrayList<String> tags;
    ArrayList<String> filteredTags;

    public void setOnTagFollowedListener(OnTagFollowedListener listener) {
        this.mListener = listener;
    }

    @Override
    public int getCount() {
        if (filteredTags != null) {
            return filteredTags.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return filteredTags.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_interest_result_adapter_item, parent, false);
            ViewHolder holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        ViewHolder holder = (ViewHolder) convertView.getTag();
        holder.tagName.setText(Utils.capitalize(filteredTags.get(position)));
        addLeftDrawable(holder.tagName, filteredTags.get(position));

        holder.tagName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mAccount.isFollowing(filteredTags.get(position))) {
                    mAccount.setIsFollowing(filteredTags.get(position), true);
                    if (mListener != null) {
                        mListener.onTagFollowed(filteredTags.get(position));
                    }
                    notifyDataSetChanged();
                }
            }
        });
        return convertView;
    }

    public void addLeftDrawable(TextView textView, String tag) {
        Drawable drawableLeft;
        if (mAccount.isFollowing(tag)) {
            drawableLeft = mContext.getResources().getDrawable(R.drawable.ic_check_circle_green_24dp);
        } else {
            drawableLeft = mContext.getResources().getDrawable(R.drawable.ic_add_circle_outline_gray_24dp);
        }
        drawableLeft.setBounds(0, 0, drawableLeft.getIntrinsicWidth(), drawableLeft.getIntrinsicHeight());
        textView.setCompoundDrawables(drawableLeft, null, null, null);
    }

    @Override
    public android.widget.Filter getFilter() {
        return filter;
    }


    public class ViewHolder {
        TextView tagName;

        public ViewHolder(View view) {
            tagName = (TextView) view.findViewById(R.id.tag_name);
        }
    }

    Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint != null) {
                filteredTags.clear();
                for (String tag : tags) {
                    if (tag.toLowerCase().startsWith(constraint.toString().toLowerCase())) {
                        filteredTags.add(tag);
                    }
                }
                filteredTags.add(constraint.toString());
                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredTags;
                filterResults.count = filteredTags.size();
                return filterResults;
            } else {
                return new FilterResults();
            }
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
           /* ArrayList<String> filteredList = (ArrayList<String>) results.values;
            if (results != null && results.count > 0) {
                filteredTags.clear();
                for (String c : filteredList) {
                    filteredTags.add(c);
                }*/
            notifyDataSetChanged();
            //}
        }
    };


    public interface OnTagFollowedListener {
        void onTagFollowed(String tag);
    }
}
