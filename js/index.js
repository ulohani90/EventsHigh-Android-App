import React, { Component } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Provider } from 'react-redux';
import createStore from 'app/store/createStore';
import Router from 'route';

class EventsHigh extends Component {
  render() {
    return (
      <Provider store={createStore()}>
        <Router />
      </Provider>
    )
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
  },
  hello: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
});

export default EventsHigh;