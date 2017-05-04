import { AsyncStorage } from 'react-native';
import { persistStore } from 'redux-persist';
import startupAction from 'action';

export const REDUX_PERSIST = {
  active: true,
  reducerVersion: '1', // need to update whenever we make a huge change in redux actions and release
  keyPrefix: 'EventsHigh',
  storeConfig: {
    storage: AsyncStorage
  }
}

export const updateReducers = (store) => {
  const { reducerVersion, storeConfig: config } = REDUX_PERSIST;


  AsyncStorage.getItem('reducerVersion').then((localVersion) => {
    if(localVersion !== reducerVersion) {
      persistStore(store, config).purge();
    } else {
      persistStore(store, config);
    }
    store.dispatch(startupAction());
  }).catch(() => {
    persistStore(store, config)
    AsyncStorage.setItem('reducerVersion', reducerVersion)
    store.dispatch(startupAction())
  })
}