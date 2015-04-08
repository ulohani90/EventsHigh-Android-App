package com.eventshigh.nearme.app.data;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.utils.IntentUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Offer from EventsHigh. Offer is some incentives for taking an actions -- e.g BookMyShow pass
 * for getting referrer install.
 */
public class Offer implements Parcelable {
    public enum OfferType {
        REFERRER_INSTALL,
        EVENT_CONTEST,
    }

    public final String id;
    public final OfferType type;
    public final String imgUrl;
    public final String shortMessage;
    public final String longMessage;
    public final Date offerEndDate;
    public final int claimCount;
    public final String contestURL;

    public Offer(String id, OfferType type, String imgUrl, String shortMessage, String longMessage,
                 Date offerEndDate, int claimCount, String contestURL) {
        this.id = id;
        this.type = type;
        this.imgUrl = imgUrl;
        this.shortMessage = shortMessage;
        this.longMessage = longMessage;
        this.offerEndDate = offerEndDate;
        this.claimCount = claimCount;
        this.contestURL = contestURL;
    }

    public boolean isExpired() {
        return offerEndDate.getTime() < System.currentTimeMillis();
    }

    public boolean isGoodToShow() {
        return offerEndDate.getTime() + TimeUnit.HOURS.toMillis(2) < System.currentTimeMillis();
    }

    public void launch(BaseActivity activity) {
        if (isExpired()) {
            activity.reportActionToAnalytics("expiredShowOffer");
            Toast.makeText(activity, R.string.ui_offer_expire, Toast.LENGTH_SHORT).show();
            return;
        }

        activity.reportActionToAnalytics("showOffer");
        switch (type) {
            case REFERRER_INSTALL:
                activity.shareApp();
                break;
            case EVENT_CONTEST:
                IntentUtils.processContestViewIntent(activity, Uri.parse(contestURL), shortMessage);
                break;
        }
    }

    /**********************************
     Parcel management methods.
     *********************************/
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(type.toString());
        dest.writeString(imgUrl);
        dest.writeString(shortMessage);
        dest.writeLong(offerEndDate.getTime());
        dest.writeInt(claimCount);
        dest.writeString(contestURL);
    }

    // This is used to regenerate your object. All Parcelables must have
    // a CREATOR that implements these two methods
    public static final Parcelable.Creator<Offer> CREATOR =
            new Parcelable.Creator<Offer>() {
                public Offer createFromParcel(Parcel in) {
                    return new Offer(in.readString(),
                            OfferType.valueOf(in.readString()),
                            in.readString(),
                            in.readString(),
                            in.readString(),
                            new Date(in.readLong()),
                            in.readInt(),
                            in.readString()
                    );
                }

                public Offer[] newArray(int size) {
                    return new Offer[size];
                }
            };
}
