import { call, fork, put, spawn } from 'redux-saga/effects';
import { takeLatest } from 'redux-saga/effects';

import { STARTUP_ACTION } from 'action/types';

// Add all the startup actions here
function* startupAction() {
}

export default function* root() {
  yield [
    takeLatest(STARTUP_ACTION, startupAction),
  ]
}