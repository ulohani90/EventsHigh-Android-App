package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventSession;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.utils.CustomLayoutManager;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by umesh on 16/09/16.
 */
public class EventSessionDetailFragment extends Fragment {


    ArrayList<EventSession> sessions;

    City city;

    BaseContextActivity activity;

    public static EventSessionDetailFragment newInstance(ArrayList<EventSession> sessions, City city) {

        Bundle args = new Bundle();
        args.putParcelableArrayList("sessions", sessions);
        args.putSerializable("city", city);
        EventSessionDetailFragment fragment = new EventSessionDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.sessions = getArguments().getParcelableArrayList("sessions");
        this.city = (City) getArguments().getSerializable("city");
        activity = (BaseContextActivity) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_session_layout, container, false);
    }

    private LinearLayout sessionDateContainer;
    private AutofitRecyclerView sessionsList;
    private Spinner venueSpinner;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionDateContainer = (LinearLayout) view.findViewById(R.id.session_date_container);
        sessionsList = (AutofitRecyclerView) view.findViewById(R.id.sessions_list);
        CustomLayoutManager layoutManager = new CustomLayoutManager(getActivity());
        sessionsList.setLayoutManager(layoutManager);
        layoutManager.setScrollEnabled(false);
        venueSpinner = (Spinner) view.findViewById(R.id.venue_spinner);
        computeData();
    }

    HashMap<Long, List<String>> dateVenueHashMap;

    HashMap<String, List<EventSession>> venueSessionsMap;

    public void computeData() {
        dateVenueHashMap = new HashMap<>();
        venueSessionsMap = new HashMap<>();
        dates = new ArrayList<>();
        for (EventSession session : sessions) {
            long sessionDate = session.getDate();
            String venueName = session.getVenue();

            if (venueName != null) {

                if (dateVenueHashMap.containsKey(sessionDate)) {
                    if (!(dateVenueHashMap.get(sessionDate).contains(venueName)))
                        dateVenueHashMap.get(sessionDate).add(venueName);
                } else {
                    dates.add(sessionDate);
                    List<String> venueNamesList = new ArrayList<>();
                    venueNamesList.add(venueName);
                    dateVenueHashMap.put(sessionDate, venueNamesList);
                }
                if (venueSessionsMap.containsKey(venueName)) {
                    if (!(venueSessionsMap.get(venueName).contains(session)))
                        venueSessionsMap.get(venueName).add(session);
                } else {
                    List<EventSession> sessionsList = new ArrayList<>();
                    sessionsList.add(session);
                    venueSessionsMap.put(venueName, sessionsList);
                }
            }
        }
        addDatesData();


    }

    List<Long> dates;

    TextView selectedDateView;

    public void addDatesData() {

        sessionDateContainer.removeAllViews();
        if (dateVenueHashMap != null && dateVenueHashMap.size() > 0) {
            for (int i = 0; i < dates.size(); i++) {
                final long dateValue = dates.get(i);
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.session_date_layout, sessionDateContainer, false);
                final TextView date = (TextView) view.findViewById(R.id.date_text);
                date.setText(DateTimeUtils.getSessionDate(dateValue));
                if (i == 0) {
                    selectedDateView = date;
                    date.setSelected(true);
                }
                date.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectedDateView.setSelected(false);
                        date.setSelected(true);
                        selectedDateView = date;
                        activity.reportActionToAnalytics("sessionDateSelected", DateTimeUtils.getDateFromMillisTime(dateValue));
                        addVenueData(dateValue);
                    }
                });
                sessionDateContainer.addView(view);
            }

        }

        if (dates.size() > 0) {
            addVenueData(dates.get(0));
        }
    }

    public void addVenueData(final long key) {
        ArrayAdapter<String> gameKindArray = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, dateVenueHashMap.get(key).toArray(new String[dateVenueHashMap.get(key).size()]));
        gameKindArray.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        venueSpinner.setAdapter(gameKindArray);

        venueSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activity.reportActionToAnalytics("sessionVenueSelected", dateVenueHashMap.get(key).get(position));
                addSessionsData(dateVenueHashMap.get(key).get(position), key);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        venueSpinner.setSelection(0);
        addSessionsData(dateVenueHashMap.get(key).get(0), dates.get(0));
    }

    EventsAdapter sessionsAdapter;

    public void addSessionsData(String venue, long date) {
        if (sessionsAdapter == null) {
            sessionsAdapter = new EventsAdapter((BaseContextActivity) getActivity());
        }
        sessionsList.setAdapter(sessionsAdapter);
        ArrayList<EventSession> sessionsData = getSessionForDay(venueSessionsMap.get(venue), date);
        if (sessionsData != null)
            sessionsAdapter.setSessionsData(sessionsData, city);

    }

    public ArrayList<EventSession> getSessionForDay(List<EventSession> sessions, long date) {
        ArrayList<EventSession> leftSessions = new ArrayList<>();
        for (EventSession session : sessions) {
            if (session.getDate() == date) {
                leftSessions.add(session);
            }
        }
        return leftSessions;
    }

}
