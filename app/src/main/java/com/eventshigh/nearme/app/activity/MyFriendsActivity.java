package com.eventshigh.nearme.app.activity;

import android.os.Bundle;

/**
 * Shows the user's friends. See {@link ContactsFragment}.
 */
public class MyFriendsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If not already added to the Fragment manager add it. If you don't do this a
        // new Fragment will be added every time this method is called (Such as on orientation change)
        if(savedInstanceState == null) {
            ContactsFragment contactsFragment = new ContactsFragment();
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, contactsFragment).commit();
        }
    }
}
