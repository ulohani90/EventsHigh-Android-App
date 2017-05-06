import Firestack, { FirestackModule } from 'react-native-firestack'
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
const firestack = new Firestack({
  databaseUrl: DB_URL,
  storageBucket: STORAGE_BUCKET,
})

firestack.database.setPersistence(true);

export const getModule = (r) => new FirestackModule(r, { firestack });

export default firestack;