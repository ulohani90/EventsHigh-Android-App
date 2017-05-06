import React, { Component } from 'react';
import { View, Text, StyleSheet, FlatList, Image } from 'react-native';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { fetchMyEvents } from 'action/app'
import firestack, { getModule } from 'lib/firestack';
import { objToSortedArray } from 'lib/utils';
import EventItem from 'component/EventItem';

class EventList extends Component {
  constructor(props) {
    super(props);
    this.eventList = null;
  }
  componentWillMount() {
    const { user } = this.props;
    this.inst = getModule(`users/${user.uid}/events`);
    this.inst.listen()
  }

  componentWillUnmount() {
    this.inst.unlisten()
  }

  componentDidupdate() {
    this.eventList.scrollToEnd()
  }

  renderItem({item}) {
    return <EventItem {...item} />
  }

  render() {
    return (
      <View style={styles.container}>
        <View style={{height: 100, width: 100, borderRadius: 50, backgroundColor: "white", padding: 9, alignSelf: 'center'}}>
          <Image style={{height: 82, width: 82}} source={{uri: "https://storage.googleapis.com/ehassets/images/logo-82.png"}} />
        </View>
        <FlatList
          ref={(fl) => { this.eventList = fl }}
          style={styles.eventList}
          data={this.props.events}
          renderItem={this.renderItem.bind(this)}
          keyExtractor={(i, idx) => `${i._key}-${idx}`}
        />
      </View>
    );
        // <View style={styles.pastEvents}>
        //   <Text>Past Events</Text>
        // </View>
  }
}

const mapStateToProps = (state) => {
  const props = {
    user: state.app.user,
  }
  const events = state[`users/${state.app.user.uid}/events`];
  if(events && events.items) {
    props.events = objToSortedArray(events.items);
  } else {
    props.events = []
  }

  return props;
}

const mapDispatchToProps = (dispatch) => bindActionCreators({
  fetchMyEvents
}, dispatch)

export default connect(mapStateToProps, mapDispatchToProps)(EventList);

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'column',
    backgroundColor: '#3b3d43'
  },
  eventList: {
    flex: 0.9,
  },
  pastEvents: {
    flex: 0.1
  }
})