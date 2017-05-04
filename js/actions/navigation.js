import { ROUTE_APP, ROUTE_EVENT_LIST, ROUTE_EVENT } from 'route/names';
import { NAV_PUSH, NAV_POP, NAV_REPLACE, NAV_JUMP_TO_KEY, } from 'action/types';

export const navJumpToKey = (key, navigator=ROUTE_APP) => ({
  type: NAV_JUMP_TO_KEY,
  key,
  navigator
})

export const navPop = (navigator: String) => ({
  type: NAV_POP,
  navigator
})

export const navPush = (key: String, navigator: String, params={}) => ({
  type: NAV_PUSH,
  key,
  navigator,
  params
})

export const navReplace = (navigator: String, index: Integer, key: String) => ({
  type: NAV_REPLACE,
  navigator,
  index,
  key,
})

export const updateRouteParams = (key, params={}) => ({
  type: NAV_UPDATE_PARAMS,
  navigator: ROUTE_APP,
  key,
  params
});

export const navigateToEvent = (eventId: String) => navPush(ROUTE_EVENT, { eventId })