import isEmpty from 'lodash/isEmpty';

export const getMsgRefName = (eventId, messageId)  => {
  let refName = `events/${eventId}/messages`
  if(!isEmpty(messageId)) {
    refName += `/${messageId}/messages`
  }
  return refName
}