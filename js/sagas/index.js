import { takeLatest, all, call, put, fork } from 'redux-saga/effects';

import { STARTUP_ACTION, CHECK_LOGIN } from 'action/types';
import appSagas from 'saga/app';
import messagingSaga from 'saga/messaging'

// Add all the startup actions here
function * startupAction() {
  console.log("Checking login");
  yield put({type: CHECK_LOGIN});
}

export default function * root() {
  yield all([
    takeLatest(STARTUP_ACTION, startupAction),
    fork(appSagas),
    fork(messagingSaga)
  ])
}