import React, { Component } from 'react';
import { View, Text, StyleSheet, Image } from 'react-native';
import { Link } from 'react-router-native'

export default (props) => {
  return <Link to={`/event/${props._key}`}>
    <View style={styles.container}>
      <Text style={styles.date}>Date: 10 Apr-2016</Text>
      <View style={styles.content}>
        <Text style={styles.title}>{props.name}</Text>
        <Text style={styles.location}>{props.location}</Text>
      </View>
    </View>
  </Link>
}


const styles = StyleSheet.create({
  container: {
    height: 100,
    margin: 8,
    borderRadius: 2,
    borderColor: '#c9302c',
    borderWidth: 2,
  },
  content: {
    backgroundColor: '#FFF9F4',
    flex: 1,
  },
  title: {

  },
  location: {
    color: '#383f3f'
  },
  date: {
    fontFamily: 'monospace',
    backgroundColor: '#c9302c',
    textAlign: 'center',
    color: 'white',
    paddingTop: 4,
    paddingBottom: 4,
  }
});