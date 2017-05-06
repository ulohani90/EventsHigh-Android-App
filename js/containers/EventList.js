import React, { Component } from 'react';
import { View, Text, StyleSheet, FlatList } from 'react-native';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { fetchMyEvents } from 'action/app'
import firestack, { getModule } from 'lib/firestack'
import EventItem from 'component/EventItem';

class EventList extends Component {
  constructor(props) {
    super(props);
    this.inst = null;
  }
  componentWillMount() {
    const { user } = this.props;
    this.inst = getModule(`users/${user.uid}/events`);
    this.inst.listen();
  }

  componentWillUnmount() {
    this.inst.unlisten();
  }

  renderItem({item}) {
    return <EventItem {...item} />
  }

  render() {
    return (
      <View style={styles.container}>
        <FlatList
          style={styles.eventList}
          data={this.props.events}
          renderItem={this.renderItem.bind(this)}
          keyExtractor={(i, idx) => `${i._key}-${idx}`}
        />
        <View style={styles.pastEvents}>
          <Text>Past Events</Text>
        </View>
      </View>
    );
  }
}

const mapStateToProps = (state) => {
  const props = {
    user: state.app.user,
  }
  const events = state[`users/${state.app.user.uid}/events`];
  if(events && events.items) {
    props.events = events.items
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