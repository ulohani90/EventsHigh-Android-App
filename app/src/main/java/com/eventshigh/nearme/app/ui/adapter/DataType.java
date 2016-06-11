package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.activity.BaseActivity;

/**
 * Various card types used in various adapters.
 */
public enum DataType {
    HEADER(0),
    EVENT(1),
    FOLLOW(3),
    TRENDING_CATEGORY(4),
    EXPLORE_CATEGORY(5),
    SMALL_HEADER(6),
    EVENT_PAGER(7),
    SEE_ALL(8),
    EVENT_INVITATION(9),
    SOCIAL_FRIEND(10),
    EH_INVITE(11),
    EH_INVITE_NOTIFICATION(12),
    EXPLORE_LOCALITY(13),
    OFFER(14),
    POINTS(15),
    TOTAL_POINT_HEADER(16),
    POINT_BREAKDWON(17),
    MOVIE_REVIEW(18),
    SHOWTIME(19),
    MOVIE_INFO(20),
    MOVIE_LIST_CARD(21),
    MOVIE_CATEGORY(22),
    MOVIE_USER_REVIEW(23),
    NEW_EXPLORE_CATEOGRY(24);

    public final int typeId;

    DataType(int typeId) {
        this.typeId = typeId;
    }

    public static boolean spanAllColumns(int typeId) {
        return typeId == HEADER.typeId || typeId == SMALL_HEADER.typeId
                || typeId == EVENT_PAGER.typeId || typeId == SEE_ALL.typeId
                || typeId == EVENT_INVITATION.typeId || typeId == OFFER.typeId
                || typeId == POINTS.typeId || typeId == TOTAL_POINT_HEADER.typeId
                || typeId == POINT_BREAKDWON.typeId || typeId == MOVIE_CATEGORY.typeId;
    }

    public static ViewHolder onCreateViewHolder(BaseActivity activity, ViewGroup parent, int typeId) {
        if (typeId == HEADER.typeId) {
            return HeaderCard.newInstance(activity, parent);
        }

        if (typeId == EVENT.typeId) {
            return EventCard.newInstance(activity, parent, false, true);
        }

        if (typeId == FOLLOW.typeId) {
            return FollowCard.newInstance(activity, parent);
        }

        if (typeId == TRENDING_CATEGORY.typeId) {
            return TrendingCategoryCard.newInstance(activity, parent);
        }

        if (typeId == EXPLORE_CATEGORY.typeId) {
            return TrendingCategoryCard.newInstance(activity, parent);
        }

        if (typeId == SMALL_HEADER.typeId) {
            return SmallHeaderCard.newInstance(activity, parent);
        }

        if (typeId == EVENT_PAGER.typeId) {
            return EventPagerCard.newInstance(activity, parent);
        }

        if (typeId == SEE_ALL.typeId) {
            return SeeAllCard.newInstance(activity, parent);
        }

        if (typeId == EVENT_INVITATION.typeId) {
            return EventInvitationCard.newInstance(activity, parent);
        }

        if (typeId == SOCIAL_FRIEND.typeId) {
            return SocialFriendCard.newInstance(activity, parent);
        }

        if (typeId == EH_INVITE.typeId) {
            return EhInviteCard.newInstance(activity, parent);
        }

        if (typeId == EH_INVITE_NOTIFICATION.typeId) {
            return EhInviteNotificationCard.newInstance(activity, parent);
        }

        if (typeId == EXPLORE_LOCALITY.typeId) {
            return LocalityCard.newInstance(activity, parent);
        }
        if (typeId == OFFER.typeId) {
            return OfferCard.newInstance(activity, parent);
        }

        if (typeId == POINTS.typeId) {
            return PointsCard.newInstance(activity, parent);
        }

        if (typeId == TOTAL_POINT_HEADER.typeId) {
            return TotalPointsHeaderCard.newInstance(activity, parent);
        }
        if (typeId == POINT_BREAKDWON.typeId) {
            return PointBreakdownCard.newInstance(activity, parent);
        }

        if (typeId == MOVIE_REVIEW.typeId) {
            return MovieReviewCard.newInstance(activity, parent);
        }
        if (typeId == MOVIE_USER_REVIEW.typeId) {
            return MovieUserReviewCard.newInstance(activity, parent);
        }
        if (typeId == SHOWTIME.typeId) {
            return ShowTimeCard.newInstance(activity, parent);
        }
        if (typeId == MOVIE_INFO.typeId) {
            return MovieInfoCard.newInstance(activity, parent);
        }
        if (typeId == MOVIE_LIST_CARD.typeId) {
            return MovieListCard.newInstance(activity, parent);
        }
        if (typeId == MOVIE_CATEGORY.typeId) {
            return TrendingCategoryCard.newInstance(activity, parent);
        }
        if (typeId == NEW_EXPLORE_CATEOGRY.typeId) {
            return ExploreCategoryCard.newInstance(activity, parent);
        }
        throw new IllegalArgumentException("invalid typeid");
    }
}
