import { combineReducers } from 'redux';
import app from 'reducer/app';
// import { fireMod } from 'lib/firestack';

const makeRootReducer = (asyncReducers) => combineReducers({
  app,
  ...asyncReducers
});

export const injectReducer = (store, { key, reducer }) => {
  store.asyncReducers[key] = reducer
  store.replaceReducer(makeRootReducer(store.asyncReducers))
}

export default makeRootReducer;