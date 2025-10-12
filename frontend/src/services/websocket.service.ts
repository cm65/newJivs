import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * WebSocket service for real-time updates using STOMP over SockJS
 * Part of Sprint 2 - Workflow 5: WebSocket Real-time Updates
 */
class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000;
  private isConnecting = false;

  /**
   * Connect to WebSocket server
   */
  connect(): Promise<void> {
    if (this.client?.connected) {
      console.log('WebSocket already connected');
      return Promise.resolve();
    }

    if (this.isConnecting) {
      console.log('WebSocket connection already in progress');
      return Promise.resolve();
    }

    this.isConnecting = true;

    return new Promise((resolve, reject) => {
      const token = localStorage.getItem('accessToken');
      const wsUrl = process.env.REACT_APP_WS_URL || 'http://localhost:8080/ws';

      this.client = new Client({
        webSocketFactory: () => new SockJS(wsUrl) as any,
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        debug: (str) => {
          if (process.env.NODE_ENV === 'development') {
            console.log('[STOMP Debug]', str);
          }
        },
        reconnectDelay: this.reconnectDelay,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('WebSocket connected successfully');
          this.reconnectAttempts = 0;
          this.isConnecting = false;
          resolve();
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame);
          this.isConnecting = false;
          reject(new Error('WebSocket connection failed'));
        },
        onWebSocketClose: () => {
          console.log('WebSocket connection closed');
          this.isConnecting = false;
          this.handleReconnect();
        },
      });

      this.client.activate();
    });
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect(): void {
    if (this.client) {
      this.subscriptions.forEach((subscription) => subscription.unsubscribe());
      this.subscriptions.clear();
      this.client.deactivate();
      this.client = null;
      console.log('WebSocket disconnected');
    }
  }

  /**
   * Handle reconnection logic
   */
  private handleReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
      setTimeout(() => {
        this.connect().catch((error) => {
          console.error('Reconnection failed:', error);
        });
      }, this.reconnectDelay);
    } else {
      console.error('Max reconnection attempts reached');
    }
  }

  /**
   * Subscribe to extraction status updates
   * @param jobId Extraction job ID
   * @param callback Callback function to handle updates
   * @returns Subscription key for unsubscribing
   */
  subscribeToExtractionUpdates(
    jobId: string,
    callback: (update: any) => void
  ): string {
    const destination = `/topic/extraction/${jobId}`;
    return this.subscribe(destination, callback);
  }

  /**
   * Subscribe to migration status updates
   * @param jobId Migration job ID
   * @param callback Callback function to handle updates
   * @returns Subscription key for unsubscribing
   */
  subscribeToMigrationUpdates(
    jobId: string,
    callback: (update: any) => void
  ): string {
    const destination = `/topic/migration/${jobId}`;
    return this.subscribe(destination, callback);
  }

  /**
   * Subscribe to all extraction updates (for listing page)
   * @param callback Callback function to handle updates
   * @returns Subscription key for unsubscribing
   */
  subscribeToAllExtractions(callback: (update: any) => void): string {
    const destination = '/topic/extractions';
    return this.subscribe(destination, callback);
  }

  /**
   * Subscribe to all migration updates (for listing page)
   * @param callback Callback function to handle updates
   * @returns Subscription key for unsubscribing
   */
  subscribeToAllMigrations(callback: (update: any) => void): string {
    const destination = '/topic/migrations';
    return this.subscribe(destination, callback);
  }

  /**
   * Subscribe to a specific destination
   * @param destination STOMP destination
   * @param callback Callback function to handle messages
   * @returns Subscription key for unsubscribing
   */
  private subscribe(destination: string, callback: (update: any) => void): string {
    if (!this.client?.connected) {
      console.warn('WebSocket not connected, attempting to connect...');
      this.connect().then(() => {
        this.doSubscribe(destination, callback);
      });
      return destination;
    }

    return this.doSubscribe(destination, callback);
  }

  /**
   * Perform the actual subscription
   */
  private doSubscribe(destination: string, callback: (update: any) => void): string {
    if (!this.client) {
      console.error('WebSocket client not initialized');
      return destination;
    }

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const update = JSON.parse(message.body);
        callback(update);
      } catch (error) {
        console.error('Error parsing WebSocket message:', error);
      }
    });

    this.subscriptions.set(destination, subscription);
    console.log(`Subscribed to ${destination}`);

    return destination;
  }

  /**
   * Unsubscribe from a destination
   * @param subscriptionKey Subscription key returned from subscribe methods
   */
  unsubscribe(subscriptionKey: string): void {
    const subscription = this.subscriptions.get(subscriptionKey);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(subscriptionKey);
      console.log(`Unsubscribed from ${subscriptionKey}`);
    }
  }

  /**
   * Check if WebSocket is connected
   */
  isConnected(): boolean {
    return this.client?.connected || false;
  }

  /**
   * Send a message to a destination
   * @param destination STOMP destination
   * @param body Message body
   */
  send(destination: string, body: any): void {
    if (!this.client?.connected) {
      console.error('Cannot send message: WebSocket not connected');
      return;
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body),
    });
  }
}

// Export singleton instance
export const websocketService = new WebSocketService();
export default websocketService;
