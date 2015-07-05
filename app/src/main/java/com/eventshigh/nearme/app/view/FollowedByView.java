package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.ui.FriendsDialog;
import com.eventshigh.nearme.app.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;

public class FollowedByView extends HorizontalScrollView {

    public FollowedByView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setFollowers(final BaseActivity activity, @Nullable final Set<UserContact> contacts,
            final @Nullable String text, int gravity) {
        removeAllViews();
        if (contacts == null || contacts.isEmpty()) {
            return;
        }

        LinearLayout container = new LinearLayout(getContext());
        container.setLayoutParams(
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, gravity));
        addView(container);

        int size = Utils.dpToPx(getContext(), 24);
        for (UserContact contact : contacts) {
            new ContactPhotoLoaderTask(container, size, text).execute(contact);
        }

        container.setClickable(true);
        container.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<UserContact> contactList = new ArrayList<>(contacts.size());
                contactList.addAll(contacts);
                FriendsDialog.show(activity, contactList);
            }
        });
    }

    public static class ContactPhotoLoaderTask extends AsyncTask<UserContact, Void, Drawable> {
        private final WeakReference<LinearLayout> parentViewReference;
        private final Context context;
        private final int size;
        @Nullable private final String text;

        public ContactPhotoLoaderTask(LinearLayout parentView, int size, @Nullable String text) {
            this.parentViewReference = new WeakReference<>(parentView);

            this.context = parentView.getContext();
            this.size = size;
            this.text = text;
        }

        @Override
        protected Drawable doInBackground(UserContact... contacts) {
            return contacts[0].getDrawable(context, size);
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            LinearLayout parentView = parentViewReference.get();
            if (parentView != null) {
                if (text != null && parentView.getChildCount() == 0) {
                    TextView tv = new TextView(parentView.getContext());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
                    lp.topMargin = Utils.dpToPx(parentView.getContext(), 2);
                    tv.setLayoutParams(lp);
                    tv.setGravity(Gravity.CENTER);
                    tv.setTextColor(parentView.getContext().getResources().getColor(android.R.color.black));
                    tv.setText(text);
                    parentView.addView(tv);
                }

                ImageView imageView = new ImageView(parentView.getContext());
                imageView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
                imageView.setImageDrawable(drawable);
                parentView.addView(imageView, 0);
            }
        }
    }
}
