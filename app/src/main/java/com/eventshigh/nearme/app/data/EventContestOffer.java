package com.eventshigh.nearme.app.data;

import android.net.Uri;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.utils.IntentUtils;

import java.util.Date;

/**
 * An {@link Offer} which is associated with an event and shows event share contest screen.
 */
public class EventContestOffer  extends Offer {
    public final String contestURL;

    public EventContestOffer(String id, String imgUrl, Date offerEndDate,
                             String contestURL) throws IllegalArgumentException {
        super(id, imgUrl, offerEndDate);
        this.contestURL = checkNotEmptyOrNull(contestURL);
    }

    public void launch(BaseActivity activity) {
        activity.reportActionToAnalytics("showOffer", id);

        if (isExpired()) {
            activity.reportActionToAnalytics("expiredShowOffer", id);
            Toast.makeText(activity, R.string.ui_offer_expire, Toast.LENGTH_SHORT).show();
        } else {
            IntentUtils.processContestViewIntent(activity, Uri.parse(contestURL), id);
        }
    }
}
