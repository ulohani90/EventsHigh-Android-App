package com.eventshigh.nearme.app.ui.adapter;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.UserProfileActivity;
import com.eventshigh.nearme.app.data.FriendsStore;
import com.eventshigh.nearme.app.data.NewSocialFriend;
import com.eventshigh.nearme.app.view.CircularImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by umesh on 23/07/16.
 */
public class NewContactsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final BaseActivity activity;

    private FriendsStore friendsStore;

    OnInviteBtnClick mListener;

    public NewContactsAdapter(BaseActivity activity) {
        this.activity = activity;
        friendsStore = new FriendsStore(activity);

    }

    public void setOnInviteBtnClick(OnInviteBtnClick listener) {
        this.mListener = listener;
    }


    int TYPE_USER_CONTACT = 1;

    int TYPE_INVITE_FRIENDS = 2;

    public void setFriendList(List<NewSocialFriend> friendList) {
        this.friendList = friendList;
    }

    private List<NewSocialFriend> friendList = new ArrayList<>();

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_USER_CONTACT) {
            View view = activity.getLayoutInflater().inflate(R.layout.card_contact, parent, false);
            return new FriendViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.invite_friends_btn_layout, parent, false);
            return new InviteButtonViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if (getItemViewType(position) == TYPE_USER_CONTACT) {
            ((FriendViewHolder) holder).contactName.setText(friendList.get(position).getName());

            int size = ((FriendViewHolder) holder).contactPhoto.getLayoutParams().height;
            Glide.with(activity).load(friendList.get(position).getPhoto())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                    .into(((FriendViewHolder) holder).contactPhoto);

            if (((FriendViewHolder) holder).followButton != null) {
                ((FriendViewHolder) holder).followButton.setVisibility(View.INVISIBLE);
            }
            ((FriendViewHolder) holder).followButton.setVisibility(friendsStore.isFollowing(friendList.get(position).getEmail()) ? View.VISIBLE : View.GONE);
            updateActionButton(((FriendViewHolder) holder), friendsStore, friendList.get(position));
            ((FriendViewHolder) holder).followButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (friendList.get(position).getEmail() != null) {
                        friendsStore.setFollowing(friendList.get(position).getEmail(), null, !((FriendViewHolder) holder).followButton.isSelected());
                        updateActionButton(((FriendViewHolder) holder), friendsStore, friendList.get(position));
                    }
                }
            });
            ((FriendViewHolder) holder).parentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(activity, UserProfileActivity.class);
                    intent.putExtra(UserProfileActivity.PROFILE_ID, friendList.get(position).getEmail());
                    activity.startActivity(intent);

                }
            });
        } else {
            ((InviteButtonViewHolder) holder).inviteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onInviteBtnClick();
                }
            });
        }

    }

    private void updateActionButton(FriendViewHolder holder, FriendsStore friendsStore, NewSocialFriend friend) {
        if (friend.getEmail() != null && friendsStore.isFollowing(friend.getEmail())) {
            holder.followButton.setText(R.string.ui_following);
            holder.followButton.setSelected(true);
        } else {
            holder.followButton.setText(R.string.ui_follow);
            holder.followButton.setSelected(false);
        }
    }


    @Override
    public int getItemCount() {
        if (friendList != null)
            return friendList.size();
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == friendList.size() - 1) {
            return TYPE_INVITE_FRIENDS;
        } else {
            return TYPE_USER_CONTACT;
        }
    }


    public class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView contactName;
        final CircularImageView contactPhoto;
        final TextView followButton;
        LinearLayout parentLayout;

        public FriendViewHolder(View itemView) {
            super(itemView);
            contactName = (TextView) itemView.findViewById(R.id.contact_name);
            contactPhoto = (CircularImageView) itemView.findViewById(R.id.contact_photo);
            followButton = (TextView) itemView.findViewById(R.id.follow_button);
            parentLayout = (LinearLayout) itemView.findViewById(R.id.parent_layout);
        }
    }

    public class InviteButtonViewHolder extends RecyclerView.ViewHolder {

        TextView inviteBtn;

        public InviteButtonViewHolder(View itemView) {
            super(itemView);
            inviteBtn = (TextView) itemView.findViewById(R.id.invite_button);
        }
    }

    public interface OnInviteBtnClick {
        void onInviteBtnClick();
    }
}
