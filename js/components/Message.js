import React, { Component } from 'react';
import { View, Text, Image, StyleSheet } from 'react-native';
import TimeAgo from 'react-native-timeago';

export default (props) => {
  return (<View style={styles.container}>
      <View style={styles.imageContainer}>
        <Image source={{uri: props.author.image}} style={styles.authorImage}></Image>
      </View>
      <View style={styles.contentContainer}>
        <View style={styles.messageInfo}>
          <Text style={styles.authorInfo}>{props.author.name}</Text>
          <TimeAgo style={styles.timestamp} time={props.timestamp}/>
        </View>
        <Text style={styles.messageContent}>{props.message}</Text>
      </View>
    </View>);
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'row'
  },
  imageContainer: {
  },
  authorImage: {
    width: 40,
    height: 40,
    borderRadius: 2,
    margin: 4
  },

  contentContainer: {
    flex: 1,
  },

  messageInfo: {
    flexDirection: 'row',
    padding: 4
  },
  authorInfo : {
    fontWeight: "600",
    paddingRight: 15
  },
  timestamp: {
    fontWeight: "100",
    color: "#cccccc"
  },

  messageContent: {
    fontFamily: 'Roboto',
    fontWeight: "100",
    color: "#0a0a0a",
    padding: 4
  }

})