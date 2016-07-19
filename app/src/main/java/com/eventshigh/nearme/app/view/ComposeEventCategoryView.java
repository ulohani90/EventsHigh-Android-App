package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.LaunchActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by umesh on 21/06/16.
 */
public class ComposeEventCategoryView extends FrameLayout implements ZRuntimeView {

    private TextView textView;
    private ImageView followImg;
    private View view;
    private LinearLayout categoryTagParent;
    Context context;

    public ComposeEventCategoryView(Context context) {
        super(context);
        init(context);
    }

    public ComposeEventCategoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ComposeEventCategoryView(Context context, AttributeSet attrs, int defStyle) {
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
        this.context = context;
        view = ((LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE)).inflate(
                R.layout.compose_category_layout, null);
        textView = (TextView) view.findViewById(R.id.category_name);
        followImg = (ImageView) view.findViewById(R.id.follow_img);
        categoryTagParent = (LinearLayout) view.findViewById(R.id.category_tag_parent);
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

    public void setContent(final BaseContextActivity activity, final Account account, final String obj, final Event event, final String action) {
        textView.setText(obj);
        setMarkedViews(account.isFollowing(obj));
        categoryTagParent.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity != null) {
                    activity.reportEventAction(event, action, obj);
                    Intent searchIntent = new Intent(activity, LaunchActivity.class);
                    searchIntent.putExtra(IntentUtils.EXTRA_EVENT_CONTEXT,
                            new EventsContext(account.getLastCity().cityBounds.getCenter(), obj.toLowerCase()));
                    activity.startActivity(searchIntent);
                }
            }
        });
        followImg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity != null && account != null) {
                    if (categoryTagParent.isSelected()) {
                        activity.reportActionToAnalytics("removeFollowing", obj);
                        account.setIsFollowing(obj, true);
                        setMarkedViews(false);
                        Toast.makeText(context, "You unfollowed " + obj, Toast.LENGTH_SHORT).show();
                    } else {
                        activity.reportActionToAnalytics("addFollowing", obj);
                        if (!account.getUserInfo().isVerified) {
                            PhoneVerificationDialog.show(activity,
                                    R.string.ui_verify_phone, R.string.ui_phone_verify_pa);
                        }
                        account.setIsFollowing(obj, true);
                        setMarkedViews(true);
                        Toast.makeText(context, "You are now following " + obj, Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });
    }

    public void setMarkedViews(boolean isSelected) {
        if (isSelected) {
            textView.setSelected(true);
            categoryTagParent.setSelected(true);
            followImg.setSelected(true);
        } else {
            textView.setSelected(false);
            categoryTagParent.setSelected(false);
            followImg.setSelected(false);
        }
    }
}
