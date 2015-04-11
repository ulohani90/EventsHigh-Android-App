package com.eventshigh.nearme.app.data;

import android.net.Uri;

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

    public boolean launch(BaseActivity activity) {
        if (!super.launch(activity)) {
            IntentUtils.processContestViewIntent(activity, Uri.parse(contestURL), id);
        }
        return true;
    }
}
