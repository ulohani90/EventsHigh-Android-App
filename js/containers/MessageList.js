import React, { Component } from 'react';
import { View, Text, Image, FlatList, StyleSheet } from 'react-native';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import Message from 'component/Message';
import { fetchMessages } from 'action/messaging'
import { getMsgRefName } from 'lib/utils';
import { getModule } from 'lib/firestack'
class MessageList extends Component {
  componentWillMount() {
    const { eventId, messageId } = this.props;
    // this.props.fetchMessages(eventId, messageId);
    this.inst = getModule(getMsgRefName(eventId, messageId));
    this.inst.listen();
  }

  renderItem({ item }) {
    return <Message {...item} />
  }
  render() {
    return <View style={styles.container}>
      <FlatList
        data={this.props.messages.items}
        renderItem={this.renderItem}
        keyItereator={(i, idx) => `${i._key}-${idx}`}
      />
    </View>
  }
}

const mapStateToProps = (state, ownProps) => {
  const props = {};
  const msgProp = state[getMsgRefName(ownProps.eventId, ownProps.messageId)];

  if(msgProp && msgProp.items) {
    props.messages = msgProp;
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