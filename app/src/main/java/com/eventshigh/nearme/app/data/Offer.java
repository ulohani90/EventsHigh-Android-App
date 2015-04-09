package com.eventshigh.nearme.app.data;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.OffersActivity;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.IntentUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Offer from EventsHigh. Offer is some incentives for taking an actions -- e.g BookMyShow pass
 * for getting referrer install.
 */
public class Offer implements Parcelable {
    public enum OfferType {
        REFERRAL_INSTALL_CONTEST,
        EVENT_CONTEST,
    }

    public final String id;
    public final OfferType type;
    public final Uri imgUrl;
    public final String message;
    public final Date offerEndDate;
    public final int claimCount;
    public final String contestURL;

    public Offer(String id, String type, String imgUrl, String message, Date offerEndDate,
                 int claimCount, String contestURL) throws IllegalArgumentException {
        this.id = checkNotEmptyOrNull(id);
        this.type = OfferType.valueOf(type.toUpperCase());
        this.imgUrl = Uri.parse(checkNotEmptyOrNull(imgUrl));
        this.message = message;
        this.offerEndDate = offerEndDate;
        this.claimCount = claimCount;
        this.contestURL = contestURL;

        if (this.type == OfferType.REFERRAL_INSTALL_CONTEST) {
            checkNotEmptyOrNull(this.message);
        }
        if (this.type == OfferType.EVENT_CONTEST) {
            checkNotEmptyOrNull(this.contestURL);
        }
        if (offerEndDate == null || offerEndDate.getTime() <= 0) {
            throw new IllegalArgumentException("offerEndDate is not valid");
        }
    }

    public boolean isExpired() {
        return offerEndDate.getTime() < System.currentTimeMillis();
    }

    public boolean isGoodToShow() {
        return offerEndDate.getTime() > System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2);
    }

    public void launch(BaseActivity activity) {
        if (isExpired()) {
            activity.reportActionToAnalytics("expiredShowOffer");
            Toast.makeText(activity, R.string.ui_offer_expire, Toast.LENGTH_SHORT).show();
            return;
        }

        activity.reportActionToAnalytics("showOffer", id);
        switch (type) {
            case REFERRAL_INSTALL_CONTEST:
                activity.startActivity(new Intent(activity, OffersActivity.class)
                        .putExtra(OffersActivity.OFFER_EXTRA_PARAM, this));
                break;
            case EVENT_CONTEST:
                IntentUtils.processContestViewIntent(activity, Uri.parse(contestURL), null);
                break;
        }
    }

    public void populateOfferCard(View offerCard, final BaseActivity activity) {
        NetworkImageView imageView = (NetworkImageView) offerCard.findViewById(R.id.image);
        imageView.setImageBitmap(null);
        imageView.setImageUrl(imgUrl.toString(), VolleyHelper.getImageLoader(activity));
        offerCard.findViewById(R.id.expired).setVisibility(isExpired() ? View.VISIBLE : View.GONE);
        offerCard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                launch(activity);
            }
        });
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
        dest.writeString(imgUrl.toString());
        dest.writeString(message);
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


    /**********************************
     JSON Parsing.
     *********************************/
    public static Offer parse(JSONObject offerJSON)
            throws JSONException, IllegalArgumentException, ParseException {
        return new Offer(
                offerJSON.getString("offer_id"),
                offerJSON.getString("offer_type"),
                offerJSON.getString("img_url"),
                offerJSON.optString("offer_detail_message"),
                DateTimeUtils.parseOfferDate(offerJSON.getString("offer_end_date")),
                offerJSON.optInt("claim_count", 0),
                offerJSON.optString("contest_url", null)
            );
    }

    public static List<Offer> parse(JSONArray offersJSONArray)
            throws JSONException, IllegalArgumentException, ParseException {
        List<Offer> offers = new ArrayList<>();
        for (int i = 0; i < offersJSONArray.length(); i++) {
            offers.add(parse(offersJSONArray.getJSONObject(i)));
        }
        return offers;
    }

    private static String checkNotEmptyOrNull(String ref) throws IllegalArgumentException {
        if (ref == null || ref.isEmpty()) {
            throw new IllegalArgumentException("null or empty value");
        }
        return ref;
    }
}
