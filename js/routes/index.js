import React, { Component } from 'react';
import { View, StyleSheet } from 'react-native';
import { NativeRouter, Route, Redirect } from 'react-router-native'

import { ROUTE_APP, ROUTE_EVENT_LIST, ROUTE_EVENT } from 'route/names';
// import { navPop, updateRouteParams } from 'action/navigation';

import EventList from 'container/EventList';
import Event from 'container/Event';

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

  render() {
    return (
      <NativeRouter>
        <View style={styles.container}>
          <Route exact path="/" render={() => <Redirect to={ROUTE_EVENT_LIST} />}/>
          <Route path={ROUTE_EVENT_LIST} component={EventList}/>
          <Route path={ROUTE_EVENT} component={Event}/>
        </View>
      </NativeRouter>
    )
  }
}

export default AppNavigator;

const styles = StyleSheet.create({
  container: {
    height: 100
  }
})