package com.eventshigh.nearme.app.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;

/**
 * Created by umesh on 27/11/17.
 */

public class EventHighlightsDialogFragment extends DialogFragment {


    LinkedTreeMap<String, Object> configMap;

    int width;

    public static EventHighlightsDialogFragment newInstance(Bundle bundle) {
        EventHighlightsDialogFragment fragment = new EventHighlightsDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String config = getArguments().getString("config");
        width = getArguments().getInt("width");
        configMap = new Gson().fromJson(config, new TypeToken<LinkedTreeMap<String, Object>>() {
        }.getType());


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.event_highlights_dialog_layout, container, false);
        //Add Highlights
        view.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        if (checkIfKeyHasValue("special_highlights") || checkIfKeyHasValue("artists_performing")) {
            view.findViewById(R.id.highlights_layout).setVisibility(View.VISIBLE);
            view.findViewById(R.id.highlights_border).setVisibility(View.VISIBLE);
            if (checkIfKeyHasValue("special_highlights")) {
                LinearLayout highlightsContainer = (LinearLayout) view.findViewById(R.id.highlights_container);
                highlightsContainer.setVisibility(View.VISIBLE);
                view.findViewById(R.id.highlights_title).setVisibility(View.VISIBLE);
                String[] value = ((String) ((LinkedTreeMap<String, Object>) configMap.get("special_highlights")).get("value")).split(",");
                for (int i = 0; i < value.length; i++) {
                    View highlightView = getActivity().getLayoutInflater().inflate(R.layout.textview_layout, highlightsContainer, false);
                    TextView textView = (TextView) highlightView.findViewById(R.id.textview_text);
                    textView.setText("\u2022 " + value[i].trim());
                    highlightsContainer.addView(highlightView);
                }
            } else {
                view.findViewById(R.id.highlights_container).setVisibility(View.GONE);
                view.findViewById(R.id.highlights_title).setVisibility(View.GONE);
            }

            if (checkIfKeyHasValue("artists_performing")) {
                LinearLayout artistsContainer = (LinearLayout) view.findViewById(R.id.artists_container);
                artistsContainer.setVisibility(View.VISIBLE);
                view.findViewById(R.id.artists_title).setVisibility(View.VISIBLE);
                String[] value = ((String) ((LinkedTreeMap<String, Object>) configMap.get("artists_performing")).get("value")).split(",");
                for (int i = 0; i < value.length; i++) {
                    View highlightView = getActivity().getLayoutInflater().inflate(R.layout.textview_layout, artistsContainer, false);
                    TextView textView = (TextView) highlightView.findViewById(R.id.textview_text);
                    textView.setText("\u2022 " + value[i].trim());
                    artistsContainer.addView(highlightView);
                }
            } else {
                view.findViewById(R.id.artists_container).setVisibility(View.GONE);
                view.findViewById(R.id.artists_title).setVisibility(View.GONE);
            }


        } else {
            view.findViewById(R.id.highlights_border).setVisibility(View.GONE);
            view.findViewById(R.id.highlights_layout).setVisibility(View.GONE);
        }
        //Add party

        if (checkIfParentChildKeyHasValue("is_parties_and_nightlife", "party_venue_type")
                || checkIfParentChildKeyHasValue("is_parties_and_nightlife", "venue_view")
                || checkIfKeyHasValue("is_unlimited_food")
                || checkIfKeyHasValue("is_unlimited_alcohol")
                || checkIfParentChildKeyHasValue("is_parties_and_nightlife", "is_stags_allowed")
                || checkIfParentChildKeyHasValue("is_parties_and_nightlife", "is_parking_available")
                || checkIfKeyHasValue("is_group_discounts")) {
            LinearLayout partyLayout = (LinearLayout) view.findViewById(R.id.party_info_layout);
            partyLayout.setVisibility(View.VISIBLE);
            view.findViewById(R.id.party_border).setVisibility(View.VISIBLE);

            int partyLayoutCount = 1;
            int childCount = 0;
            if (checkIfParentChildKeyHasValue("is_parties_and_nightlife", "party_venue_type")) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("party_venue_type")).get("value");
                addPartyVenue(partyLayoutCount, childCount, view, "Venue Type", value, "party_info_layout_", "party_info_textview_");

            }
            if (checkIfParentChildKeyHasValue("is_parties_and_nightlife", "venue_view")) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("venue_view")).get("value");
                addPartyVenue(partyLayoutCount, childCount, view, "Venue View", value, "party_info_layout_", "party_info_textview_");
            }
            if (checkIfKeyHasValue("is_unlimited_food")) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_unlimited_food")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : "");
                addPartyVenue(partyLayoutCount, childCount, view, "Unlimited Food Available", finalValue, "party_info_layout_", "party_info_textview_");
            }
            if (checkIfKeyHasValue("is_unlimited_alcohol")) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_unlimited_alcohol")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : "");
                addPartyVenue(partyLayoutCount, childCount, view, "Unlimited Alcohol Available", finalValue, "party_info_layout_", "party_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_parties_and_nightlife", "is_stags_allowed")) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("is_stags_allowed")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : "");
                addPartyVenue(partyLayoutCount, childCount, view, "Stag Entry Allowed", finalValue, "party_info_layout_", "party_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_parties_and_nightlife", "is_parking_available")) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("is_parking_available")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : "");
                addPartyVenue(partyLayoutCount, childCount, view, "Parking Available", finalValue, "party_info_layout_", "party_info_textview_");
            }
            if (checkIfKeyHasValue("is_group_discounts")) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_group_discounts")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : "");
                addPartyVenue(partyLayoutCount, childCount, view, "Group Discounts", finalValue, "party_info_layout_", "party_info_textview_");
            }

        } else {
            view.findViewById(R.id.party_info_layout).setVisibility(View.GONE);
            view.findViewById(R.id.party_border).setVisibility(View.GONE);
        }
        //Add Outdoors
        if (checkIfParentChildKeyHasValue("is_outdoors", "is_transportation_available")
                || checkIfParentChildKeyHasValue("is_outdoors", "is_alcohol_allowed")
                || checkIfParentChildKeyHasValue("is_outdoors", "tour_duration")
                || checkIfParentChildKeyHasValue("is_outdoors", "outdoor_venue_type")
                || checkIfParentChildKeyHasValue("is_outdoors", "stay_type")
                || checkIfParentChildKeyHasValue("is_outdoors", "tent_sharing_type")
                || checkIfParentChildKeyHasValue("is_outdoors", "food_type")
                || checkIfParentChildKeyHasValue("is_outdoors", "activity_type")) {
            view.findViewById(R.id.outdoor_info_layout).setVisibility(View.VISIBLE);
            view.findViewById(R.id.outdoor_border).setVisibility(View.VISIBLE);
            int outdoorLayoutCount = 1;
            int outdoorChildCount = 0;
            if (checkIfParentChildKeyHasValue("is_outdoors", "is_transportation_available")) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("is_transportation_available")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : "");
                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Transportation Available", finalValue, "outdoor_info_layout_", "outdoor_info_textview_");

            }

            if (checkIfParentChildKeyHasValue("is_outdoors", "is_alcohol_allowed")) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("is_alcohol_allowed")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : "");
                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Alcohol Allowed", finalValue, "outdoor_info_layout_", "outdoor_info_textview_");

            }

            if (checkIfParentChildKeyHasValue("is_outdoors", "tour_duration")) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("tour_duration")).get("value");

                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Duration", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }
            if (checkIfParentChildKeyHasValue("is_outdoors", "outdoor_venue_type")) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("outdoor_venue_type")).get("value");

                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Venue type", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }
            if (checkIfParentChildKeyHasValue("is_outdoors", "stay_type")) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("stay_type")).get("value");

                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Stay type", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }

            if (checkIfParentChildKeyHasValue("is_outdoors", "tent_sharing_type")) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("tent_sharing_type")).get("value");

                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Tent sharing type", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }
            if (checkIfParentChildKeyHasValue("is_outdoors", "food_type")) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("food_type")).get("value");

                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Food type", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }
            if (checkIfParentChildKeyHasValue("is_outdoors", "activity_type")) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("activity_type")).get("value");

                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Activity type", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }

        } else {
            view.findViewById(R.id.outdoor_info_layout).setVisibility(View.GONE);
            view.findViewById(R.id.outdoor_border).setVisibility(View.GONE);
        }

        //Add Stay type
        if (checkIfKeyHasValue("is_kid_friendly")
                || checkIfParentChildKeyHasValue("is_kid_friendly", "is_free_for_kids_below_five")
                || checkIfParentChildKeyHasValue("is_kid_friendly", "is_child_care_zone")
                || checkIfParentChildKeyHasValue("is_kid_friendly", "max_age_kids_pricing")
                || checkIfParentChildKeyHasValue("is_kid_friendly", "kid_activities")) {
            view.findViewById(R.id.kids_info_layout).setVisibility(View.VISIBLE);
            view.findViewById(R.id.kids_border).setVisibility(View.VISIBLE);

            int kidsLayoutCount = 1;
            int kidsChildCount = 0;
            if (checkIfKeyHasValue("is_kid_friendly")) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : "");
                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Kids Friendly", finalValue, "kids_info_layout_", "kids_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_kid_friendly", "is_free_for_kids_below_five")) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("is_free_for_kids_below_five")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : "");
                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Free for kids below 5 years", finalValue, "kids_info_layout_", "kids_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_kid_friendly", "is_child_care_zone")) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("is_child_care_zone")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : "");
                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Child care zone available", finalValue, "kids_info_layout_", "kids_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_kid_friendly", "max_age_kids_pricing")) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("max_age_kids_pricing")).get("value");

                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Max age for kids pricing ", value, "kids_info_layout_", "kids_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_kid_friendly", "kid_activities")) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("kid_activities")).get("value");


                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Activities for Kids", value, "kids_info_layout_", "kids_info_textview_");
            }

        } else {
            view.findViewById(R.id.kids_info_layout).setVisibility(View.GONE);
            view.findViewById(R.id.kids_border).setVisibility(View.GONE);
        }

        //Add Stay Type
        if (checkIfKeyHasValue("is_stay_provided")
                || checkIfParentChildKeyHasValue("is_stay_provided", "is_breakfast_included")
                || checkIfParentChildKeyHasValue("is_stay_provided", "is_extra_bed_available")
                || checkIfParentChildKeyHasValue("is_stay_provided", "check_in_time")
                || checkIfParentChildKeyHasValue("is_stay_provided", "check_out_time")) {
            view.findViewById(R.id.stay_info_layout).setVisibility(View.VISIBLE);
            int kidsLayoutCount = 1;
            int kidsChildCount = 0;
            if (checkIfKeyHasValue("is_stay_provided")) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : "");
                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Stay Provided", finalValue, "stay_info_layout_", "stay_info_textview_");
            }
            if (checkIfParentChildKeyHasValue("is_stay_provided", "is_breakfast_included")) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("is_breakfast_included")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : "");
                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Breakfast included", finalValue, "stay_info_layout_", "stay_info_textview_");
            }
            if (checkIfParentChildKeyHasValue("is_stay_provided", "is_extra_bed_available")) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("is_extra_bed_available")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : "");
                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Extra Bed Available", finalValue, "stay_info_layout_", "stay_info_textview_");
            }
            if (checkIfParentChildKeyHasValue("is_stay_provided", "check_in_time")) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("check_in_time")).get("value");

                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Check in time", value, "stay_info_layout_", "stay_info_textview_");
            }
            if (checkIfParentChildKeyHasValue("is_stay_provided", "check_out_time")) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("check_out_time")).get("value");

                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Check out time", value, "stay_info_layout_", "stay_info_textview_");
            }

        } else {
            view.findViewById(R.id.stay_info_layout).setVisibility(View.GONE);
        }


        return view;
    }

    public void addPartyVenue(int layoutCount, int childCount, View view, String keyName, String value, String layoutName, String textViewName) {
        try {
            int layoutKey = R.id.class.getField(layoutName + layoutCount).getInt(null);
            int textViewKey = R.id.class.getField(textViewName + layoutCount + childCount).getInt(null);
            view.findViewById(layoutKey).setVisibility(View.VISIBLE);
            TextView textView = (TextView) view.findViewById(textViewKey);
            textView.setVisibility(View.VISIBLE);
            SpannableString string = new SpannableString(keyName + " : " + value);
            string.setSpan(new StyleSpan(Typeface.BOLD), keyName.length(), string.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(string);

        } catch (NoSuchFieldException | IllegalAccessException e) {

        }
    }

    public boolean checkIfKeyHasValue(String key) {
        if (configMap.containsKey(key)) {
            LinkedTreeMap<String, Object> map = (LinkedTreeMap<String, Object>) configMap.get(key);
            if (map.containsKey("value")
                    && map.get("value") != null
                    && ((String) map.get("value")).length() > 0
                    && !((String) map.get("value")).equalsIgnoreCase("n/a")) {
                return true;
            }
        }
        return false;
    }

    public boolean checkIfParentChildKeyHasValue(String parentKey, String childkey) {
        if (configMap.containsKey(parentKey)) {
            LinkedTreeMap<String, Object> map = (LinkedTreeMap<String, Object>) configMap.get(parentKey);
            if (map.containsKey(childkey)) {
                LinkedTreeMap<String, Object> childMap = (LinkedTreeMap<String, Object>) map.get(childkey);
                if (childMap.containsKey("value")
                        && childMap.get("value") != null
                        && ((String) childMap.get("value")).length() > 0 &&
                        !((String) childMap.get("value")).equalsIgnoreCase("n/a")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        int width = getResources().getDisplayMetrics().widthPixels;
                //- (int) (2 * TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics())));
        int height = (getResources().getDisplayMetrics().heightPixels
                - (int) (2 * TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics())));

        dialog.getWindow().setLayout(width, height);


        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                getActivity().onBackPressed();
            }
        });
        return dialog;
    }


}
