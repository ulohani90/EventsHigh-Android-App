package com.eventshigh.nearme.app.activity;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.ui.AskForContactsDialog;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.user.UserActionHelper;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.utils.ZendeskUtils;
import com.eventshigh.nearme.app.view.ZCustomFlowLayout;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.zendesk.sdk.feedback.ui.ContactZendeskActivity;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

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
    TextView eventOrganizer, eventTime, timeDetails, addressView, travelTimeView, alsoOnView, addToCalender;
    HorizontalScrollView futureTimesViewGroup;
    LinearLayout futureTimesView, youtubeFragment;
    WebView descriptionView;
    View timeGroupView, eventTimeFirstView;
    ZCustomFlowLayout performersFlowLayout, venueFlowLayout, categoriesFlowLayout;
    View view;
    View enquiryBtn, callOrganizer, directionLayout, mapDirection;

    TextView eventVenueText;

    private GoogleApiClient client;
    private Action viewAction = null;

    Account account;

    View venueHeader, performersHeader;

    TextView ratingHeader, reviewsCount;

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
        addressView = (TextView) view.findViewById(R.id.event_address);
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
        return view;
    }

    public void updateReview(Event event) {
        if (event.reviewObjects.size() > 0) {
            view.findViewById(R.id.review_card).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.tv_user_review_by)).setText(event.reviewObjects.get(0).getReviewBy());
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
            /*
            Glide.with(this).load("url")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                    .into((CircularImageView)findViewById(R.id.civ_user_review));
            */
            //  findViewById(R.id.ll_event_write_review).setVisibility(View.GONE);
            Preferences.getInstance(getActivity()).setIsReviewAdded(false);
        }

    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        account = new Account(getActivity());
        final Event event = getArguments().getParcelable("event");
        eventName.setText(event.title);
        if (event.organizerName != null) {
            eventOrganizer.setText("By " + event.organizerName);
        }
        DateTimeUtils.EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
        timeGroupView.setVisibility(eventTime == null ? View.GONE : View.VISIBLE);
        if (!event.description.isEmpty()) {
            CustomUrlActivity.setupWebView(descriptionView, (BaseContextActivity) getActivity(), false);
            descriptionView.loadData(toHtmlNoFrame(event.description), "text/html; charset=UTF-8", null);
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
            if (event.eventTimings.length > 1) {
                for (int i = 1; i < event.eventTimings.length; i++) {
                    eventTime = DateTimeUtils.getEventTime(event, i);
                    if (eventTime == null) {
                        break;
                    }

                    final Date eventDateCurr = new Date(event.eventTimings[i]);
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
        if (event.venue != null) {
            builder.append(event.venue + "\n");
        }
        if (event.address != null) {
            builder.append(event.address);
        } else {
            builder.append(event.getFullAddress());
        }
        SpannableString string = new SpannableString(builder.toString());
        if (event.venue != null) {
            string.setSpan(new ForegroundColorSpan(Color.parseColor("#353535")), 0, event.venue.length() + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            string.setSpan(new RelativeSizeSpan(1.2f), 0, event.venue.length() + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        eventVenueText.setText(string);
        travelTimeView.setVisibility(View.GONE);
        view.findViewById(R.id.direction_separator).setVisibility(View.GONE);
        if (event.performers != null && event.performers.length > 0) {
            performersHeader.setVisibility(View.VISIBLE);
            performersFlowLayout.setVisibility(View.VISIBLE);
            performersFlowLayout.setRecipientForEventCategories((BaseContextActivity) getActivity(), new Account(getActivity()), new ArrayList<String>(Arrays.asList(event.performers)), event, "performerAsTag");

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

        if (event.organizerPhone != null) {
            enquiryBtn.setVisibility(View.GONE);
            callOrganizer.setVisibility(View.VISIBLE);

            callOrganizer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    call(event);
                }
            });
        } else {
            enquiryBtn.setVisibility(View.VISIBLE);
            callOrganizer.setVisibility(View.GONE);
            enquiryBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ama(event);
                }
            });
        }


        mapDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDirections(event);
            }
        });


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
    }

    LatLng userLocation;

    private void getGoogleApiClient(final Event event) {
        if (client != null && client.isConnected()) {
            populateEventTravelTime(event);
            return;
        }

        client = new GoogleApiClient.Builder(getActivity())
                .addApi(AppIndex.API)
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
        viewAction = new Action.Builder(Action.TYPE_VIEW)
                .setObject(new Thing.Builder()
                        .setName(event.title)
                        .setId(webUri.toString())
                        .setUrl(Utils.getAppUri(webUri))
                        .build())
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
        AppIndex.AppIndexApi.start(client, viewAction);
    }


    public void writeReview(Event event) {
        if (account.getUserInfo().phoneNo == null || account.getUserInfo().name == null) {
            PhoneVerificationDialog.show(((NewEventDetailActivity) getActivity()), R.string.ui_verify_phone, R.string.ui_phone_verify_book);
            return;
        }
        ((NewEventDetailActivity) getActivity()).reportActionToAnalytics("write_review_btn_click");
        Intent i = new Intent(getActivity(), WriteReviewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable("movie_detail_object", event);
        bundle.putString(MovieDetailActivity.OBJECT_TYPE, "event");
        i.putExtras(bundle);
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.animate_slide_up, R.anim.stay);

    }

    public String getAverageRating(Event event) {
        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < event.reviewObjects.size(); i++) {
            sum += event.reviewObjects.get(i).getReviewRating();
            count++;
        }

        return ((sum / count)) + "/5.0";
    }

    private void populateEventTravelTime(Event event) {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(client);
            if (location != null) {
                userLocation = LocationUtils.locationToLatLng(location);
            }

            String eventTravelTime = LocationUtils.getTravelTime(getActivity(),
                    userLocation, event.location);
            travelTimeView.setVisibility(eventTravelTime == null ? View.GONE : View.VISIBLE);
            view.findViewById(R.id.direction_separator).setVisibility(eventTravelTime == null ? View.GONE : View.VISIBLE);
            if (eventTravelTime != null) {
                travelTimeView.setText(eventTravelTime);
            }
        }
    }

    public void ama(Event event) {
        Account account = new Account(getActivity());
        Account.UserInfo userInfo = account.getUserInfo();
        if (userInfo.phoneNo == null || userInfo.name == null) {
            PhoneVerificationDialog.show(((NewEventDetailActivity) getActivity()), R.string.ui_verify_phone, R.string.ui_phone_verify_plan);
            return;
        }

        Preferences preferences = Preferences.getInstance(getActivity());
        if (!preferences.canUploadContacts()) {
            if (AskForContactsDialog.checkIfToShow(((NewEventDetailActivity) getActivity()), preferences)) {
                return;
            }
        }


        ((NewEventDetailActivity) getActivity()).reportEventAction(event, "ama");
        ZendeskUtils.initZendesk(getActivity());
        ZendeskUtils.setEventFeedbackConfiguration(getActivity(), event);
        Intent feedbackIntent = new Intent(getActivity(), ContactZendeskActivity.class);
        getActivity().startActivity(feedbackIntent);
    }

    public void save(Event event) {
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
        if (userInfo.phoneNo == null || userInfo.name == null) {
            PhoneVerificationDialog.show(((NewEventDetailActivity) getActivity()), R.string.ui_verify_phone, R.string.ui_phone_verify_plan);
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

    public void showDirections(Event event) {
        ((NewEventDetailActivity) getActivity()).addToFavourite = true;
        ((NewEventDetailActivity) getActivity()).reportEventAction(event, "showDirections");

        Intent intent = event.getShowDirectionsOnMapIntent();
        if (intent == null) {
            ((NewEventDetailActivity) getActivity()).reportActionToAnalytics("skipDirectionsNoLocation");
            ((NewEventDetailActivity) getActivity()).showMessage(R.string.failed_event_location);
            return;
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open maps.
            Crashlytics.getInstance().core.logException(e);
            ((NewEventDetailActivity) getActivity()).showMessage(R.string.no_map_app);
        }
    }

    private static String toHtmlNoFrame(String html) {
        return "<body>" + html.replaceAll("<iframe.*/iframe>", "") + "</body>";
    }


    @Override
    public void onStop() {
        super.onStop();
        if (client != null && client.isConnected()) {
            if (viewAction != null) {
                AppIndex.AppIndexApi.end(client, viewAction);
            }
            client.disconnect();
        }
    }
}
