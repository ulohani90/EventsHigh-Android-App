package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.utils.ContactUtils;
import com.eventshigh.nearme.app.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.Set;

public class FollowedByView extends HorizontalScrollView {
    private LinearLayout container;

    public FollowedByView(Context context, AttributeSet attrs) {
        super(context, attrs);

        container = new LinearLayout(context);
        container.setLayoutParams(
            new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        addView(container);
    }

    public void setFollowers(@Nullable Set<String> contactIds) {
        container.removeAllViews();
        if (contactIds == null || contactIds.isEmpty()) {
            return;
        }

        int size = Utils.dpToPx(getContext(), 12);
        for (String contactId : contactIds) {
            new ContactPhotoLoaderTask(container, size).execute(contactId);
        }
    }

    public static class ContactPhotoLoaderTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<LinearLayout> parentViewReference;
        private final Context context;
        private final int size;

        public ContactPhotoLoaderTask(LinearLayout parentView, int size) {
            this.parentViewReference = new WeakReference<>(parentView);

            this.context = parentView.getContext();
            this.size = size;
        }

        @Override
        protected @Nullable Bitmap doInBackground(String... contactIds) {
            return ContactUtils.getPhotoForContactId(context, contactIds[0], size);
        }

        @Override
        protected void onPostExecute(@Nullable Bitmap bitmap) {
            LinearLayout parentView = parentViewReference.get();
            if (parentView != null && bitmap != null) {
                if (parentView.getChildCount() == 0) {
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
