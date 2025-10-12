import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface StatusUpdateEvent {
  eventType: string;
  entityType: string;
  entityId: string;
  status?: string;
  progress?: number;
  recordsProcessed?: number;
  totalRecords?: number;
  phase?: string;
  message?: string;
  timestamp: string;
  metadata?: any;
}

export type MessageHandler = (event: StatusUpdateEvent) => void;

/**
 * WebSocket service for real-time updates
 * Uses STOMP over SockJS for browser compatibility
 */
class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 10;
  private reconnectDelay = 1000; // Start with 1 second
  private connectionStatus: 'disconnected' | 'connecting' | 'connected' = 'disconnected';
  private statusListeners: Array<(status: string) => void> = [];

  /**
   * Connect to WebSocket server
   */
  connect(): void {
    if (this.client && this.client.active) {
      console.log('WebSocket already connected');
      return;
    }

    const socketFactory = () => new SockJS('http://localhost:8080/ws');

    this.client = new Client({
      webSocketFactory: socketFactory,
      debug: (str) => {
        console.debug('[WebSocket Debug]', str);
      },
      reconnectDelay: this.reconnectDelay,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('WebSocket connected successfully');
        this.connectionStatus = 'connected';
        this.reconnectAttempts = 0;
        this.reconnectDelay = 1000;
        this.notifyStatusListeners('connected');
      },
      onDisconnect: () => {
        console.log('WebSocket disconnected');
        this.connectionStatus = 'disconnected';
        this.notifyStatusListeners('disconnected');
      },
      onStompError: (frame) => {
        console.error('WebSocket STOMP error:', frame.headers['message']);
        console.error('Error details:', frame.body);
        this.handleReconnect();
      },
      onWebSocketClose: (event) => {
        console.warn('WebSocket connection closed:', event.reason);
        this.handleReconnect();
      },
    });

    this.connectionStatus = 'connecting';
    this.notifyStatusListeners('connecting');
    this.client.activate();
  }

  /**
   * Handle reconnection with exponential backoff
   */
  private handleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached. Please refresh the page.');
      this.notifyStatusListeners('error');
      return;
    }

    this.reconnectAttempts++;
    this.reconnectDelay = Math.min(30000, this.reconnectDelay * 2); // Max 30 seconds

    console.log(
      `Reconnection attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts} in ${this.reconnectDelay}ms...`
    );

    setTimeout(() => {
      this.connect();
    }, this.reconnectDelay);
  }

  /**
   * Subscribe to extraction updates
   */
  subscribeToExtractions(handler: MessageHandler): () => void {
    return this.subscribe('/topic/extractions', handler, 'extractions');
  }

  /**
   * Subscribe to migration updates
   */
  subscribeToMigrations(handler: MessageHandler): () => void {
    return this.subscribe('/topic/migrations', handler, 'migrations');
  }

  /**
   * Subscribe to data quality updates
   */
  subscribeToDataQuality(handler: MessageHandler): () => void {
    return this.subscribe('/topic/data-quality', handler, 'data-quality');
  }

  /**
   * Generic subscribe method
   */
  private subscribe(topic: string, handler: MessageHandler, key: string): () => void {
    if (!this.client) {
      console.error('WebSocket client not initialized. Call connect() first.');
      return () => {};
    }

    // Wait for connection if not connected yet
    if (!this.client.connected) {
      console.log(`Waiting for connection before subscribing to ${topic}...`);
      const checkConnection = setInterval(() => {
        if (this.client && this.client.connected) {
          clearInterval(checkConnection);
          this.performSubscription(topic, handler, key);
        }
      }, 100);
    } else {
      this.performSubscription(topic, handler, key);
    }

    // Return unsubscribe function
    return () => this.unsubscribe(key);
  }

  /**
   * Perform actual subscription
   */
  private performSubscription(topic: string, handler: MessageHandler, key: string): void {
    if (!this.client || !this.client.connected) {
      console.warn('Cannot subscribe: client not connected');
      return;
    }

    // Unsubscribe if already subscribed
    if (this.subscriptions.has(key)) {
      this.unsubscribe(key);
    }

    const subscription = this.client.subscribe(topic, (message: IMessage) => {
      try {
        const event: StatusUpdateEvent = JSON.parse(message.body);
        console.log(`[${topic}] Received event:`, event);
        handler(event);
      } catch (error) {
        console.error(`Error parsing message from ${topic}:`, error);
      }
    });

    this.subscriptions.set(key, subscription);
    console.log(`Subscribed to ${topic}`);
  }

  /**
   * Unsubscribe from topic
   */
  private unsubscribe(key: string): void {
    const subscription = this.subscriptions.get(key);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(key);
      console.log(`Unsubscribed from ${key}`);
    }
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect(): void {
    if (this.client) {
      // Unsubscribe from all topics
      this.subscriptions.forEach((subscription) => subscription.unsubscribe());
      this.subscriptions.clear();

      this.client.deactivate();
      this.client = null;
      this.connectionStatus = 'disconnected';
      this.notifyStatusListeners('disconnected');
      console.log('WebSocket disconnected');
    }
  }

  /**
   * Get connection status
   */
  getConnectionStatus(): 'disconnected' | 'connecting' | 'connected' {
    return this.connectionStatus;
  }

  /**
   * Add connection status listener
   */
  addStatusListener(listener: (status: string) => void): () => void {
    this.statusListeners.push(listener);
    // Return function to remove listener
    return () => {
      const index = this.statusListeners.indexOf(listener);
      if (index > -1) {
        this.statusListeners.splice(index, 1);
      }
    };
  }

  /**
   * Notify all status listeners
   */
  private notifyStatusListeners(status: string): void {
    this.statusListeners.forEach((listener) => {
      try {
        listener(status);
      } catch (error) {
        console.error('Error in status listener:', error);
      }
    });
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.client !== null && this.client.connected;
  }
}

// Export singleton instance
export const websocketService = new WebSocketService();

export default websocketService;
