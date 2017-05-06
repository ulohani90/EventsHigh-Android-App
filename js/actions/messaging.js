import { POST_MESSAGE, FETCH_MESSAGES } from 'action/types';

export const postMessage = (eventId, messageId=null, message) => ({
  type: POST_MESSAGE,
  eventId,
  messageId,
  message
})

export const fetchMessages = (eventId, messageId=null) => ({
  type: FETCH_MESSAGES,
  messageId,
  eventId
})