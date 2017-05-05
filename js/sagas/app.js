import { takeEvery, call, all, put } from 'redux-saga/effects';
import isEmpty from 'lodash/isEmpty';
import firestack from 'lib/firestack';
import SharedPref from 'react-native-sensitive-info';
import { Store } from 'app/store/createStore';
// import firebase from 'firebase';

import { CHECK_LOGIN } from 'action/types';
import { loginUser, logoutUser } from 'action/app';

firestack._auth.listenForAuth((evt) => {
  console.log("Listening for auth");
  console.log(evt);
  if(evt.authenticated) {
    console.log("Authenticating user");
    Store.dispatch(loginUser(evt.user));
  } else {
    console.log("Logout user");
    Store.dispatch(logoutUser())
  }
});

const getSharedPref = (item) => new Promise((resolve) => {
  console.log(`checking preferences for ${item}`);
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
    console.log("Waiting for firestack to connect");
    yield firestack.configurePromise;
  }
  console.log("getting accesstoken");
  const accessToken = yield call(SharedPref.getItem, 'access_token', { sharedPreferencesName: "eh_user_credentials"});
  if(!isEmpty(accessToken)) {
    // const credential = firebase.auth.FacebookAuthProvider.credential(accessToken);
    // debugger
    console.log(firestack);
    firestack._auth.signInWithProvider('facebook', accessToken);
    // yield call(setCredential, credential);
    // yield put(loginUser(user));
  } else {
    yield put(logoutUser())
  }
}

export default function * appSaga() {
  yield all([
    takeEvery(CHECK_LOGIN, checkLogin),
  ])
}