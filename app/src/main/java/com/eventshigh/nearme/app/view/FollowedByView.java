package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.Set;

public class FollowedByView extends HorizontalScrollView {

    public FollowedByView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setFollowers(@Nullable Set<UserContact> contacts, boolean addFollowedBy, int gravity) {
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
            new ContactPhotoLoaderTask(container, size, addFollowedBy).execute(contact);
        }
    }

    public static class ContactPhotoLoaderTask extends AsyncTask<UserContact, Void, Drawable> {
        private final WeakReference<LinearLayout> parentViewReference;
        private final Context context;
        private final int size;
        private final boolean addFollowedBy;

        public ContactPhotoLoaderTask(LinearLayout parentView, int size, boolean addFollowedBy) {
            this.parentViewReference = new WeakReference<>(parentView);

            this.context = parentView.getContext();
            this.size = size;
            this.addFollowedBy = addFollowedBy;
        }

        @Override
        protected Drawable doInBackground(UserContact... contacts) {
            return contacts[0].getDrawable(context, size);
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            LinearLayout parentView = parentViewReference.get();
            if (parentView != null) {
                if (addFollowedBy && parentView.getChildCount() == 0) {
                    TextView tv = new TextView(parentView.getContext());
                    tv.setText("Followed By: ");
                    parentView.addView(tv);
                }

                ImageView imageView = new ImageView(parentView.getContext());
                imageView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
                imageView.setImageDrawable(drawable);
                parentView.addView(imageView);
            }
        }
    }
}
