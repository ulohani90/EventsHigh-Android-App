package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventInfoObject;
import com.eventshigh.nearme.app.user.Account;

import java.util.ArrayList;

/**
 * This Class Defines the FlowLayout Class for adding and removing the Compose
 * Views
 *
 * @author Umesh Lohani
 */
public class ZCustomFlowLayout extends ZFlowLayout {

    OnRecipientRemoveListener mListener;

    OnHashTagClickListener listener;

    ZComposeAddTagsView adduserView;

    int viewsWidth;

    public ZCustomFlowLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

    }

    public void setOnRecipientRemoveListener(OnRecipientRemoveListener listener) {
        this.mListener = listener;

    }

    public void setOnHashTagClickListener(OnHashTagClickListener listener) {
        this.listener = listener;

    }

    public void setReceipentsForShowTimes(
            final ArrayList<String> objects, boolean addBackground) {

        if (this.getChildCount() > 0)
            this.removeAllViews();
        viewsWidth = 0;
        ZComposeAddTagsView adduserView;
        for (int i = 0; i < objects.size(); i++) {
            final String obj = objects.get(i);
            adduserView = new ZComposeAddTagsView(getContext());

            if (addBackground) {
                adduserView.addBackGround();
            }
            adduserView.setTag(obj);
            adduserView.changeText(obj);

            adduserView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.selectedHashTag(obj);
                    }
                }
            });

            this.addView(adduserView);

        }

    }

    public void setReceipentsForMoVieCasts(
            final ArrayList<String> objects, boolean addBackground) {

        if (this.getChildCount() > 0)
            this.removeAllViews();
        viewsWidth = 0;
        ComposeAddMovieCastView adduserView;
        for (int i = 0; i < objects.size(); i++) {
            final String obj = objects.get(i);
            adduserView = new ComposeAddMovieCastView(getContext());

            adduserView.setTag(obj);
            adduserView.changeText(obj);

            adduserView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.selectedHashTag(obj);
                    }
                }
            });

            this.addView(adduserView);

        }

    }

    public void setRecipientForEventCategories(BaseContextActivity activity, Account account, ArrayList<String> tags, EventInfoObject event, String action) {

        if (this.getChildCount() > 0)
            this.removeAllViews();
        viewsWidth = 0;
        ComposeEventCategoryView adduserView;
        for (int i = 0; i < tags.size(); i++) {
            final String obj = tags.get(i);
            adduserView = new ComposeEventCategoryView(getContext());

            adduserView.setTag(obj);
            adduserView.setContent(activity, account, obj, event, action);

            adduserView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.selectedHashTag(obj);
                    }
                }
            });

            this.addView(adduserView);

        }
    }

    public void setRecipientForSelectedInterest(final ArrayList<String> tags) {
        if (this.getChildCount() > 0)
            this.removeAllViews();
        viewsWidth = 0;
        ComposeInterestView adduserView;
        for (int i = 0; i < tags.size(); i++) {
            final String obj = tags.get(i);
            adduserView = new ComposeInterestView(getContext());

            //adduserView.setTag(obj);
            adduserView.setContent(obj);
            adduserView.getFollowImg().setTag(obj);
            adduserView.getFollowImg().setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    tags.remove((String) v.getTag());
                    mListener.refreshRecipientNames((String) v.getTag());
                    setRecipientForSelectedInterest(tags);
                }
            });

            this.addView(adduserView);

        }
    }

    public interface OnHashTagClickListener {
        void selectedHashTag(String obj);

    }

    public interface OnRecipientRemoveListener {
        void refreshRecipientNames(String tag);
    }
}
