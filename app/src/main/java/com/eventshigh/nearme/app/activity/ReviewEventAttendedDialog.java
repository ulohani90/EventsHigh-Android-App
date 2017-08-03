package com.eventshigh.nearme.app.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.MyTicketObject;

/**
 * @author shubham
 * @since 20/6/16.
 */

public class ReviewEventAttendedDialog extends DialogFragment implements RatingBar.OnRatingBarChangeListener{

    public static final String MY_TICKET = "my_ticket_object";
    public static final String MY_TICKET_RATING = "my_ticket_rating";
    public static final String MY_TICKET_RATING_BUNDLE = "my_ticket_rating_bundle";

    Bundle bundle;
    MyTicketObject myTicketObject;

    RatingBar ratingBar;
    ImageView imageViewCancel;
    TextView thankYouText,expText;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bundle = getArguments();
        myTicketObject = bundle.getParcelable(MY_TICKET);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_review_event_attened, container, false);
        ratingBar = (RatingBar)view.findViewById(R.id.rb_write_rating);
        ratingBar.setOnRatingBarChangeListener(this);
        imageViewCancel = (ImageView)view.findViewById(R.id.btn_cancel_action);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //do nothing
            }
        });
        imageViewCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        thankYouText = (TextView)view.findViewById(R.id.tv_thanks_text);
        expText = (TextView)view.findViewById(R.id.rate_this_event);

        SpannableString text = new SpannableString("Thank you for attending " +myTicketObject.getEventName()
                +".\nWill you please take a minute to share your experience?");
        StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
        //text.setSpan(boldSpan, 24, text.length() - 55, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new ForegroundColorSpan(Color.parseColor("#666666")), 24, text.length() - 55, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        thankYouText.setText(text);

        expText.setText("Rate your experience at "+myTicketObject.getEventName());
        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
               getActivity().onBackPressed();
            }
        });
        return dialog;
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        onMovieRated(rating);
    }

    public void onMovieRated(float rating){
        Intent intent = new Intent(getActivity(),WriteRecentEventReviewActivity.class);
        Bundle bundle2 = new Bundle();
        bundle2.putFloat(MY_TICKET_RATING, rating);
        intent.putExtra(MY_TICKET_RATING_BUNDLE, bundle2);
        intent.putExtras(bundle);
        getActivity().startActivityForResult(intent, LaunchActivity.SUBMIT_REVIEW_REQUEST_CODE);
        ((BaseContextActivity)getActivity()).reportActionToAnalytics("reviewModelAccepted");

    }

}
