package com.eventshigh.nearme.app.data;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.OffersActivity;

import java.util.Date;

/**
 * An {@link Offer} which encourages users to share app.
 */
public class ReferralInstallOffer extends Offer implements Parcelable {
    public final int claimCount;
    public final String message;
    public final String shareMessage;

    public ReferralInstallOffer(String id, String imgUrl, Date offerEndDate,
            int claimCount, String message, String shareMessage) throws IllegalArgumentException {
        super(id, imgUrl, offerEndDate);

        this.claimCount = claimCount;
        this.message = checkNotEmptyOrNull(message);
        this.shareMessage = checkNotEmptyOrNull(shareMessage);
    }

    public void launch(BaseActivity activity) {
        activity.reportActionToAnalytics("showOffer", id);

        activity.startActivity(new Intent(activity, OffersActivity.class)
                .putExtra(OffersActivity.OFFER_EXTRA_PARAM, this));
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
        dest.writeString(imgUrl.toString());
        dest.writeLong(offerEndDate.getTime());
        dest.writeInt(claimCount);
        dest.writeString(message);
        dest.writeString(shareMessage);
    }

    // This is used to regenerate your object. All Parcelables must have
    // a CREATOR that implements these two methods
    public static final Parcelable.Creator<ReferralInstallOffer> CREATOR =
            new Parcelable.Creator<ReferralInstallOffer>() {
                public ReferralInstallOffer createFromParcel(Parcel in) {
                    return new ReferralInstallOffer(in.readString(),
                            in.readString(),
                            new Date(in.readLong()),
                            in.readInt(),
                            in.readString(),
                            in.readString()
                    );
                }

                public ReferralInstallOffer[] newArray(int size) {
                    return new ReferralInstallOffer[size];
                }
            };
}
