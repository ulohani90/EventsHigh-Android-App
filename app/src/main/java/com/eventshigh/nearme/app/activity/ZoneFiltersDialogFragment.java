package com.eventshigh.nearme.app.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ExpandableListView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Locality;
import com.eventshigh.nearme.app.data.stream.ZoneLocalityMapObject;
import com.eventshigh.nearme.app.ui.adapter.ZonesExpandableListAdapter;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by umesh on 22/11/17.
 */

public class ZoneFiltersDialogFragment extends DialogFragment {

    public static ZoneFiltersDialogFragment newInstance(Bundle bundle) {
        ZoneFiltersDialogFragment fragment = new ZoneFiltersDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    ArrayList<ZoneLocalityMapObject> zoneLocalityMap;

    ArrayList<String> selectedLocalities;

    OnAcceptClickListener mListener;

    int previousGroup = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        zoneLocalityMap = getArguments().getParcelableArrayList("zones");
        selectedLocalities = getArguments().getStringArrayList("selected_localities");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.zone_filter_dialog_layout, container, false);
        final ExpandableListView zonesList = (ExpandableListView) view.findViewById(R.id.zones_list);
        final ZonesExpandableListAdapter adapter = new ZonesExpandableListAdapter(getActivity(), zoneLocalityMap, selectedLocalities);
        zonesList.setGroupIndicator(null);
        zonesList.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                if ((previousGroup != -1) && (groupPosition != previousGroup)) {
                    zonesList.collapseGroup(previousGroup);

                }
                adapter.setSelectedGroup(groupPosition);
                previousGroup = groupPosition;
            }
        });

        zonesList.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
                if (groupPosition == previousGroup) {
                    adapter.setSelectedGroup(-1);
                }
                previousGroup = -1;
            }
        });

        zonesList.setAdapter(adapter);
        view.findViewById(R.id.accept_dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedLocalities = adapter.getSelectedLocalities();
                if (mListener != null) {
                    mListener.onAcceptClick(selectedLocalities);
                }
                dismiss();
            }
        });

        view.findViewById(R.id.cancel_dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        return view;
    }

    public void setOnAcceptClickListener(OnAcceptClickListener mListener) {
        this.mListener = mListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                getActivity().onBackPressed();
            }
        });
        return dialog;
    }

    public interface OnAcceptClickListener {
        void onAcceptClick(ArrayList<String> selectedLocalities);
    }
}
