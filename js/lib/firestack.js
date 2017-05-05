import Firestack from 'react-native-firestack'
import {
  APP_ID,
  DB_URL,
  STORAGE_BUCKET,
  API_KEY,
  CLIENT_ID,
  PROJECT_ID
} from 'react-native-dotenv';

// setting up firebase
console.log("Creating Firestack instance");
const firestack = new Firestack()

// console.log(DB_URL);
// firestack.database.setPersistence(true);
// firebase.initializeApp({
//   apiKey: API_KEY,
//   authDomain: `${PROJECT_ID}.firebaseapp.com`,
//   databaseURL: DB_URL,
//   storageBucket: STORAGE_BUCKET,
//   // messagingSenderId: ,
// });

export default firestack;