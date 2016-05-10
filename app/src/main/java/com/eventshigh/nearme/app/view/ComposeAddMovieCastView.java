package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;

/**
 * Created by umesh on 06/05/16.
 */
public class ComposeAddMovieCastView extends FrameLayout implements ZRuntimeView {

    @SuppressWarnings("unused")
    private RemoveviewListener removeViewListener;

    private TextView textView;
    private View view;

    public void setRemoveViewListener(RemoveviewListener removeViewListener) {
        this.removeViewListener = removeViewListener;
    }

    public ComposeAddMovieCastView(Context context) {
        super(context);
        init(context);
    }

    public ComposeAddMovieCastView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ComposeAddMovieCastView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    @Override
    public void init(Context context) {
        setupView(context);
    }

    @Override
    public void setupView(Context context) {

        this.setPadding(0, 0, 5, 0);

        view = ((LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE)).inflate(
                R.layout.compose_movie_cast_view, null);
        textView = (TextView) view.findViewById(R.id.cast_name);

        this.addView(view);

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

		/*
         * ComposeAddTagsView.this.setAnimation(AnimationUtils.loadAnimation(
		 * getContext(), R.anim.anim_bottomtotopscale));
		 */

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

		/*
		 * ComposeAddTagsView.this.setAnimation(AnimationUtils.loadAnimation(
		 * getContext(), R.anim.anim_toptobottomscale));
		 */

    }

    public void changeText(String subjectName) {
        textView.setText(subjectName);
    }


    public interface RemoveviewListener {
        public void onremoveView(View view, Object tag);
    }


}
