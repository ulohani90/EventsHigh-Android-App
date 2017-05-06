import { createStore, applyMiddleware, compose, combineReducers } from 'redux';
import createSagaMiddleware, { END } from 'redux-saga';
import { autoRehydrate } from 'redux-persist'
import { updateReducers } from './rehydration'
import logger from 'redux-logger';
import firestack from 'lib/firestack';
import makeRootReducer from 'app/reducers';
import rootSaga from 'app/sagas';
import { composeWithDevTools } from 'remote-redux-devtools';


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

  // const loggerMiddleware = createLogger({
  //   predicate: () => __DEV__
  // });
  let composeEnhancer = compose;
  if(__DEV__) {
    // middleware.push(logger);
    composeEnhancer = composeWithDevTools;
  }

  enhancers.push(applyMiddleware(...middleware));
  enhancers.push(autoRehydrate());

  const store = createStore(makeRootReducer(), initialState, composeEnhancer(...enhancers));
  store.asyncReducers = {};

  updateReducers(store);
  firestack.setStore(store);
  sagaMiddleware.run(rootSaga);

  Store = store;
  return store;
}
