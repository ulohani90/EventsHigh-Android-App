package com.eventshigh.nearme.app.ui.adapter;

import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest.SocialInvite;

import java.util.Set;

/**
 * Provider for social data used in adapters.
 */
public interface SocialDataProvider {
    /**
     * @param tag tag name
     * @return {@link Set} of {@link SocialFriend} who is following given tag.
     * It can be {@code NULL} or empty.
     */
    @Nullable Set<SocialFriend> getFollowers(String tag);

    /**
     * @param eventId event id.
     * @return An {@link SocialInvite} if user has been invited to this event
     * by his friend, {@code NULL} otherwise.
     */
    @Nullable SocialInvite getSocialInvite(String eventId);

    @Nullable
    Set<SocialFriend> getSocialActions(String eventId);
}
