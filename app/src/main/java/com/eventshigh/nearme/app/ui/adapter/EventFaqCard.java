package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.EventZendeskTicketObject;
import com.eventshigh.nearme.app.utils.DateTimeUtils;

/**
 * Created by umesh on 15/03/17.
 */

public class EventFaqCard extends RecyclerView.ViewHolder {


    TextView questionText, questionTime;

    LinearLayout answersLayout;

    public static EventFaqCard newInstance(BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_event_faq_layout, parent, false);
        return new EventFaqCard(view);
    }

    public EventFaqCard(View itemView) {
        super(itemView);
        questionText = (TextView) itemView.findViewById(R.id.question);
        questionTime = (TextView) itemView.findViewById(R.id.question_time);
        answersLayout = (LinearLayout) itemView.findViewById(R.id.answers_container);

    }

    public void bindData(EventZendeskTicketObject obj, BaseActivity activity) {
        questionText.setText(Html.fromHtml(obj.question));
        //questionText.setMovementMethod(LinkMovementMethod.getInstance());
        questionTime.setText(DateTimeUtils.getDateFromLongTime(obj.timeStamp));
        answersLayout.removeAllViews();
        if (obj.answers != null) {
            for (int i = 0; i < obj.answers.size(); i++) {


                View view = LayoutInflater.from(activity).inflate(R.layout.faq_answer_layout, answersLayout, false);
                TextView answer = (TextView) view.findViewById(R.id.answer_text);
                TextView answerTime = (TextView) view.findViewById(R.id.answer_time);
                View separator = view.findViewById(R.id.separator);
                answerTime.setText(DateTimeUtils.getDateFromLongTime(obj.answers.get(i).timeStamp));
                answer.setText(Html.fromHtml(obj.answers.get(i).answer));
                if (i == obj.answers.size() - 1) {
                    separator.setVisibility(View.GONE);
                } else {
                    separator.setVisibility(View.VISIBLE);
                }
                answersLayout.addView(view);
            }
        }
    }
}
