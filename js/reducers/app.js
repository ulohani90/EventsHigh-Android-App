import { createReducer } from 'reduxsauce';
import { LOGIN_USER, LOGOUT_USER } from 'action/types';

const initialState = {
  user: null
};

const loginUser = (state, {user}) => ({
  ...state,
  user
})

const logoutUser = (state) => ({
  ...state,
  user: null
})

export default createReducer(initialState, {
  [LOGIN_USER]: loginUser,
  [LOGOUT_USER]: logoutUser
})