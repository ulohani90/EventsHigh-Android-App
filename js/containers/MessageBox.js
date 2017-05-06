import React, { Component } from 'react';
import { View, Text, StyleSheet, TextInput } from 'react-native';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { postMessage } from 'action/messaging';


class MessageBox extends Component {
  postMessage(evt) {
    const {eventId, messageId }  = this.props;
    this.props.postMessage(eventId, messageId, evt.nativeEvent.text)
    this._textInput.clear();
  }
  render() {
    return <View style={styles.container}>
      <TextInput
        ref={(textInput) => this._textInput = textInput }
        style={styles.textbox}
        placeholder="Type a message"
        autoCapitalize="sentences"
        returnKeyType="send"
        onSubmitEditing={this.postMessage.bind(this)}
      />
    </View>;
  }
}

const mapDispatchToProps = (dispatch) => bindActionCreators({
  postMessage
}, dispatch);

export default connect(null, mapDispatchToProps)(MessageBox);

const styles = StyleSheet.create({
  container: {
    paddingLeft: 8,
    paddingRight: 8,

    borderStyle: "solid",
    borderTopWidth: 1,
    borderTopColor: "#cccccc",
  },
  textbox: {
  }
});