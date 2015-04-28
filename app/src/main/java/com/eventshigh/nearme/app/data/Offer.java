package com.eventshigh.nearme.app.data;

import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.DateTimeUtils;

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
public abstract class Offer {
    public enum OfferType {
        REFERRAL_INSTALL_CONTEST,
        EVENT_CONTEST,
    }

    public final String id;
    public final Uri imgUrl;
    public final Date offerEndDate;

    public Offer(String id, String imgUrl, Date offerEndDate) throws IllegalArgumentException {
        this.id = checkNotEmptyOrNull(id);
        this.imgUrl = Uri.parse(checkNotEmptyOrNull(imgUrl));
        this.offerEndDate = offerEndDate;

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

    public abstract void launch(BaseActivity activity);

    public void populateOfferCard(View offerCard, final BaseActivity activity) {
        NetworkImageView imageView = (NetworkImageView) offerCard.findViewById(R.id.image);
        imageView.setDefaultImageResId(R.drawable.eh_default_event);
        imageView.setDefaultImageResId(R.drawable.eh_default_event);
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
     JSON Parsing.
     *********************************/
    public static Offer parse(JSONObject offerJSON)
            throws JSONException, IllegalArgumentException, ParseException {
        OfferType type = OfferType.valueOf(offerJSON.getString("offer_type").toUpperCase());

        if (type == OfferType.REFERRAL_INSTALL_CONTEST) {
            return new ReferralInstallOffer(
                    offerJSON.getString("offer_id"),
                    offerJSON.getString("img_url"),
                    DateTimeUtils.parseOfferDate(offerJSON.getString("offer_end_date")),
                    offerJSON.optInt("claim_count", 0),
                    offerJSON.getString("offer_detail_message"),
                    offerJSON.optString("share_message"));
        } else {
            return new EventContestOffer(
                    offerJSON.getString("offer_id"),
                    offerJSON.getString("img_url"),
                    DateTimeUtils.parseOfferDate(offerJSON.getString("offer_end_date")),
                    offerJSON.getString("contest_url"));
        }
    }

    public static List<Offer> parse(JSONArray offersJSONArray)
            throws JSONException, IllegalArgumentException, ParseException {
        List<Offer> offers = new ArrayList<>();
        for (int i = 0; i < offersJSONArray.length(); i++) {
            offers.add(parse(offersJSONArray.getJSONObject(i)));
        }
        return offers;
    }

    public static String checkNotEmptyOrNull(String ref) throws IllegalArgumentException {
        if (ref == null || ref.isEmpty()) {
            throw new IllegalArgumentException("null or empty value");
        }
        return ref;
    }
}
