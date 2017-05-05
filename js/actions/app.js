import { LOGIN_USER, LOGOUT_USER } from 'action/types';


export const loginUser = (user) => ({
  type: LOGIN_USER,
  user
})

export const logoutUser = () => ({
  type: LOGOUT_USER
})