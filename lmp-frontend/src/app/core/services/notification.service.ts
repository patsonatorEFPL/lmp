import {
  Injectable,
  inject,
  signal,
  OnDestroy,
  Inject,
  PLATFORM_ID,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

export interface AppNotification {
  id: string;
  type: string;
  message: string;
  orderId?: string;
  serviceName?: string;
  amount?: number;
  timestamp: string;
  read: boolean;
}

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {
  private readonly authService = inject(AuthService);
  private client: Client | null = null;
  private isBrowser: boolean;

  /** Live list of notifications (most recent first) */
  readonly notifications = signal<AppNotification[]>([]);
  /** Unread count */
  readonly unreadCount = signal(0);
  /** Whether the WebSocket connection is active */
  readonly connected = signal(false);

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  /**
   * Connect to WebSocket and start listening for user notifications.
   * Should be called once the user is authenticated.
   */
  connect(): void {
    if (!this.isBrowser) return;

    const user = this.authService.user();
    if (!user) return;

    // Avoid duplicate connections
    if (this.client?.active) return;

    const wsBaseUrl = environment.apiUrl || window.location.origin;

    this.client = new Client({
      // Use SockJS as the transport for compatibility
      webSocketFactory: () => new SockJS(`${wsBaseUrl}/ws/notifications`),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      onConnect: () => {
        this.connected.set(true);

        // Subscribe to user-specific notification topic
        this.client!.subscribe(
          `/topic/user/${user.id}/notifications`,
          (message: IMessage) => this.handleMessage(message),
        );

        // Also subscribe to the user-scoped queue (Spring's /user prefix)
        this.client!.subscribe(
          `/user/queue/notifications`,
          (message: IMessage) => this.handleMessage(message),
        );
      },

      onDisconnect: () => {
        this.connected.set(false);
      },

      onStompError: (frame) => {
        console.error('WebSocket STOMP error:', frame.headers['message']);
        this.connected.set(false);
      },
    });

    this.client.activate();
  }

  /**
   * Disconnect the WebSocket client.
   */
  disconnect(): void {
    if (this.client?.active) {
      this.client.deactivate();
    }
    this.client = null;
    this.connected.set(false);
  }

  /**
   * Mark all notifications as read.
   */
  markAllRead(): void {
    this.notifications.update((list) =>
      list.map((n) => ({ ...n, read: true })),
    );
    this.unreadCount.set(0);
  }

  /**
   * Mark a specific notification as read.
   */
  markAsRead(notificationId: string): void {
    this.notifications.update((list) =>
      list.map((n) =>
        n.id === notificationId ? { ...n, read: true } : n,
      ),
    );
    this.unreadCount.update((c) => Math.max(0, c - 1));
  }

  /**
   * Clear all notifications.
   */
  clearAll(): void {
    this.notifications.set([]);
    this.unreadCount.set(0);
  }

  /**
   * Add a notification programmatically (for testing or HTTP-based notifications).
   */
  addNotification(payload: {
    type?: string;
    message?: string;
    orderId?: string;
    serviceName?: string;
    amount?: number;
  }): void {
    const notification: AppNotification = {
      id: crypto.randomUUID(),
      type: payload.type || 'INFO',
      message: payload.message || 'Nouvelle notification',
      orderId: payload.orderId,
      serviceName: payload.serviceName,
      amount: payload.amount,
      timestamp: new Date().toISOString(),
      read: false,
    };

    this.notifications.update((list) =>
      [notification, ...list].slice(0, 50),
    );
    this.unreadCount.update((c) => c + 1);
  }

  private handleMessage(message: IMessage): void {
    try {
      const payload = JSON.parse(message.body);

      const notification: AppNotification = {
        id: crypto.randomUUID(),
        type: payload.type || 'INFO',
        message: payload.message || 'Nouvelle notification',
        orderId: payload.orderId,
        serviceName: payload.serviceName,
        amount: payload.amount,
        timestamp: payload.timestamp || new Date().toISOString(),
        read: false,
      };

      // Prepend to the list (most recent first), max 50 notifications
      this.notifications.update((list) =>
        [notification, ...list].slice(0, 50),
      );
      this.unreadCount.update((c) => c + 1);
    } catch (e) {
      console.error('Failed to parse WebSocket notification:', e);
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
