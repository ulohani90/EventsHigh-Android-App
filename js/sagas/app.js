import { takeEvery, call, all, put, select } from 'redux-saga/effects';
import isEmpty from 'lodash/isEmpty';
import firestack from 'lib/firestack';
import SharedPref from 'react-native-sensitive-info';
import { Store } from 'app/store/createStore';
import { FirestackModule } from 'react-native-firestack';
// import firebase from 'firebase';

import { CHECK_LOGIN, FETCH_MY_EVENTS, LOGIN_USER, LOGOUT_USER } from 'action/types';
import { loginUser, logoutUser, getCurrentUser } from 'action/app';

firestack._auth.listenForAuth((evt) => {
  if(evt.authenticated) {
    const { user } = evt;
    firestack.database.ref(`users/${user.uid}`).update({
      name: user.displayName,
      image: user.photoUrl,
      email: user.email,
      status: "online"
    });
    Store.dispatch(loginUser(evt.user));
  } else {
    Store.dispatch(logoutUser())
  }
});

const getSharedPref = (item) => new Promise((resolve) => {
  SharedPref.getItem(item, resolve)
})

// const setCredential = (credential) => new Promise((resolve, reject) => {
//   firebase.auth().signInWithCredential(credential).catch((error) => {
//     // handle errors here
//     Store.dispatch(logoutUser());
//   })
//   resolve();
// })

function * checkLogin() {
  if(!firestack.configured) {
    yield firestack.configurePromise;
  }
  const accessToken = yield call(SharedPref.getItem, 'access_token', { sharedPreferencesName: "eh_user_credentials"});
  if(!isEmpty(accessToken)) {
    firestack._auth.signInWithProvider('facebook', accessToken);
  }
}

function goOffline() {
  // const presence = firestack.presence;
  // presence.on('auser').setOffline();
}

export default function * appSaga() {
  yield all([
    takeEvery(CHECK_LOGIN, checkLogin),
    // takeEvery(LOGOUT_USER, goOffline),
  ])
}