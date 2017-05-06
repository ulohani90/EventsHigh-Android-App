import isEmpty from 'lodash/isEmpty';

export const getMsgRefName = (eventId, messageId)  => {
  let refName = `events/${eventId}/messages`
  if(!isEmpty(messageId)) {
    refName += `/${messageId}/messages`
  }
  return refName
}

export const objToSortedArray = (obj, key="timestamp") => Object
  .keys( obj ).sort(function( a, b ) {
    return obj[a][key] - obj[b][key];
  }).map(function( sortedKey ) {
    return obj[sortedKey];
  });
