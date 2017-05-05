import { createStore, applyMiddleware, compose, combineReducers } from 'redux';
import createSagaMiddleware, { END } from 'redux-saga';
import { autoRehydrate } from 'redux-persist'
import { updateReducers } from './rehydration'
import { createLogger } from 'redux-logger';
import reducers from 'app/reducers';
import rootSaga from 'app/sagas';

export let Store = null

export default configureStore = (initialState) => {
  const middleware = []
  const enhancers = []

  const sagaMiddleware = createSagaMiddleware();
  middleware.push(sagaMiddleware);

  const SAGA_LOGGING_BLACKLIST = [
    'EFFECT_TRIGGERED',
    'EFFECT_RESOLVED',
    'EFFECT_REJECTED',
    'persist/REHYDRATE'
  ]

  const loggerMiddleware = createLogger({
    predicate: () => __DEV__
  });
  // if(__DEV__) {
  //   middleware.push(loggerMiddleware)
  // }

  enhancers.push(applyMiddleware(...middleware));
  enhancers.push(autoRehydrate());

  const store = createStore(reducers, initialState, compose(...enhancers));

  updateReducers(store);
  sagaMiddleware.run(rootSaga);

  Store = store;
  return store;
}
