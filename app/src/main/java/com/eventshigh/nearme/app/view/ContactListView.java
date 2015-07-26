package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.content.res.TypedArray;
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

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.ui.FriendsDialog;
import com.eventshigh.nearme.app.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.Set;

public class ContactListView extends HorizontalScrollView {
    private int size;
    private int gravity;
    @Nullable private String text;

    public ContactListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setHorizontalScrollBarEnabled(false);

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.ContactListView, 0, 0);
        size = typedArray.getDimensionPixelSize(R.styleable.ContactListView_contactPhotoSize, 12);
        int position = typedArray.getInteger(R.styleable.ContactListView_contactPosition, 0);
        gravity = position == 0 ? Gravity.START : (position == 1) ? Gravity.END : Gravity.CENTER;
        text = typedArray.getString(R.styleable.ContactListView_text);
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setGravity(int gravity) {
        this.gravity = gravity;
    }

    public void setText(@Nullable String text) {
        this.text = text;
    }

    public void setFollowers(final BaseActivity activity, @Nullable final Set<SocialFriend> friends) {
        removeAllViews();
        if (friends == null || friends.isEmpty()) {
            return;
        }

        LinearLayout container = new LinearLayout(getContext());
        container.setLayoutParams(
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, gravity));
        addView(container);

        for (SocialFriend friend : friends) {
            new ContactPhotoLoaderTask(container, size, text).execute(friend);
        }

        container.setClickable(true);
        container.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                FriendsDialog.show(activity, friends);
            }
        });
    }

    public static class ContactPhotoLoaderTask extends AsyncTask<SocialFriend, Void, Drawable> {
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
        protected Drawable doInBackground(SocialFriend... friends) {
            return friends[0].getDrawable(context, size);
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            LinearLayout parentView = parentViewReference.get();
            if (parentView != null) {
                if (text != null && parentView.getChildCount() == 0) {
                    TextView tv = new TextView(parentView.getContext());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
                    lp.gravity = Gravity.CENTER;
                    tv.setLayoutParams(lp);
                    tv.setTextAppearance(context, android.R.style.TextAppearance_Medium);
                    tv.setGravity(Gravity.CENTER);
                    tv.setText(text);
                    parentView.addView(tv);
                }

                ImageView imageView = new ImageView(parentView.getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                lp.leftMargin = Utils.dpToPx(parentView.getContext(), 2);
                lp.rightMargin = Utils.dpToPx(parentView.getContext(), 2);
                lp.gravity = Gravity.CENTER;
                imageView.setLayoutParams(lp);
                imageView.setImageDrawable(drawable);
                parentView.addView(imageView, 0);
            }
        }
    }
}
