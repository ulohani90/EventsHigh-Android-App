import { LOGIN_USER, LOGOUT_USER, FETCH_MY_EVENTS } from 'action/types';


export const loginUser = (user) => ({
  type: LOGIN_USER,
  user
})

export const logoutUser = () => ({
  type: LOGOUT_USER
})

export const fetchMyEvents = () => ({
  type: FETCH_MY_EVENTS
})


export const getCurrentUser = ({app}) => app.user