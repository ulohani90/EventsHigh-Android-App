package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.utils.ContactUtils;
import com.eventshigh.nearme.app.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.Set;

public class FollowedByView extends HorizontalScrollView {

    public FollowedByView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setFollowers(@Nullable Set<String> contactIds, boolean addFollowedBy, int gravity) {
        removeAllViews();
        if (contactIds == null || contactIds.isEmpty()) {
            return;
        }

        LinearLayout container = new LinearLayout(getContext());
        container.setLayoutParams(
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, gravity));
        addView(container);

        int size = Utils.dpToPx(getContext(), 24);
        for (String contactId : contactIds) {
            new ContactPhotoLoaderTask(container, size, addFollowedBy).execute(contactId);
        }
    }

    public static class ContactPhotoLoaderTask extends AsyncTask<String, Void, Bitmap> {
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
        protected @Nullable Bitmap doInBackground(String... contactIds) {
            return ContactUtils.getPhotoForContactId(context, contactIds[0], size);
        }

        @Override
        protected void onPostExecute(@Nullable Bitmap bitmap) {
            LinearLayout parentView = parentViewReference.get();
            if (parentView != null && bitmap != null) {
                if (addFollowedBy && parentView.getChildCount() == 0) {
                    TextView tv = new TextView(parentView.getContext());
                    tv.setText("Followed By: ");
                    parentView.addView(tv);
                }

                ImageView imageView = new ImageView(parentView.getContext());
                imageView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
                imageView.setImageBitmap(bitmap);
                parentView.addView(imageView);
            }
        }
    }
}
