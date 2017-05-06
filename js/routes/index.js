import React, { Component } from 'react';
import { View, StyleSheet, Text } from 'react-native';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
// import Firestack from 'react-native-firestack'
import {
  APP_ID,
  DB_URL,
  STORAGE_BUCKET,
  API_KEY,
  CLIENT_ID,
  PROJECT_ID
} from 'react-native-dotenv';

import { NativeRouter, Route, Redirect, Switch, AndroidBackButton } from 'react-router-native'

import { ROUTE_APP, ROUTE_EVENT_LIST, ROUTE_EVENT } from 'route/names';
// import { navPop, updateRouteParams } from 'action/navigation';

import EventList from 'container/EventList';
import EventPage from 'container/Event';


const PrivateRoute = connect(mapStateToProps)(({ props, component: Component, ...rest }) => {
  console.log("private route", props);
  return <Route {...rest} render={props => (
    props.authenticated ? (
      <Component {...props}/>
    ) : (
      <Redirect to="/login" />
        // pathname: '/login',
        // state: { from: props.location }
      // }}/>
    )
  )}/>
})


const LoginRoute = connect(mapStateToProps)(({ props, ...rest }) => {
  console.log(props);
  return <Route {...rest} render={ props => (
    props.authenticated ? (
      <Redirect to="/" />
      ) : (
      <Text> This will be login page.</Text>
    )
  )}/>
})

class AppNavigator extends Component {
  constructor(props) {
    super(props);
    // this.handleBack = this._handleBack.bind(this);
  }

  // componentDidMount() {
  //   BackAndroid.addEventListener('hardwareBackPress', this.handleBack);
  // }

  // componentWillUnmount() {
  //   BackAndroid.addEventListener('hardwareBackPress', this.handleBack);
  // }

  // _handleBack() {
  //   this.props.navPop(ROUTE_APP)
  // }

  // componentWillMount() {
  //   const firestack = new Firestack({
  //       applicationId: APP_ID,
  //       debug: __DEV__,
  //       databaseUrl: DB_URL,
  //       storageBucket: STORAGE_BUCKET,
  //       apiKey: API_KEY,
  //       clientId: CLIENT_ID,
  //     });
  //   console.log(firestack)
  //   // debugger
  // }

  render() {
    return (
      <NativeRouter>
        <AndroidBackButton>
        <View style={styles.container}>
          {this.props.authenticated ? (
            <Switch>
              <Route exact path="/" render={() => <Redirect to={ROUTE_EVENT_LIST} />}/>
              <Route path={ROUTE_EVENT_LIST} component={EventList}/>
              <Route path={ROUTE_EVENT} component={EventPage}/>
            </Switch>
          ) : <Text>User needs to login</Text>}
        </View>
        </AndroidBackButton>
      </NativeRouter>
    )
  }
}
const mapStateToProps = state => ({
  authenticated: !!state.app.user
})

export default connect(mapStateToProps)(AppNavigator);

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
})

