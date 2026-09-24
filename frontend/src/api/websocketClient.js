import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getToken } from './axiosInstance';

let stompClient = null;

const getSockJS = () => {
  if (typeof SockJS === 'function') return SockJS;
  if (SockJS && typeof SockJS.default === 'function') return SockJS.default;
  if (typeof window !== 'undefined' && window.SockJS) return window.SockJS;
  return SockJS;
};

//stomp over sockjs client connecting to backend /ws endpoint with jwt auth
export const websocketClient = {
  connect: (onConnected, onError) => {
    const token = getToken();
    if (!token) {
      // Guests must never trigger connection attempts
      return;
    }

    if (stompClient && stompClient.active) {
      return;
    }

    const SockFactory = getSockJS();
    stompClient = new Client({
      webSocketFactory: () => new SockFactory('http://localhost:8081/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      beforeConnect: () => {
        const currentToken = getToken();
        if (currentToken && stompClient) {
          stompClient.connectHeaders = {
            Authorization: `Bearer ${currentToken}`
          };
        }
      },
      reconnectDelay: 5000,
      debug: (msg) => {
        // Stomp protocol debug trace
      },
      onConnect: (frame) => {
        console.log('[WebSocket] STOMP connected successfully.');
        if (onConnected) onConnected(frame);
      },
      onStompError: (frame) => {
        console.error('[WebSocket] STOMP broker error:', frame.headers?.message, frame.body);
        if (onError) onError(frame);
      },
      onWebSocketClose: (event) => {
        // Connection closed
      }
    });

    stompClient.activate();
  },

  disconnect: () => {
    if (stompClient) {
      stompClient.deactivate();
      stompClient = null;
      console.log('[WebSocket] Disconnected.');
    }
  },

  subscribe: (destination, callback) => {
    if (!stompClient || !stompClient.connected) {
      console.warn('[WebSocket] Cannot subscribe, STOMP client is not connected:', destination);
      return null;
    }
    return stompClient.subscribe(destination, (message) => {
      let parsed = message.body;
      try {
        parsed = JSON.parse(message.body);
      } catch (e) {
        // Keep string content if payload is not json
      }
      callback(parsed, message);
    });
  },

  publish: (destination, body = {}) => {
    if (!stompClient || !stompClient.connected) {
      console.warn('[WebSocket] Cannot publish, STOMP client is not connected:', destination);
      return;
    }
    stompClient.publish({
      destination,
      body: typeof body === 'string' ? body : JSON.stringify(body)
    });
  },

  getClient: () => stompClient
};

export default websocketClient;
