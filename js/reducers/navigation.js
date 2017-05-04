import { NavigationExperimental, StatusBar } from 'react-native';
import { createReducer } from 'reduxsauce';
import {
  NAV_PUSH,
  NAV_POP,
  NAV_JUMP_TO_KEY,
  NAV_JUMP_TO_INDEX,
  NAV_RESET,
  NAV_REPLACE,
  NAV_UPDATE_PARAMS
} from 'action/types';
import { ROUTE_APP, ROUTE_EVENT, ROUTE_EVENT_LIST } from 'route/names';


const initialAppState = {
  index: 0,
  routes: [
    {
      key: ROUTE_EVENT_LIST
    }
  ]
}

const initialState = {
  navigationState: {
    [ROUTE_APP]: initialAppState
  }
}

const {
  StateUtils: NavigationStateUtils
} = NavigationExperimental;

const getRouterState = (state, router=ROUTE_APP) => state.navigationStates[router]

const updateRouterState = (state, routerState, router = ROUTE_APP) => ({
  ...state,
  navigationStates: {
    ...state.navigationStates,
    [router]: {
      ...routerState
    }
  }
})


//TODO:: add a way to figure out which one you are gonna use
const navPush = (state, action) => {
  const router = action.navigator;

  // check if the current state is same as action state than just let it be
  const routeState = getRouterState(state, router);
  if(routeState.routes[routeState.index].key === action.key)
    return state;

  const newState = NavigationStateUtils.push(routeState, {
    key: action.key,
    params: action.params });
  return updateRouterState(state, newState, router);
}


const navPop = (state, action) => {
  const router = action.navigator;

  // check if this is the current or the only router just return the same state
  const routeState = getRouterState(state, router);
  if(routeState.index === 0 || routeState.routes.length === 1)
    return state

  const newState = NavigationStateUtils.pop(routeState);
  return updateRouterState(state, newState, router);
}

const navReplace = (state, action ) => {
  const router = action.navigator;
  const routeState = getRouterState(state, router);

  if(action.index > routeState.routes.length)
    return state
  const { index, key, params={} } = action;
  const newState = NavigationStateUtils.replaceAtIndex(routeState, index, {
    key,
    params
  });
  return updateRouterState(state, newState, router);
}


const navUpdateParams = (state, action) => {
  const router = action.navigator;
  const routeState = getRouterState(state, router);

  const { key, params={} } = action;

  const index = NavigationStateUtils.indexOf(routeState, key);
  oldParams = routeState.routes[index].params || {};
  return navReplace(state, {
    ...action,
    index,
    params: {
      ...oldParams,
      ...params
    }
  });
}


const navJumpToKey = (state, action) => {
  const router = action.navigator;

  const newState = NavigationStateUtils.jumpTo(getRouterState(state, router), action.key);
  return updateRouterState(state, newState, router);
}

const navJumpToIndex = (state, action) => {
  const router = action.navigator;

  const newState = NavigationStateUtils.jumpTo(getRouterState(state, router), action.index);
  return updateRouterState(state, newState, router);
}

// Currently this is equivalent to going to event_list page
const navReset = (state, action) => {
  return initialState
}

export default createReducer(initialState, {
  [NAV_PUSH]: navPush,
  [NAV_POP]: navPop,
  [NAV_JUMP_TO_KEY]: navJumpToKey,
  [NAV_JUMP_TO_INDEX]: navJumpToIndex,
  [NAV_RESET]: navReset,
  [NAV_REPLACE]: navReplace,
  [NAV_UPDATE_PARAMS]: navUpdateParams
});
