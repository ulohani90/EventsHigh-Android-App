package com.eventshigh.nearme.app.view;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.ui.ContactsAdapter.SocialFriendCard;
import com.tokenautocomplete.TokenCompleteTextView;

public class ContactsCompletionView extends TokenCompleteTextView<UserContact> {
    public ContactsCompletionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        allowDuplicates(false);
    }

    @Override
    protected View getViewForObject(UserContact contact) {
        LayoutInflater l = (LayoutInflater)getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = l.inflate(R.layout.view_contact, (ViewGroup) getParent(), false);
        SocialFriendCard card = new SocialFriendCard(view);
        card.populate(getContext(), new SocialFriend(contact));
        return card.itemView;
    }

    @Override
    protected UserContact defaultObject(String completionText) {
        return new UserContact("1", "7406690197", completionText, null);
    }
}
