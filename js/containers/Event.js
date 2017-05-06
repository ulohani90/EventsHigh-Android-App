import React, { Component } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import MessageList from 'container/MessageList';
import MessageBox from 'container/MessageBox'

class EventPage extends Component {
  render() {
    return <View style={styles.container}>
      <MessageList
        eventId={this.props.match.params.eid}
        messageId={this.props.match.params.messageId}
        style={styles.messageList} />
      <MessageBox
        style={styles.messageBox}
        eventId={this.props.match.params.eid}
        messageId={this.props.match.params.messageId}
        />
    </View>
  }
}

export default EventPage;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'column'
  },
  messageList: {
    flex: 1,
    backgroundColor: "red"
  },
  messageBox: {
  }
});