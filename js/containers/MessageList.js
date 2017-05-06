import React, { Component } from 'react';
import { View, Text, Image, FlatList, StyleSheet } from 'react-native';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import Message from 'component/Message';
import { fetchMessages } from 'action/messaging'
import { getMsgRefName, objToSortedArray } from 'lib/utils';
import { getModule } from 'lib/firestack'
class MessageList extends Component {
  constructor(props) {
    super(props);
    this.msgList = null;
  }
  componentWillMount() {
    const { eventId, messageId } = this.props;
    // this.props.fetchMessages(eventId, messageId);
    this.inst = getModule(getMsgRefName(eventId, messageId));
    this.inst.listen();
  }

  // componentWillUpdate(prevProps, prevState) {
    // this.msgList.scrollToEnd();
  // }

  renderItem({ item }) {
    return <Message {...item} />
  }
  render() {
    return <View style={styles.container}>
      <FlatList
        ref={(fl) => { this.msgList = fl}}
        data={this.props.messages}
        renderItem={this.renderItem}
        keyExtractor={(i, idx) => `${i._key}-${idx}`}
      />
    </View>
  }
}

const mapStateToProps = (state, ownProps) => {
  const props = {};
  const msgProp = state[getMsgRefName(ownProps.eventId, ownProps.messageId)];

  if(msgProp && msgProp.items) {
    props.messages = objToSortedArray(msgProp.items, 'timestamp');
  } else {
    props.messages = [];
  }
  return props;
}

const mapDispatchToProps = (dispatch) => bindActionCreators({
  fetchMessages
}, dispatch)

export default connect(mapStateToProps, mapDispatchToProps)(MessageList)

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
})