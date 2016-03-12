package com.eventshigh.nearme.app.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Locality;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by umesh on 09/03/16.
 */
public class LocalitiesAdapter extends BaseAdapter{

    Context mContext;
    List<Locality> localities;
    List<Locality> selectedLocalities;

    OnLocalitySelectedListener mListener;

    public LocalitiesAdapter(Context context,List<Locality> localities,List<Locality> selectedLocalities){
        this.mContext = context;
        this.localities = new ArrayList<>(localities);
        this.selectedLocalities=new ArrayList<>(selectedLocalities);
    }


    @Override
    public int getCount() {
        return localities.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if(convertView == null){
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.checkbox_item_layout,parent,false);
            ViewHolder holder =new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        final ViewHolder holder = (ViewHolder)convertView.getTag();
        holder.checkBox.setText(localities.get(position).name);
        if(selectedLocalities.contains(localities.get(position))){
            holder.checkBox.setChecked(true);
        }else{
            holder.checkBox.setChecked(false);
        }
        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener.onLocalitySelected(localities.get(position), selectedLocalities.contains(localities.get(position)))) {
                    holder.checkBox.setChecked(true);
                    selectedLocalities.add(localities.get(position));
                } else {
                    holder.checkBox.setChecked(false);
                    selectedLocalities.remove(localities.get(position));
                }
            }
        });
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });

        return convertView;
    }

    public void setOnLocalitySelectedListener(OnLocalitySelectedListener listener){
        this.mListener = listener;
    }

    public class ViewHolder{
        CheckBox checkBox;

        public ViewHolder(View view){
            checkBox = (CheckBox)view.findViewById(R.id.checkbox_view);
        }
    }

    public interface OnLocalitySelectedListener{
        boolean onLocalitySelected(Locality locality,boolean isChecked);

    }
}
