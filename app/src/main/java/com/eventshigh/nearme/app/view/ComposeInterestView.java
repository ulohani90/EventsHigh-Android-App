package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.utils.Utils;

/**
 * Created by umesh on 11/07/16.
 */
public class ComposeInterestView extends FrameLayout implements ZRuntimeView {

    private TextView textView;
    private ImageView followImg;
    private View view;

    Context context;

    public ComposeInterestView(Context context) {
        super(context);
        init(context);
    }

    public ComposeInterestView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ComposeInterestView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    @Override
    public void init(Context context) {
        setupView(context);
    }

    @Override
    public void setupView(Context context) {
        this.setPadding(0, 0, 5, 0);
        this.context = context;
        view = ((LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE)).inflate(
                R.layout.compose_interest_layout, null);
        textView = (TextView) view.findViewById(R.id.interest_name);
        followImg = (ImageView) view.findViewById(R.id.follow_cancel);
        followImg.setVisibility(View.GONE);
        //  categoryTagParent = (LinearLayout) view.findViewById(R.id.category_tag_parent);
        this.addView(view);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    public void setContent(String tag) {
        textView.setText(Utils.capitalize(tag));
    }

    public ImageView getFollowImg() {
        return followImg;
    }
}
