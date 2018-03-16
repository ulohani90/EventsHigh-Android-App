package com.eventshigh.nearme.app.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventInfoObject;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.network.RequestToCallApi;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.user.UserActionHelper;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.utils.ZendeskUtils;
import com.eventshigh.nearme.app.view.ZCustomFlowLayout;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.zendesk.sdk.feedback.ui.ContactZendeskActivity;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.crypto.AEADBadTagException;

/**
 * Created by umesh on 17/06/16.
 */
public class EventInfoFragment extends Fragment {

    public static EventInfoFragment newInstance(Bundle bundle) {
        EventInfoFragment fragment = new EventInfoFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    TextView eventName;
    TextView eventOrganizer, eventTime, timeDetails, travelTimeView, alsoOnView, addToCalender;
    HorizontalScrollView futureTimesViewGroup;
    LinearLayout futureTimesView, youtubeFragment;
    WebView descriptionView;
    View timeGroupView, eventTimeFirstView;
    ZCustomFlowLayout performersFlowLayout, venueFlowLayout, categoriesFlowLayout;
    View view;
    View enquiryBtn, callOrganizer, directionLayout, mapDirection;

    TextView eventVenueText;

    TextView eventPrice, eventDiscount;

    private GoogleApiClient client;

    Account account;

    View venueHeader, performersHeader;

    TextView ratingHeader, reviewsCount;

    BaseContextActivity activity;

    LinearLayout eventInfoLayout;

    LinearLayout eventSourceLayout;

    TextView eventSrcText;

    ImageView trustedPartner;

    ProgressDialog progressDialog;

    LinearLayout configLayout;
    RelativeLayout configContainer;

    TextView showAllConfig;

    FrameLayout configsFrame;

    LinearLayout offerMessagesContainerLayout;

    public static final String OBJECT_TYPE = "movie";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (BaseContextActivity) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_event_info_layout, container, false);

        eventName = (TextView) view.findViewById(R.id.event_name);
        eventOrganizer = (TextView) view.findViewById(R.id.event_organizer);
        eventTime = (TextView) view.findViewById(R.id.event_time);
        timeDetails = (TextView) view.findViewById(R.id.event_time_details);

        alsoOnView = (TextView) view.findViewById(R.id.also_on);
        futureTimesViewGroup = (HorizontalScrollView) view.findViewById(R.id.event_future_times_hs);
        futureTimesView = (LinearLayout) view.findViewById(R.id.event_future_times);
        descriptionView = (WebView) view.findViewById(R.id.event_description);
        youtubeFragment = (LinearLayout) view.findViewById(R.id.youtube_fragment);
        addToCalender = (TextView) view.findViewById(R.id.btn_add_calender);
        travelTimeView = (TextView) view.findViewById(R.id.event_travel_time);
        timeGroupView = view.findViewById(R.id.event_time_group);
        eventTimeFirstView = view.findViewById(R.id.event_time_first);
        categoriesFlowLayout = (ZCustomFlowLayout) view.findViewById(R.id.category_flowlayout);
        performersFlowLayout = (ZCustomFlowLayout) view.findViewById(R.id.performer_flowlayout);
        venueFlowLayout = (ZCustomFlowLayout) view.findViewById(R.id.venue_flowlayout);
        venueHeader = view.findViewById(R.id.venue_header);
        performersHeader = view.findViewById(R.id.performers_header);

        enquiryBtn = view.findViewById(R.id.btn_enquiry);
        callOrganizer = view.findViewById(R.id.btn_call_organizer);
        eventVenueText = (TextView) view.findViewById(R.id.event_venue_text);
        directionLayout = view.findViewById(R.id.direction_layout);
        mapDirection = view.findViewById(R.id.btn_direction);
        ratingHeader = (TextView) view.findViewById(R.id.rating_header);
        reviewsCount = (TextView) view.findViewById(R.id.reviews_count);

        eventInfoLayout = (LinearLayout) view.findViewById(R.id.event_info_layout);
        trustedPartner = (ImageView) view.findViewById(R.id.trusted_partner);
        eventSourceLayout = (LinearLayout) view.findViewById(R.id.event_source_layout);
        eventSrcText = (TextView) view.findViewById(R.id.source_link);
        eventPrice = (TextView) view.findViewById(R.id.event_price);
        eventDiscount = (TextView) view.findViewById(R.id.event_discount);
        configLayout = (LinearLayout) view.findViewById(R.id.config_layout);
        configContainer = (RelativeLayout) view.findViewById(R.id.config_container);
        showAllConfig = (TextView) view.findViewById(R.id.show_more_config);
        configsFrame = (FrameLayout) view.findViewById(R.id.configs_frame);
        offerMessagesContainerLayout = (LinearLayout) view.findViewById(R.id.offer_messages_container_layout);
        return view;
    }

    public void updateReview(final EventInfoObject event) {
        if (event.reviewObjects.size() > 0) {
            view.findViewById(R.id.review_card).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.tv_user_review_by)).setText(event.reviewObjects.get(0).getReviewBy());
            //Changing reviewers name clickable if profile_id available


            ((RatingBar) view.findViewById(R.id.rb_user_review_rating)).setRating(event.reviewObjects.get(0).getReviewRating());
            ((TextView) view.findViewById(R.id.tv_user_review_text)).setText(event.reviewObjects.get(0).getReviewText());
            if ((event.reviewObjects.get(0).getReviewedEntityId() == null || !event.reviewObjects.get(0).getReviewedEntityId().equalsIgnoreCase(event.id)) && event.reviewObjects.get(0).getReviewEntity() != null) {
                ((TextView) view.findViewById(R.id.tv_user_review_for)).setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.tv_user_review_for)).setText("This review was for " + event.reviewObjects.get(0).getReviewEntity());
            } else {
                ((TextView) view.findViewById(R.id.tv_user_review_for)).setVisibility(View.GONE);
            }
            ImageView reviewerImage = (ImageView) view.findViewById(R.id.civ_user_review);
            int size = reviewerImage.getLayoutParams().height;
            reviewerImage.setImageDrawable(UserContact.getDrawableForName(event.reviewObjects.get(0).getReviewBy(), size));

            if (event.reviewObjects.get(0).getReviewerId() != null
                    && Utils.isValidEmail(event.reviewObjects.get(0).getReviewerId())
                    && !(event.reviewObjects.get(0).getReviewPlatform().equalsIgnoreCase("web"))) {
                (view.findViewById(R.id.tv_user_review_by)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(activity, UserProfileActivity.class);
                        intent.putExtra(UserProfileActivity.PROFILE_ID, event.reviewObjects.get(0).getReviewerId());
                        startActivity(intent);
                    }
                });

            }

            /*
            Glide.with(this).load("url")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                    .into((CircularImageView)findViewById(R.id.civ_user_review));
            */
            //  findViewById(R.id.ll_event_write_review).setVisibility(View.GONE);
            Preferences.getInstance(getActivity()).setIsReviewAdded(false);

            view.invalidate();
        }

    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null)
            account = new Account(getActivity());

        final EventInfoObject event = getArguments().getParcelable("event_info_object");
        eventName.setText(event.title);
        String priceString = event.getPriceString();
        if (priceString == null) {
            eventPrice.setVisibility(View.GONE);
        } else {
            eventPrice.setVisibility(View.VISIBLE);
            eventPrice.setText(priceString);
        }
        if (event.discountPercentageText != null) {
            eventDiscount.setVisibility(View.VISIBLE);
            eventDiscount.setText(event.discountPercentageText);
        } else if (event.discountPercentage != null) {
            eventDiscount.setVisibility(View.VISIBLE);
            eventDiscount.setText(event.discountPercentage + "% OFF");
        } else {
            eventDiscount.setVisibility(View.GONE);
        }


        //Add Offer Messages if any
        if (getActivity() != null) {
            if (event.offerMessages != null && event.offerMessages.size() > 0) {
                for (int i = 0; i < event.offerMessages.size(); i++) {
                    View couponView = getActivity().getLayoutInflater().inflate(R.layout.offer_message_text_view, offerMessagesContainerLayout, false);
                    TextView couponMessage = (TextView) couponView.findViewById(R.id.offer_message);
                    couponMessage.setText(Html.fromHtml(event.offerMessages.get(i)));
                    if (i == event.offerMessages.size() - 1) {
                        couponView.findViewById(R.id.separator).setVisibility(View.GONE);
                    } else {
                        couponView.findViewById(R.id.separator).setVisibility(View.GONE);
                    }

                    offerMessagesContainerLayout.addView(couponView);

                }
            }


            if (event.config != null && event.config.length() > 0) {
                configLayout.setVisibility(View.VISIBLE);

                ((NewEventDetailActivity) getActivity()).configLayout.setVisibility(View.VISIBLE);
                LinkedTreeMap<String, Object> config = new Gson().fromJson(event.config, new TypeToken<LinkedTreeMap<String, Object>>() {
                }.getType());
                if (!((NewEventDetailActivity) getActivity()).addConfigsData(config)) {
                    ((NewEventDetailActivity) getActivity()).configParentLayout.setVisibility(View.GONE);
                    configLayout.setVisibility(View.GONE);
                } else {
                    int rowsAdded = setUpConfigContainer(config);
                    if (rowsAdded > 2) {
                        showAllConfig.setVisibility(View.VISIBLE);
                    } else {
                        if (rowsAdded == 1 || rowsAdded == 2) {
                            configsFrame.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    (int) ((rowsAdded + 1) * TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics()))));
                        }
                        showAllConfig.setVisibility(View.GONE);
                    }
                }

                showAllConfig.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ((NewEventDetailActivity) getActivity()).showHideConfigsLayout();
                    }
                });
            } else {
                ((NewEventDetailActivity) getActivity()).configParentLayout.setVisibility(View.GONE);
                configLayout.setVisibility(View.GONE);
            }

        }
        if (event.organizerName != null) {
            eventOrganizer.setText("By " + event.organizerName);
        }
        DateTimeUtils.EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
        timeGroupView.setVisibility(eventTime == null ? View.GONE : View.VISIBLE);
        if (!event.description.isEmpty()) {
            CustomUrlActivity.setupWebView(descriptionView, (BaseContextActivity) getActivity(), false);
            descriptionView.loadData(Utils.changedHeaderHtml(event.description), "text/html; charset=UTF-8", null);
        }
        if (eventTime != null) {
            this.eventTime.setText(eventTime.toString());
            int numDays = DateTimeUtils.getDaysLater(event);
            if (numDays >= 0) {
                timeDetails.setVisibility(View.VISIBLE);
                timeDetails.setText(MessageFormat.format(
                        getActivity().getString(R.string.event_time_details), numDays));
            } else {
                timeDetails.setVisibility(View.GONE);
            }

            futureTimesView.removeAllViews();
            if (event.eventTimings.size() > 1) {
                for (int i = 1; i < event.eventTimings.size(); i++) {
                    eventTime = DateTimeUtils.getEventTime(event, i);
                    if (eventTime == null) {
                        break;
                    }

                    final Date eventDateCurr = new Date(event.eventTimings.get(i));
                    View timeView = LayoutInflater.from(getActivity()).inflate(
                            R.layout.view_event_time, futureTimesView, false);
                    ((TextView) timeView.findViewById(R.id.event_day)).setText(
                            eventTime.day + ", " + eventTime.date);
                    ((TextView) timeView.findViewById(R.id.event_time)).setText(eventTime.time);
                    futureTimesView.addView(timeView);
                    timeView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((BaseContextActivity) getActivity()).addToCalendar(event, eventDateCurr);
                        }
                    });
                }

                alsoOnView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (futureTimesViewGroup.getVisibility() == View.GONE) {
                            futureTimesViewGroup.setVisibility(View.VISIBLE);
                            eventTimeFirstView.setVisibility(View.GONE);
                            addToCalender.setVisibility(View.GONE);
                        } else {
                            futureTimesViewGroup.setVisibility(View.GONE);
                            eventTimeFirstView.setVisibility(View.VISIBLE);
                            addToCalender.setVisibility(View.VISIBLE);
                        }
                    }
                });
            } else {
                alsoOnView.setVisibility(View.INVISIBLE);
            }
        }
        eventTimeFirstView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save(event);
            }
        });

        StringBuilder builder = new StringBuilder();

        int endIndex = 0;
        if (event.venue != null && event.city != null) {
            if (event.venue.equalsIgnoreCase("Outside " + event.city) && event.destination != null && event.destination.length() > 0) {
                builder.append(event.destination + "\n");
                endIndex = event.destination.length();
            } else {
                builder.append(event.venue + "\n");
                endIndex = event.venue.length();
            }
        }


        /*if (event.venue != null) {
            builder.append(event.venue + "\n");
        }*/
        if (event.address != null) {
            builder.append(event.address);
        } else {
            builder.append(event.getFullAddress());
        }
        SpannableString string = new SpannableString(builder.toString());
        if (event.venue != null) {
            string.setSpan(new ForegroundColorSpan(Color.parseColor("#353535")), 0, endIndex + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            string.setSpan(new RelativeSizeSpan(1.2f), 0, endIndex + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        eventVenueText.setText(string);

        travelTimeView.setVisibility(View.GONE);
        view.findViewById(R.id.direction_separator).setVisibility(View.GONE);
        if (event.performers != null && event.performers.size() > 0) {
            performersHeader.setVisibility(View.VISIBLE);
            performersFlowLayout.setVisibility(View.VISIBLE);
            performersFlowLayout.setRecipientForEventCategories((BaseContextActivity) getActivity(), new Account(getActivity()), new ArrayList<String>(event.performers), event, "performerAsTag");

        } else {
            performersHeader.setVisibility(View.GONE);
            performersFlowLayout.setVisibility(View.GONE);
        }
        if (event.isCleanVenue) {
            venueFlowLayout.setVisibility(View.VISIBLE);
            venueHeader.setVisibility(View.VISIBLE);
            ArrayList<String> venueList = new ArrayList<>();
            venueList.add(event.venue);
            if (event.locality != null)
                venueList.add(event.locality);
            venueFlowLayout.setRecipientForEventCategories((BaseContextActivity) getActivity(), new Account(getActivity()), venueList, event, "venueAsTag");
        } else {
            venueFlowLayout.setVisibility(View.GONE);
            venueHeader.setVisibility(View.GONE);
        }

        categoriesFlowLayout.setRecipientForEventCategories((BaseContextActivity) getActivity(), new Account(getActivity()), event.tags, event, "tagClick");
        addToCalender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save(event);
            }
        });


        if (event.sourceUrl != null && event.sourceUrl.length() > 0) {
            try {
                URL url = new URL(event.sourceUrl);
                String host = url.getHost();
                if (!host.equalsIgnoreCase("www.eventshigh.com")) {
                    eventSourceLayout.setVisibility(View.VISIBLE);
                    SpannableString hostString = new SpannableString(host);
                    hostString.setSpan(new UnderlineSpan(), 0, host.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    eventSrcText.setText(hostString);
                    eventSourceLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((NewEventDetailActivity) getActivity()).openSourceSite(v);
                        }
                    });

                } else {
                    eventSourceLayout.setVisibility(View.GONE);
                }
            } catch (MalformedURLException e) {
                eventSourceLayout.setVisibility(View.GONE);
            }
        } else {
            eventSourceLayout.setVisibility(View.GONE);
        }

        if (event.organizerPhone != null && event.organizerPhone.length() > 0 && !event.skipRequestToCall) {
            enquiryBtn.setVisibility(View.GONE);
            callOrganizer.setVisibility(View.VISIBLE);
            callOrganizer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestCallOrganizer(event, event.organizerPhone);
                }
            });

        } else if (event.skipRequestToCall && event.skipCallbackupPhone != null) {
            enquiryBtn.setVisibility(View.GONE);
            callOrganizer.setVisibility(View.VISIBLE);
            callOrganizer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestCallOrganizer(event, event.skipCallbackupPhone);
                }
            });
        } else {

            if (event.organizerName != null) {
                eventInfoLayout.setVisibility(View.VISIBLE);
            } else {
                eventInfoLayout.setVisibility(View.GONE);
            }

            if (account.getLastCity() != null && account.getLastCity().equals(City.BANGALORE)) {
                enquiryBtn.setVisibility(View.VISIBLE);

                enquiryBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ama(event);
                    }
                });
            } else {
                enquiryBtn.setVisibility(View.GONE);
            }
            callOrganizer.setVisibility(View.GONE);
        }


        /*if (account.getLastCity() != null && account.getLastCity().equals(City.BANGALORE)) {
            enquiryBtn.setVisibility(View.VISIBLE);
            callOrganizer.setVisibility(View.GONE);
            enquiryBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ama(event);
                }
            });
        } else {
            if (event.organizerPhone != null) {
                enquiryBtn.setVisibility(View.GONE);
                callOrganizer.setVisibility(View.VISIBLE);
                callOrganizer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        call(event);
                    }
                });
                eventInfoLayout.setVisibility(View.VISIBLE);
            } else {
                if (event.organizerName != null) {
                    eventInfoLayout.setVisibility(View.VISIBLE);
                } else {
                    eventInfoLayout.setVisibility(View.GONE);
                }
                enquiryBtn.setVisibility(View.GONE);
                callOrganizer.setVisibility(View.GONE);
            }
        }*/

        if (!(string.toString().equalsIgnoreCase("Outside " + event.city)) && event.getMapQuery() != null) {
            mapDirection.setVisibility(View.VISIBLE);
            mapDirection.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDirections(event);
                }
            });
        } else {
            mapDirection.setVisibility(View.GONE);
        }


        if (event.reviewObjects.size() == 0) {
            ratingHeader.setVisibility(View.GONE);
            reviewsCount.setVisibility(View.GONE);
            view.findViewById(R.id.write_review).setVisibility(View.VISIBLE);
            ((LinearLayout) view.findViewById(R.id.no_review_layout)).setVisibility(View.VISIBLE);


            (view.findViewById(R.id.review_layout)).setVisibility(View.GONE);

        } else {
            ratingHeader.setVisibility(View.VISIBLE);
            reviewsCount.setVisibility(View.VISIBLE);
            reviewsCount.setText("( " + event.reviewObjects.size() + " Reviews )");
            ratingHeader.setText(getAverageRating(event));
            if (((NewEventDetailActivity) getActivity()).isMyReviewWritten) {
                view.findViewById(R.id.write_review).setVisibility(View.GONE);
            } else {
                view.findViewById(R.id.write_review).setVisibility(View.VISIBLE);
            }
            (view.findViewById(R.id.review_layout)).setVisibility(View.VISIBLE);
            updateReview(event);
            ((LinearLayout) view.findViewById(R.id.no_review_layout)).setVisibility(View.GONE);
            TextView readAllReviews = (TextView) view.findViewById(R.id.show_more_text);
            if (event.reviewObjects.size() > 1) {
                readAllReviews.setVisibility(View.VISIBLE);
                readAllReviews.setText("Read all reviews ( " + event.reviewObjects.size() + " )");
            } else {
                readAllReviews.setVisibility(View.GONE);
            }

            readAllReviews.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), EventAllReviewsActivity.class);
                    intent.putParcelableArrayListExtra("event_reviews", event.reviewObjects);
                    intent.putExtra("event_id", event.id);
                    startActivity(intent);
                }
            });
        }
        view.findViewById(R.id.write_review).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeReview(event);
            }
        });

        // Connect to Google API client to notify the view.
        getGoogleApiClient(event);
        if (showWriteReview && view.findViewById(R.id.write_review).isShown()) {
            writeReview(event);
            showWriteReview = false;
        }

        if (event.isPrimaryOrganizer) {
            trustedPartner.setVisibility(View.VISIBLE);
        } else {
            trustedPartner.setVisibility(View.GONE);
        }

    }


    LatLng userLocation;

    private void getGoogleApiClient(final EventInfoObject event) {
        if (client != null && client.isConnected()) {
            populateEventTravelTime(event);
            return;
        }

        client = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        populateEventTravelTime(event);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        // do nothing.
                    }
                })
                .build();
        client.connect();
        Uri webUri = event.getEventDetailsURI();
    }

    boolean showWriteReview;

    public void setShowWriteReview(boolean showWriteReview) {
        this.showWriteReview = showWriteReview;
    }

    public void writeReview(EventInfoObject event) {
        if (!account.getUserInfo().isSignedIn) {
            // PhoneVerificationDialog.show(((NewEventDetailActivity) getActivity()), R.string.ui_verify_phone, R.string.ui_phone_verify_book);

            // FBSigninDialog.show(((NewEventDetailActivity) getActivity()), R.string.ui_signin_via_fb, R.string.ui_signin_fb_plan, NewEventDetailActivity.REQUEST_FOR_RESULT_WRITE_REVIEW);

            Intent intent = new Intent(activity, FBLoginActivity.class);
            intent.putExtra("show_special_text", true);
            intent.putExtra("hide_skip", true);
            activity.startActivityForResult(intent, NewEventDetailActivity.REQUEST_FOR_RESULT_WRITE_REVIEW);

            return;
        }
        ((NewEventDetailActivity) getActivity()).reportActionToAnalytics("write_review_btn_click");
        Intent i = new Intent(getActivity(), WriteReviewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(NewEventDetailActivity.EVENT_OBJECT, event);
        bundle.putString(OBJECT_TYPE, "event");
        i.putExtras(bundle);
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.animate_slide_up, R.anim.stay);

    }

    public String getAverageRating(EventInfoObject event) {

        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < event.reviewObjects.size(); i++) {
            try {
                sum += event.reviewObjects.get(i).getReviewRating();
                count++;
            } catch (NumberFormatException e) {

            }
        }
        return Utils.roundToTwoDecimalPlaces(((sum / count))) + "/5.0";


    }

    private void populateEventTravelTime(EventInfoObject event) {
        if (activity != null) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location location = LocationServices.FusedLocationApi.getLastLocation(client);
                if (location != null) {
                    userLocation = LocationUtils.locationToLatLng(location);
                }
                String eventTravelTime = null;
                if (getActivity() != null)
                    if (account.getLastLocality() != null) {
                        eventTravelTime = LocationUtils.getTravelTime(getActivity(),
                                account.getLastLocality().getLatLng(), event.location);
                    } else if (userLocation != null) {
                        eventTravelTime = LocationUtils.getTravelTime(getActivity(),
                                userLocation, event.location);
                    }
                travelTimeView.setVisibility((event.location.latitude == 0 && event.location.longitude == 0) || eventTravelTime == null ? View.GONE : View.VISIBLE);
                view.findViewById(R.id.direction_separator).setVisibility((event.location.latitude == 0 && event.location.longitude == 0) || eventTravelTime == null ? View.GONE : View.VISIBLE);
                if (eventTravelTime != null) {
                    travelTimeView.setText(eventTravelTime);
                }
            }
        }
    }


    public void ama(EventInfoObject event) {

        final Account account = new Account(getActivity());
        Account.UserInfo userInfo = account.getUserInfo();
        if (userInfo.phoneNo == null) {
            PhoneVerificationDialog.show(((NewEventDetailActivity) getActivity()), R.string.ui_verify_phone, R.string.ui_phone_verify_plan);
            return;
        }
        Preferences preferences = Preferences.getInstance(getActivity());


        ((NewEventDetailActivity) getActivity()).reportEventAction(event, "ama");
        ZendeskUtils.initZendesk(getActivity());
        ZendeskUtils.setEventFeedbackConfiguration(getActivity(), event);
        Intent feedbackIntent = new Intent(getActivity(), ContactZendeskActivity.class);
        getActivity().startActivity(feedbackIntent);

    }


    public void requestCallOrganizer(EventInfoObject event, String organizerPhone) {
        final Account account = new Account(getActivity());
        Account.UserInfo userInfo = account.getUserInfo();
        if (userInfo.phoneNo == null) {
            PhoneVerificationDialog.show(((NewEventDetailActivity) getActivity()), R.string.ui_verify_phone, R.string.ui_phone_verify_plan);
            return;
        }

        activity.reportEventAction(event, "enquiry_request_to_call");
        progressDialog = ProgressDialog.show(activity, null, "Submitting request. Please wait..");
        RequestToCallApi.submit(activity, userInfo.phoneNo, organizerPhone, event.id, event.organizerAccountName, Request.Priority.HIGH, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject, boolean b) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                showCallReceivingDialog();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                Toast.makeText(activity, "Some problem occurred. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });


    }

    public void showCallReceivingDialog() {
        new AlertDialog.Builder(activity).setMessage("We will connect you to the organizer shortly").setTitle("Request Received").setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();

    }

    public void save(EventInfoObject event) {
        ((NewEventDetailActivity) getActivity()).showRateAppDialog = true;
        ((NewEventDetailActivity) getActivity()).addToFavourite = true;
        ((NewEventDetailActivity) getActivity()).reportEventAction(event, "addToCalendar");
        new UserActionHelper(getActivity()).recordAction(UserActionHelper.EventAction.SAVE, event.id);
        ((NewEventDetailActivity) getActivity()).addToCalendar(event, null);
    }


    public void call(Event event) {
        if (event.organizerPhone == null) {
            return;
        }
        Account account = new Account(getActivity());
        Account.UserInfo userInfo = account.getUserInfo();
        if (!userInfo.isSignedIn) {

            //  FBSigninDialog.show(((NewEventDetailActivity) getActivity()), R.string.ui_signin_via_fb, R.string.ui_signin_fb_plan, NewEventDetailActivity.REQUEST_FOR_RESULT_CALL_EVENT);
            Intent intent = new Intent(activity, FBLoginActivity.class);
            intent.putExtra("show_special_text", true);
            intent.putExtra("hide_skip", true);
            activity.startActivityForResult(intent, NewEventDetailActivity.REQUEST_FOR_RESULT_CALL_EVENT);
            return;
        }

        ((NewEventDetailActivity) getActivity()).showInviteDialog = true;
        ((NewEventDetailActivity) getActivity()).addToFavourite = true;
        ((NewEventDetailActivity) getActivity()).reportEventAction(event, "organizer", "call");
        new UserActionHelper(getActivity()).recordAction(UserActionHelper.EventAction.CALL, event.id);

        Intent intent = new Intent(Intent.ACTION_DIAL)
                .setData(Uri.parse("tel:" + (event.organizerPhone.split(",")[0])));
        ((NewEventDetailActivity) getActivity()).startActivitySafe(intent);
    }

    public void showDirections(EventInfoObject event) {
        ((NewEventDetailActivity) getActivity()).addToFavourite = true;
        ((NewEventDetailActivity) getActivity()).reportEventAction(event, "showDirections");

        Intent intent = event.getShowDirectionsOnMapIntent(activity);
        if (intent == null) {
            ((NewEventDetailActivity) getActivity()).reportActionToAnalytics("skipDirectionsNoLocation");
            Toast.makeText(getActivity(), getString(R.string.failed_event_location), Toast.LENGTH_SHORT).show();
            //((NewEventDetailActivity) getActivity()).showMessage(R.string.failed_event_location);
            return;
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open maps.
            Crashlytics.getInstance().core.logException(e);
            Toast.makeText(getActivity(), getString(R.string.no_map_app), Toast.LENGTH_SHORT).show();
            //  ((NewEventDetailActivity) getActivity()).showMessage(R.string.no_map_app);
        }
    }

    private static String toHtmlNoFrame(String html) {
        return "<body>" + html.replaceAll("<iframe.*/iframe>", "") + "</body>";
    }

    public String getCommaSeparatedString(ArrayList<String> items) {
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(item);
        }
        return builder.toString();
    }


    @Override
    public void onStop() {
        super.onStop();
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
    }

    public int setUpConfigContainer(LinkedTreeMap<String, Object> configMap) {

        int rowsMade = 1;

        //  view.findViewById(R.id.header).getLayoutParams().height = 0;
        view.findViewById(R.id.close).setVisibility(View.INVISIBLE);

        if (checkIfKeyHasValue("special_highlights", configMap) || checkIfKeyHasValue("artists_performing", configMap)
        || checkIfKeyHasValue("activities_type", configMap)) {
            view.findViewById(R.id.highlights_layout).setVisibility(View.VISIBLE);
            view.findViewById(R.id.highlights_border).setVisibility(View.VISIBLE);
            if (checkIfKeyHasValue("special_highlights", configMap)) {
                ((TextView)view.findViewById(R.id.highlights_title)).setText("Highlights");
                LinearLayout highlightsContainer = (LinearLayout) view.findViewById(R.id.highlights_container);
                highlightsContainer.setVisibility(View.VISIBLE);
                view.findViewById(R.id.highlights_title).setVisibility(View.VISIBLE);
                String[] value;
                if (((LinkedTreeMap<String, Object>) configMap.get("special_highlights")).get("value") instanceof String) {
                    value = ((String) ((LinkedTreeMap<String, Object>) configMap.get("special_highlights")).get("value")).split(",");
                } else {
                    value = (String[]) ((ArrayList) ((LinkedTreeMap<String, Object>) configMap.get("special_highlights")).get("value")).toArray();
                }
                for (int i = 0; i < value.length; i++) {

                    View highlightView = getActivity().getLayoutInflater().inflate(R.layout.textview_layout, highlightsContainer, false);
                    TextView textView = (TextView) highlightView.findViewById(R.id.textview_text);
                    textView.setText("\u2022 " + value[i].trim());
                    rowsMade += 1;
                    highlightsContainer.addView(highlightView);
                }
            }else if(checkIfKeyHasValue("activities_type", configMap)){
                ((TextView)view.findViewById(R.id.highlights_title)).setText("Activities Include");
                LinearLayout highlightsContainer = (LinearLayout) view.findViewById(R.id.highlights_container);
                highlightsContainer.setVisibility(View.VISIBLE);
                view.findViewById(R.id.highlights_title).setVisibility(View.VISIBLE);
                String[] value;
                if (((LinkedTreeMap<String, Object>) configMap.get("activities_type")).get("value") instanceof String) {
                    value = ((String) ((LinkedTreeMap<String, Object>) configMap.get("activities_type")).get("value")).split(",");
                } else {
                    value = (String[]) ((ArrayList) ((LinkedTreeMap<String, Object>) configMap.get("activities_type")).get("value")).toArray();
                }
                for (int i = 0; i < value.length; i++) {
                    View highlightView = getLayoutInflater().inflate(R.layout.textview_layout, highlightsContainer, false);
                    TextView textView = (TextView) highlightView.findViewById(R.id.textview_text);
                    rowsMade += 1;
                    textView.setText("\u2022 " + value[i].trim());
                    highlightsContainer.addView(highlightView);
                }
            } else {
                view.findViewById(R.id.highlights_container).setVisibility(View.GONE);
                view.findViewById(R.id.highlights_title).setVisibility(View.GONE);
            }

            if (checkIfKeyHasValue("artists_performing", configMap)) {
                LinearLayout artistsContainer = (LinearLayout) view.findViewById(R.id.artists_container);
                artistsContainer.setVisibility(View.VISIBLE);
                view.findViewById(R.id.artists_title).setVisibility(View.VISIBLE);
                String[] value;
                if (((LinkedTreeMap<String, Object>) configMap.get("artists_performing")).get("value") instanceof String) {
                    value = ((String) ((LinkedTreeMap<String, Object>) configMap.get("artists_performing")).get("value")).split(",");
                } else {
                    value = (String[]) ((ArrayList) ((LinkedTreeMap<String, Object>) configMap.get("artists_performing")).get("value")).toArray();
                }
                for (int i = 0; i < value.length; i++) {
                    View highlightView = getActivity().getLayoutInflater().inflate(R.layout.textview_layout, artistsContainer, false);
                    TextView textView = (TextView) highlightView.findViewById(R.id.textview_text);
                    textView.setText("\u2022 " + value[i].trim());
                    rowsMade += 1;
                    artistsContainer.addView(highlightView);
                }
            } else {
                view.findViewById(R.id.artists_container).setVisibility(View.GONE);
                view.findViewById(R.id.artists_title).setVisibility(View.GONE);
            }


        } else {

            view.findViewById(R.id.highlights_layout).setVisibility(View.GONE);
        }
        //Add party

        if (checkIfParentChildKeyHasValue("is_parties_and_nightlife", "party_venue_type", configMap)
                || checkIfParentChildKeyHasValue("is_parties_and_nightlife", "venue_view", configMap)
                || checkIfKeyHasValue("is_unlimited_food", configMap)
                || checkIfKeyHasValue("is_unlimited_alcohol", configMap)
                || checkIfParentChildKeyHasValue("is_parties_and_nightlife", "is_stags_allowed", configMap)
                || checkIfParentChildKeyHasValue("is_parties_and_nightlife", "is_parking_available", configMap)
                || checkIfKeyHasValue("is_group_discounts", configMap)
                || checkIfKeyHasValue("is_live_dhol", configMap)
                || checkIfKeyHasValue("is_rain_dance", configMap)) {
            LinearLayout partyLayout = (LinearLayout) view.findViewById(R.id.party_info_layout);
            partyLayout.setVisibility(View.VISIBLE);
            if (view.findViewById(R.id.highlights_layout).isShown())
                view.findViewById(R.id.highlights_border).setVisibility(View.VISIBLE);
            else
                view.findViewById(R.id.highlights_border).setVisibility(View.GONE);

            int partyLayoutCount = 1;
            int childCount = 0;
            if (checkIfParentChildKeyHasValue("is_parties_and_nightlife", "party_venue_type", configMap)) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                    rowsMade += 1;
                } else {
                    childCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("party_venue_type")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("party_venue_type")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("party_venue_type")).get("value"));
                }

                addPartyVenue(partyLayoutCount, childCount, view, "Venue Type", value, "party_info_layout_", "party_info_textview_");

            }
            if (checkIfParentChildKeyHasValue("is_parties_and_nightlife", "venue_view", configMap)) {
                if (childCount == 0) {
                    childCount += 1;
                    rowsMade += 1;
                } else if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                    rowsMade += 1;
                } else {
                    childCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("venue_view")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("venue_view")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("venue_view")).get("value"));
                }
                rowsMade += 1;
                addPartyVenue(partyLayoutCount, childCount, view, "Venue View", value, "party_info_layout_", "party_info_textview_");
            }
            if (checkIfKeyHasValue("is_unlimited_food", configMap)) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                    rowsMade += 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_unlimited_food")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                addPartyVenue(partyLayoutCount, childCount, view, "Unlimited Food Available", finalValue, "party_info_layout_", "party_info_textview_");
            }
            if (checkIfKeyHasValue("is_unlimited_alcohol", configMap)) {
                if (childCount == 0) {
                    childCount += 1;
                    rowsMade += 1;
                } else if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                    rowsMade += 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_unlimited_alcohol")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                addPartyVenue(partyLayoutCount, childCount, view, "Unlimited Alcohol Available", finalValue, "party_info_layout_", "party_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_parties_and_nightlife", "is_stags_allowed", configMap)) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                    rowsMade += 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("is_stags_allowed")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                addPartyVenue(partyLayoutCount, childCount, view, "Stag Entry Allowed", finalValue, "party_info_layout_", "party_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_parties_and_nightlife", "is_parking_available", configMap)) {
                if (childCount == 0) {
                    childCount += 1;
                    rowsMade += 1;
                } else if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                    rowsMade += 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_parties_and_nightlife")).get("is_parking_available")).get("value");

                addPartyVenue(partyLayoutCount, childCount, view, "Parking Available", value, "party_info_layout_", "party_info_textview_");
            }
            if (checkIfKeyHasValue("is_group_discounts", configMap)) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                    rowsMade += 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_group_discounts")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                addPartyVenue(partyLayoutCount, childCount, view, "Group Discounts", finalValue, "party_info_layout_", "party_info_textview_");
            }
            if (checkIfKeyHasValue("is_live_dhol", configMap)) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_live_dhol")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);

                addPartyVenue(partyLayoutCount, childCount,view, "Live Dhol", finalValue, "party_info_layout_", "party_info_textview_");
            }
            if (checkIfKeyHasValue("is_rain_dance", configMap)) {
                if (childCount == 2) {
                    partyLayoutCount += 1;
                    childCount = 1;
                } else {
                    childCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_rain_dance")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);

                addPartyVenue(partyLayoutCount, childCount,view, "Rain Dance", finalValue, "party_info_layout_", "party_info_textview_");
            }

        } else {
            view.findViewById(R.id.party_info_layout).setVisibility(View.GONE);
            view.findViewById(R.id.highlights_border).setVisibility(View.GONE);
        }
        //Add Outdoors
        boolean showOutdoors = false;
        if (configMap.containsKey("is_outdoors") && ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).containsKey("value")) {
            String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("value");
            showOutdoors = value.equalsIgnoreCase("true") ? true : false;
        }

        if (showOutdoors && (checkIfParentChildKeyHasValue("is_outdoors", "is_transportation_available", configMap)
                || checkIfParentChildKeyHasValue("is_outdoors", "is_alcohol_allowed", configMap)
                || checkIfParentChildKeyHasValue("is_outdoors", "tour_duration", configMap)
                || checkIfParentChildKeyHasValue("is_outdoors", "outdoor_venue_type", configMap)
                || checkIfParentChildKeyHasValue("is_outdoors", "stay_type", configMap)
                || checkIfParentChildKeyHasValue("is_outdoors", "tent_sharing_type", configMap)
                || checkIfParentChildKeyHasValue("is_outdoors", "food_type", configMap)
                || checkIfParentChildKeyHasValue("is_outdoors", "activity_type", configMap))) {
            view.findViewById(R.id.outdoor_info_layout).setVisibility(View.VISIBLE);
            if (view.findViewById(R.id.party_info_layout).isShown()) {
                view.findViewById(R.id.party_border).setVisibility(View.VISIBLE);
            } else {
                view.findViewById(R.id.party_border).setVisibility(View.GONE);
            }
            int outdoorLayoutCount = 1;
            int outdoorChildCount = 0;
            if (checkIfParentChildKeyHasValue("is_outdoors", "is_transportation_available", configMap)) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                    rowsMade += 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("is_transportation_available")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Transportation Available", finalValue, "outdoor_info_layout_", "outdoor_info_textview_");

            }

            if (checkIfParentChildKeyHasValue("is_outdoors", "is_alcohol_allowed", configMap)) {
                if (outdoorChildCount == 0) {
                    rowsMade += 1;
                    outdoorChildCount += 1;
                } else if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                    rowsMade += 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("is_alcohol_allowed")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Alcohol Allowed", finalValue, "outdoor_info_layout_", "outdoor_info_textview_");

            }

            if (checkIfParentChildKeyHasValue("is_outdoors", "tour_duration", configMap)) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                    rowsMade += 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("tour_duration")).get("value");

                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Duration", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }
            if (checkIfParentChildKeyHasValue("is_outdoors", "outdoor_venue_type", configMap)) {
                if (outdoorChildCount == 0) {
                    rowsMade += 1;
                    outdoorChildCount += 1;
                } else if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                    rowsMade += 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("outdoor_venue_type")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("outdoor_venue_type")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("outdoor_venue_type")).get("value"));
                }

                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Venue type", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }
            if (checkIfParentChildKeyHasValue("is_outdoors", "stay_type", configMap)) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                    rowsMade += 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("stay_type")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("stay_type")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("stay_type")).get("value"));
                }
                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Stay type", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }

            if (checkIfParentChildKeyHasValue("is_outdoors", "tent_sharing_type", configMap)) {
                if (outdoorChildCount == 0) {
                    rowsMade += 1;
                    outdoorChildCount += 1;
                } else if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                    rowsMade += 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("tent_sharing_type")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("tent_sharing_type")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("tent_sharing_type")).get("value"));
                }
                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Tent sharing type", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }
            if (checkIfParentChildKeyHasValue("is_outdoors", "food_type", configMap)) {
                if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                    rowsMade += 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("food_type")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("food_type")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("food_type")).get("value"));
                }
                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Food type", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }
            if (checkIfParentChildKeyHasValue("is_outdoors", "activity_type", configMap)) {
                if (outdoorChildCount == 0) {
                    rowsMade += 1;
                    outdoorChildCount += 1;
                } else if (outdoorChildCount == 2) {
                    outdoorLayoutCount += 1;
                    outdoorChildCount = 1;
                    rowsMade += 1;
                } else {
                    outdoorChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("activity_type")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("activity_type")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_outdoors")).get("activity_type")).get("value"));
                }
                addPartyVenue(outdoorLayoutCount, outdoorChildCount, view, "Activity type", value, "outdoor_info_layout_", "outdoor_info_textview_");

            }

        } else {
            view.findViewById(R.id.outdoor_info_layout).setVisibility(View.GONE);
            view.findViewById(R.id.party_border).setVisibility(View.GONE);
        }

        //Add Kid Friendly
        boolean showKidFriendly = false;
        if (configMap.containsKey("is_kid_friendly") && ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).containsKey("value")) {
            String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("value");
            showKidFriendly = value.equalsIgnoreCase("true") ? true : false;
        }
        if (showKidFriendly && (checkIfKeyHasValue("is_kid_friendly", configMap)
                || checkIfParentChildKeyHasValue("is_kid_friendly", "is_free_for_kids_below_five", configMap)
                || checkIfParentChildKeyHasValue("is_kid_friendly", "is_child_care_zone", configMap)
                || checkIfParentChildKeyHasValue("is_kid_friendly", "max_age_kids_pricing", configMap)
                || checkIfParentChildKeyHasValue("is_kid_friendly", "kid_activities", configMap))) {


            view.findViewById(R.id.kids_info_layout).setVisibility(View.VISIBLE);
            if (view.findViewById(R.id.outdoor_info_layout).isShown())
                view.findViewById(R.id.outdoor_border).setVisibility(View.VISIBLE);
            else
                view.findViewById(R.id.outdoor_border).setVisibility(View.GONE);

            int kidsLayoutCount = 1;
            int kidsChildCount = 0;
            if (checkIfKeyHasValue("is_kid_friendly", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                    rowsMade += 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Kids Friendly", finalValue, "kids_info_layout_", "kids_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_kid_friendly", "is_free_for_kids_below_five", configMap)) {
                if (kidsChildCount == 0) {
                    rowsMade += 1;
                    kidsChildCount += 1;
                } else if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                    rowsMade += 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("is_free_for_kids_below_five")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Free for kids below 5 years", finalValue, "kids_info_layout_", "kids_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_kid_friendly", "is_child_care_zone", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                    rowsMade += 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("is_child_care_zone")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Child care zone available", finalValue, "kids_info_layout_", "kids_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_kid_friendly", "max_age_kids_pricing", configMap)) {
                if (kidsChildCount == 0) {
                    rowsMade += 1;
                    kidsChildCount += 1;
                } else if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                    rowsMade += 1;
                } else {
                    kidsChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("max_age_kids_pricing")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("max_age_kids_pricing")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("max_age_kids_pricing")).get("value"));
                }
                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Max age for kids pricing ", value, "kids_info_layout_", "kids_info_textview_");
            }

            if (checkIfParentChildKeyHasValue("is_kid_friendly", "kid_activities", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                    rowsMade += 1;
                } else {
                    kidsChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("kid_activities")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("kid_activities")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_kid_friendly")).get("kid_activities")).get("value"));
                }

                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Activities for Kids", value, "kids_info_layout_", "kids_info_textview_");
            }

        } else {
            view.findViewById(R.id.kids_info_layout).setVisibility(View.GONE);
            view.findViewById(R.id.outdoor_border).setVisibility(View.GONE);
        }

        //Add Stay Type
        boolean showStayProvided = false;
        if (configMap.containsKey("is_stay_provided") && ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).containsKey("value")) {
            String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("value");
            showStayProvided = value.equalsIgnoreCase("true") ? true : false;
        }
        if (showStayProvided && (checkIfKeyHasValue("is_stay_provided", configMap)
                || checkIfParentChildKeyHasValue("is_stay_provided", "is_breakfast_included", configMap)
                || checkIfParentChildKeyHasValue("is_stay_provided", "is_extra_bed_available", configMap)
                || checkIfParentChildKeyHasValue("is_stay_provided", "check_in_time", configMap)
                || checkIfParentChildKeyHasValue("is_stay_provided", "check_out_time", configMap))) {
            view.findViewById(R.id.stay_info_layout).setVisibility(View.VISIBLE);
            if (view.findViewById(R.id.kids_info_layout).isShown())
                view.findViewById(R.id.kids_border).setVisibility(View.VISIBLE);
            else
                view.findViewById(R.id.kids_border).setVisibility(View.GONE);

            int kidsLayoutCount = 1;
            int kidsChildCount = 0;
            if (checkIfKeyHasValue("is_stay_provided", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                    rowsMade += 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("value");
                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Stay Provided", finalValue, "stay_info_layout_", "stay_info_textview_");
            }
            if (checkIfParentChildKeyHasValue("is_stay_provided", "is_breakfast_included", configMap)) {
                if (kidsChildCount == 0) {
                    rowsMade += 1;
                    kidsChildCount += 1;
                } else if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                    rowsMade += 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("is_breakfast_included")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Breakfast included", finalValue, "stay_info_layout_", "stay_info_textview_");
            }
            if (checkIfParentChildKeyHasValue("is_stay_provided", "is_extra_bed_available", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                    rowsMade += 1;
                } else {
                    kidsChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("is_extra_bed_available")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);
                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Extra Bed Available", finalValue, "stay_info_layout_", "stay_info_textview_");
            }
            if (checkIfParentChildKeyHasValue("is_stay_provided", "check_in_time", configMap)) {
                if (kidsChildCount == 0) {
                    rowsMade += 1;
                    kidsChildCount += 1;
                } else if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                    rowsMade += 1;
                } else {
                    kidsChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("check_in_time")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("check_in_time")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("check_in_time")).get("value"));
                }
                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Check in time", value, "stay_info_layout_", "stay_info_textview_");
            }
            if (checkIfParentChildKeyHasValue("is_stay_provided", "check_out_time", configMap)) {
                if (kidsChildCount == 2) {
                    kidsLayoutCount += 1;
                    kidsChildCount = 1;
                    rowsMade += 1;
                } else {
                    kidsChildCount += 1;
                }
                String value;
                if (((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("check_out_time")).get("value") instanceof String) {
                    value = (String) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("check_out_time")).get("value");
                } else {
                    value = getCommaSeparatedString((ArrayList<String>) ((LinkedTreeMap<String, Object>) ((LinkedTreeMap<String, Object>) configMap.get("is_stay_provided")).get("check_out_time")).get("value"));
                }
                addPartyVenue(kidsLayoutCount, kidsChildCount, view, "Check out time", value, "stay_info_layout_", "stay_info_textview_");
            }

        } else {
            view.findViewById(R.id.stay_info_layout).setVisibility(View.GONE);
            view.findViewById(R.id.kids_border).setVisibility(View.GONE);
        }

        if(checkIfKeyHasValue("is_transport_available",configMap) || checkIfKeyHasValue("min_age",configMap)
                || checkIfKeyHasValue("max_age",configMap)){
            view.findViewById(R.id.summer_camp_info_layout).setVisibility(View.VISIBLE);
            if ((view.findViewById(R.id.stay_info_layout)).isShown())
                view.findViewById(R.id.stay_border).setVisibility(View.VISIBLE);
            else
                view.findViewById(R.id.stay_border).setVisibility(View.GONE);

            int summerCampLayoutCount = 1;
            int summerCampChildCount = 0;
            if (checkIfKeyHasValue("min_age", configMap)) {
                if (summerCampChildCount == 0) {
                    rowsMade += 1;
                    summerCampChildCount += 1;
                } else if (summerCampChildCount == 2) {
                    summerCampLayoutCount += 1;
                    summerCampChildCount = 1;
                    rowsMade += 1;
                } else {
                    summerCampChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("min_age")).get("value") +" yrs";

                addPartyVenue(summerCampLayoutCount, summerCampChildCount,view, "Min Age", value, "summer_camp_info_layout_", "summer_camp_info_textview_");
            }

            if (checkIfKeyHasValue("max_age", configMap)) {
                if (summerCampChildCount == 2) {
                    summerCampLayoutCount += 1;
                    summerCampChildCount = 1;
                    rowsMade += 1;
                } else {
                    summerCampChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("max_age")).get("value") +" yrs";


                addPartyVenue(summerCampLayoutCount, summerCampChildCount,view, "Max Age", value, "summer_camp_info_layout_", "summer_camp_info_textview_");
            }

            if (checkIfKeyHasValue("is_transport_available",  configMap)) {
                if (summerCampChildCount == 0) {
                    rowsMade += 1;
                    summerCampChildCount += 1;
                } else if (summerCampChildCount == 2) {
                    summerCampLayoutCount += 1;
                    summerCampChildCount = 1;
                    rowsMade += 1;
                } else {
                    summerCampChildCount += 1;
                }
                String value = (String) ((LinkedTreeMap<String, Object>) configMap.get("is_transport_available")).get("value");

                String finalValue = (value.equalsIgnoreCase("Yes") ||
                        value.equalsIgnoreCase("true")) ? "Yes" :
                        ((value.equalsIgnoreCase("No") ||
                                value.equalsIgnoreCase("false")) ? "No" : value);

                addPartyVenue(summerCampLayoutCount, summerCampChildCount,view, "Transportation Available", finalValue, "summer_camp_info_layout_", "summer_camp_info_textview_");
            }

        }else{
            view.findViewById(R.id.summer_camp_info_layout).setVisibility(View.GONE);
            view.findViewById(R.id.stay_border).setVisibility(View.GONE);
        }



        return rowsMade;
    }

    public void addPartyVenue(int layoutCount, int childCount, View view, String keyName, String value, String layoutName, String textViewName) {
        try {
            int layoutKey = R.id.class.getField(layoutName + layoutCount).getInt(null);
            int textViewKey = R.id.class.getField(textViewName + layoutCount + childCount).getInt(null);
            view.findViewById(layoutKey).setVisibility(View.VISIBLE);
            TextView textView = (TextView) view.findViewById(textViewKey);
            textView.setVisibility(View.VISIBLE);
            SpannableString string = new SpannableString(keyName + " : " + value);
            string.setSpan(new StyleSpan(Typeface.BOLD), keyName.length(), string.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(string);

        } catch (NoSuchFieldException | IllegalAccessException e) {

        }
    }

    public boolean checkIfKeyHasValue(String key, LinkedTreeMap<String, Object> configMap) {
        if (configMap.containsKey(key)) {
            LinkedTreeMap<String, Object> map = (LinkedTreeMap<String, Object>) configMap.get(key);
            if (map.containsKey("value")
                    && map.get("value") != null
                    && (map.get("value") instanceof String && ((String) map.get("value")).length() > 0
                    && !((String) map.get("value")).equalsIgnoreCase("n/a"))
                    && !((String) map.get("value")).equalsIgnoreCase("false")
                    || (map.get("value") instanceof ArrayList && ((ArrayList) map.get("value")).size() > 0
                    && !((ArrayList) map.get("value")).contains("n/a"))) {
                return true;
            }
        }
        return false;
    }

    public boolean checkIfParentChildKeyHasValue(String parentKey, String childkey, LinkedTreeMap<String, Object> configMap) {
        if (configMap.containsKey(parentKey)) {
            LinkedTreeMap<String, Object> map = (LinkedTreeMap<String, Object>) configMap.get(parentKey);
            if (map.containsKey(childkey)) {
                LinkedTreeMap<String, Object> childMap = (LinkedTreeMap<String, Object>) map.get(childkey);
                if (childMap.containsKey("value")
                        && childMap.get("value") != null
                        && (childMap.get("value") instanceof String && ((String) childMap.get("value")).length() > 0 &&
                        !((String) childMap.get("value")).equalsIgnoreCase("n/a"))
                        && !((String) childMap.get("value")).equalsIgnoreCase("false")
                        || (childMap.get("value") instanceof ArrayList && ((ArrayList) childMap.get("value")).size() > 0
                        && !((ArrayList) childMap.get("value")).contains("n/a"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
