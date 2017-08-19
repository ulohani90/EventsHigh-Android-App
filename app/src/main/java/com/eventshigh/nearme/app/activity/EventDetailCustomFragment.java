package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.utils.Utils;

/**
 * Created by umesh on 20/06/16.
 */
public class EventDetailCustomFragment extends BaseEventsFragment {

    public static EventDetailCustomFragment newInstance(Bundle bundle) {
        EventDetailCustomFragment fragment = new EventDetailCustomFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    WebView descriptionView;

    String description;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.event_detail_custom_layout, container, false);
        descriptionView = (WebView) view.findViewById(R.id.event_description);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        description = getArguments().getString("description");
        CustomUrlActivity.setupWebView(descriptionView, (BaseContextActivity) getActivity(), false);
        descriptionView.loadData(Utils.changedHeaderHtml(description.trim()), "text/html; charset=UTF-8", null);
        // descriptionView.loadData(toHtmlNoFrame(description.trim()), "text/html; charset=UTF-8", null);

    }

    private static String toHtmlNoFrame(String html) {
        return "<body>" + html.replaceAll("<iframe.*/iframe>", "") + "</body>";
    }


}
