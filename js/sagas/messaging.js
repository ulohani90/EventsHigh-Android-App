import { POST_MESSAGE, FETCH_MESSAGES } from 'action/types';
import { put, call, takeEvery, all, select} from 'redux-saga/effects';
import isEmpty from 'lodash/isEmpty';
import { getMsgRefName } from 'lib/utils';
import { getCurrentUser } from 'action/app';
import firestack from 'lib/firestack';

function * postMessage({eventId, messageId, message}) {
  const user = yield select(getCurrentUser);

  const msgRefName = getMsgRefName(eventId, messageId);
  const msgRef = firestack.database.ref(msgRefName).push({
    author: {
      name: user.displayName,
      image: user.photoUrl
    },
    type: 'text',
    timestamp: (new Date()).getTime(),
    message: message
  });
}

function * fetchMessages({ eventId, messageId }) {
  const msgRefName = getMsgRefName(eventId, messageId);
  const msgRef = firestack.database.ref(msgRefName).orderByChild("timestamp")
  const values = yield call(msgRef.get.bind(msgRef));
  console.log(values)
}

export default function * messagingSaga() {
  yield all([
    takeEvery(POST_MESSAGE, postMessage),
    // takeEvery(FETCH_MESSAGES, fetchMessages),
  ]);
}