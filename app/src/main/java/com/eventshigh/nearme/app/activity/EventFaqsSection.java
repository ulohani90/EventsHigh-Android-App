package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventZendeskTicketObject;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.utils.CustomLayoutManager;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.ArrayList;

/**
 * Created by umesh on 15/03/17.
 */

public class EventFaqsSection extends BaseEventsFragment {


    AutofitRecyclerView faqList;

    ArrayList<EventZendeskTicketObject> faqs;

    public static EventFaqsSection newInstance(Bundle bundle) {
        EventFaqsSection fragment = new EventFaqsSection();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        faqs = getArguments().getParcelableArrayList("faqs");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(activity).inflate(R.layout.event_faq_list_fragment, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        faqList = (AutofitRecyclerView) view.findViewById(R.id.faq_list);
        CustomLayoutManager layoutManager = new CustomLayoutManager(getActivity());
        faqList.setLayoutManager(layoutManager);
        layoutManager.setScrollEnabled(false);
        EventsAdapter adapter = new EventsAdapter(activity);
        faqList.setAdapter(adapter);
        adapter.setEventFaqs(faqs);

    }
}
