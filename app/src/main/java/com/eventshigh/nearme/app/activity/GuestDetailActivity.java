package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.stream.AdditionalTicketField;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class GuestDetailActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String GUEST_DETAIL_ARRAY = "guest_detail_array";

    private static int noOfGuestDetails;

    List<LinearLayout> listGuestDetailLayout;
    List<LinearLayout> listAdditionalFieldsLayout;

    List<AdditionalTicketField> additionalTicketFieldList;
    JSONArray jsonArrayGuestDetail;
    Event event;
    Bundle bundle;
    LinearLayout llGuestDetailLayoutContainer;
    Button btnNextGuestDetails;
    ScrollView svDetailCards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mapViews();
        bundle = getIntent().getExtras();
        event = bundle.getParcelable(EventDetailActivity.EVENT_OBJECT);
        additionalTicketFieldList = event.additionalTicketFieldList;
        noOfGuestDetails = (event.isRequestPerAttendeeData()) ?
                (int) bundle.getDouble(EventBookingDetailActivity.EVENT_TOTAL_TICKETS) : 1;
        addGuestCradLayouts();
        btnNextGuestDetails.setOnClickListener(this);
        preFillDetails();
    }

    private void preFillDetails(){
        LinearLayout llFirstLayout = listGuestDetailLayout.get(0);
        String firstGuestName = new Account(this).getUserInfo().name;
        if(!Utils.checkIfStringEmpty(firstGuestName))
        ((TextView)llFirstLayout.findViewById(R.id.et_guest_name)).setText(firstGuestName);

        String firstGuestPhoneNo = new Account(this).getUserInfo().phoneNo;
        if(!Utils.checkIfStringEmpty(firstGuestPhoneNo))
        ((TextView)llFirstLayout.findViewById(R.id.et_guest_phone)).setText(firstGuestPhoneNo);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_guest_detail_next:
                if (checkAllEditTextFilled()) {
                    Intent iNext = new Intent(this, TicketReviewActivity.class);
                    bundle.putString(GUEST_DETAIL_ARRAY, jsonArrayGuestDetail.toString());
                    iNext.putExtras(bundle);
                    startActivity(iNext);
                }
                break;
        }
    }

    private void mapViews() {
        svDetailCards = (ScrollView) findViewById(R.id.sv_guest_detail_cards);
        llGuestDetailLayoutContainer = (LinearLayout) findViewById(R.id.ll_guest_detail_layout_container);
        btnNextGuestDetails = (Button) findViewById(R.id.btn_guest_detail_next);
    }

    //add user detail
    private void addGuestCradLayouts() {
        listGuestDetailLayout = new ArrayList<>();
        listAdditionalFieldsLayout = new ArrayList<>();

        for (int i = 0; i < noOfGuestDetails; i++) {
            LinearLayout llGuestLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.card_guest_detail, null);
            i++;
            ((TextView) llGuestLayout.findViewById(R.id.tv_guest_no)).setText("Guest " + i);
            i--;
            addAditionalFieldViews(llGuestLayout);
            llGuestDetailLayoutContainer.addView(llGuestLayout);
            listGuestDetailLayout.add(llGuestLayout);
        }
    }


    //mapping and creating additional field programmatically
    private void addAditionalFieldViews(LinearLayout llGuestDetailCard) {
        for (AdditionalTicketField additionalTicketField : additionalTicketFieldList) {
            if (additionalTicketField.getType().equalsIgnoreCase("Text")) {
                LinearLayout textFieldLinearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.additional_ticket_text_field, null);
                textFieldLinearLayout.setTag(additionalTicketField.getName());
                TextView tvTextField = (TextView) textFieldLinearLayout.findViewById(R.id.tv_guest_text);
                tvTextField.setHint(additionalTicketField.getName());
                llGuestDetailCard.addView(textFieldLinearLayout);
                listAdditionalFieldsLayout.add(textFieldLinearLayout);
            }else if(additionalTicketField.getType().equalsIgnoreCase("Number")){
                LinearLayout textFieldLinearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.additional_ticket_text_field, null);
                textFieldLinearLayout.setTag(additionalTicketField.getName());
                TextView tvTextField = (TextView) textFieldLinearLayout.findViewById(R.id.tv_guest_text);
                tvTextField.setHint(additionalTicketField.getName());
                tvTextField.setInputType(InputType.TYPE_CLASS_PHONE);
                llGuestDetailCard.addView(textFieldLinearLayout);
                listAdditionalFieldsLayout.add(textFieldLinearLayout);
            }else if (additionalTicketField.getType().equalsIgnoreCase("One-of")){
                LinearLayout radioLinearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.additional_ticket_radio_group, null);
                radioLinearLayout.setTag(additionalTicketField.getName());
                TextView tvRadioTitle = (TextView) radioLinearLayout.findViewById(R.id.tv_guest_radio_name);
                tvRadioTitle.setText(additionalTicketField.getName());
                RadioGroup rgOneOf = (RadioGroup) radioLinearLayout.findViewById(R.id.rg_guest_radio_group);
                for (int i = 0; i < additionalTicketField.getOptions().size(); i++) {
                    String option = additionalTicketField.getOptions().get(i);
                    RadioButton radioButton = new RadioButton(this);
                    radioButton.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    radioButton.setText(option);
                    radioButton.setId(i);
                    radioButton.setTextColor(Color.parseColor("#353535"));

                    rgOneOf.addView(radioButton);
                }
                llGuestDetailCard.addView(radioLinearLayout);
                listAdditionalFieldsLayout.add(radioLinearLayout);
            }
        }
    }

    //method to check if all field are filled while submitting guest details
    private boolean checkAllEditTextFilled() {
        jsonArrayGuestDetail = new JSONArray();
        boolean is_details_complete = true;
        boolean isLoopBreak = false;
        for (LinearLayout ll_guest_detail : listGuestDetailLayout) {
            if (isLoopBreak) break;
            JSONObject jsonObject = new JSONObject();
            try {
                if (!Utils.checkIfStringEmpty(((TextView) ll_guest_detail.findViewById(R.id.et_guest_name)).getText().toString())) {
                    jsonObject.put("firstName", ((TextView) ll_guest_detail.findViewById(R.id.et_guest_name)).getText().toString());
                } else {
                    is_details_complete = false;
                    focusOnView(ll_guest_detail.findViewById(R.id.et_guest_name));
                    Toast.makeText(this, "All fields are compulsory.", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (!Utils.checkIfStringEmpty(((TextView) ll_guest_detail.findViewById(R.id.et_guest_email)).getText().toString())) {
                    if (Utils.isValidEmail(((TextView) ll_guest_detail.findViewById(R.id.et_guest_email)).getText().toString())) {
                        jsonObject.put("email", ((TextView) ll_guest_detail.findViewById(R.id.et_guest_email)).getText().toString());
                    } else {
                        Toast.makeText(this, "Email Address is not valid", Toast.LENGTH_SHORT).show();
                        is_details_complete = false;
                        focusOnView(ll_guest_detail.findViewById(R.id.et_guest_email));
                        break;
                    }
                } else {
                    is_details_complete = false;
                    focusOnView(ll_guest_detail.findViewById(R.id.et_guest_email));
                    Toast.makeText(this, "All fields are compulsory.", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (!Utils.checkIfStringEmpty(((TextView) ll_guest_detail.findViewById(R.id.et_guest_phone)).getText().toString())) {
                    if (Utils.isValidPhone(((TextView) ll_guest_detail.findViewById(R.id.et_guest_phone)).getText().toString())) {
                        jsonObject.put("mobile", ((TextView) ll_guest_detail.findViewById(R.id.et_guest_phone)).getText().toString());
                    } else {
                        Toast.makeText(this, "10 digit Phone No is required", Toast.LENGTH_SHORT).show();
                        is_details_complete = false;
                        focusOnView(ll_guest_detail.findViewById(R.id.et_guest_phone));
                        break;
                    }
                } else {
                    is_details_complete = false;
                    focusOnView(ll_guest_detail.findViewById(R.id.et_guest_phone));
                    Toast.makeText(this, "All fields are compulsory.", Toast.LENGTH_SHORT).show();
                    break;
                }

                for (AdditionalTicketField additionalTicketField : additionalTicketFieldList) {
                    if (additionalTicketField.getType().equalsIgnoreCase("Text") || additionalTicketField.getType().equalsIgnoreCase("Number")) {
                        LinearLayout llTextField = (LinearLayout) ll_guest_detail.findViewWithTag(additionalTicketField.getName());
                        TextView tvTextField = (TextView) llTextField.findViewById(R.id.tv_guest_text);
                        if (!Utils.checkIfStringEmpty(tvTextField.getText().toString())) {
                            jsonObject.put(additionalTicketField.getName(), tvTextField.getText().toString());
                        } else {
                            is_details_complete = false;
                            isLoopBreak = true;
                            focusOnAdditionalView(llTextField);
                            Toast.makeText(this, "All fields are compulsory.", Toast.LENGTH_SHORT).show();
                            break;
                        }
                    } else if (additionalTicketField.getType().equalsIgnoreCase("One-of")) {
                        LinearLayout llOneOf = (LinearLayout) ll_guest_detail.findViewWithTag(additionalTicketField.getName());
                        RadioGroup rgOneOf = (RadioGroup) llOneOf.findViewById(R.id.rg_guest_radio_group);
                        int selectedId = rgOneOf.getCheckedRadioButtonId();
                        if (selectedId != -1) {
                            RadioButton radioSexButton = (RadioButton) findViewById(selectedId);
                            jsonObject.put(additionalTicketField.getName(), radioSexButton.getText().toString());
                        } else {
                            is_details_complete = false;
                            isLoopBreak = true;
                            focusOnAdditionalView(llOneOf);
                            Toast.makeText(this, "All fields are compulsory.", Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                }

            } catch (JSONException e) {
                is_details_complete = false;
            }
            jsonArrayGuestDetail.put(jsonObject);
        }
        return is_details_complete;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //to get scroll to not-filled view
    private final void focusOnView(final View view) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                svDetailCards.smoothScrollTo(0, ((View) ((View) view.getParent()).getParent()).getTop()
                        + ((View) view.getParent()).getTop());
                view.setFocusableInTouchMode(true);
                view.requestFocus();
            }
        });
    }

    private final void focusOnAdditionalView(final View view) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                svDetailCards.smoothScrollTo(0, ((View) view.getParent()).getTop()
                        + view.getTop());
                view.setFocusableInTouchMode(true);
                view.requestFocus();
            }
        });
    }

}
