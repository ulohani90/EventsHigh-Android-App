import React, { Component } from 'react';
import { View, Text, StyleSheet, Image } from 'react-native';
import { Link } from 'react-router-native'
import TimeAgo from 'react-native-timeago'

export default (props) => {
        //<View style={styles.details}>
       // </View>
  return <Link to={`/event/${props._key}`}>
    <View style={styles.container}>
      <Text style={styles.title}>{props.name}</Text>
      <View style={styles.content}>
        <Text style={styles.location}>{props.location}</Text>
        <TimeAgo style={styles.date} time={props.timestamp} />
      </View>
    </View>
  </Link>
}


const styles = StyleSheet.create({
  container: {
    // height: 80,
    margin: 8,
    borderRadius: 8,
    // borderColor: '#c9302c',
    borderWidth: 1,
    marginLeft: 10,
    marginRight: 10
  },
  content: {
    // backgroundColor: '#FFF9F4',
    backgroundColor: '#DB5753',
    flex: 1,
    flexDirection: 'row',
    paddingTop: 10,
    paddingBottom: 10,
  },
  title: {
    backgroundColor: '#BA3632',
    fontFamily: 'monospace',
    textAlign: 'center',
    color: 'white',
    paddingTop: 4,
    paddingBottom: 4,
  },
  location: {
    color: '#ffffff',
    textAlign: 'center',
    flex: 1
  },
  date: {
    textAlign: 'center',
    color: '#FFF9F4',
    flex: 1
    // backgroundColor: '#c9302c',
  }
});