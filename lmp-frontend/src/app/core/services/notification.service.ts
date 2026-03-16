import {
  Injectable,
  inject,
  signal,
  OnDestroy,
  Inject,
  PLATFORM_ID,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
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

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);
  private client: Client | null = null;
  private isBrowser: boolean;

  /** Live list of notifications (most recent first) */
  readonly notifications = signal<AppNotification[]>([]);
  /** Unread count */
  readonly unreadCount = signal(0);
  /** Whether the WebSocket connection is active */
  readonly connected = signal(false);
  /** Whether initial load from API is done */
  readonly loaded = signal(false);

  private readonly apiUrl = `${environment.apiUrl}/api/v1/notifications`;

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  /**
   * Connect to WebSocket and load persisted notifications from backend.
   * Should be called once the user is authenticated.
   */
  connect(): void {
    if (!this.isBrowser) return;

    const user = this.authService.user();
    if (!user) return;

    // Load persisted notifications from API
    this.loadNotificationsFromApi();

    // Avoid duplicate connections
    if (this.client?.active) return;

    const wsBaseUrl = environment.apiUrl || window.location.origin;

    this.client = new Client({
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
   * Load persisted notifications from backend API.
   */
  private loadNotificationsFromApi(): void {
    this.http
      .get<ApiResponse<AppNotification[]>>(this.apiUrl, {
        withCredentials: true,
      })
      .subscribe({
        next: (response) => {
          if (response.success && response.data) {
            this.notifications.set(response.data);
            this.unreadCount.set(
              response.data.filter((n) => !n.read).length,
            );
          }
          this.loaded.set(true);
        },
        error: (err) => {
          console.error('Failed to load notifications from API:', err);
          this.loaded.set(true);
        },
      });
  }

  /**
   * Mark all notifications as read (persisted to backend).
   */
  markAllRead(): void {
    // Optimistic UI update
    this.notifications.update((list) =>
      list.map((n) => ({ ...n, read: true })),
    );
    this.unreadCount.set(0);

    // Persist to backend
    this.http
      .patch<ApiResponse<void>>(`${this.apiUrl}/read-all`, {}, {
        withCredentials: true,
      })
      .subscribe({
        error: (err) =>
          console.error('Failed to mark all as read:', err),
      });
  }

  /**
   * Mark a specific notification as read (persisted to backend).
   */
  markAsRead(notificationId: string): void {
    // Optimistic UI update
    this.notifications.update((list) =>
      list.map((n) =>
        n.id === notificationId ? { ...n, read: true } : n,
      ),
    );
    this.unreadCount.update((c) => Math.max(0, c - 1));

    // Persist to backend
    this.http
      .patch<ApiResponse<void>>(`${this.apiUrl}/${notificationId}/read`, {}, {
        withCredentials: true,
      })
      .subscribe({
        error: (err) =>
          console.error('Failed to mark notification as read:', err),
      });
  }

  /**
   * Clear all notifications (persisted to backend).
   */
  clearAll(): void {
    // Optimistic UI update
    this.notifications.set([]);
    this.unreadCount.set(0);

    // Persist to backend
    this.http
      .delete<ApiResponse<void>>(`${this.apiUrl}/all`, {
        withCredentials: true,
      })
      .subscribe({
        error: (err) =>
          console.error('Failed to clear all notifications:', err),
      });
  }

  /**
   * Dismiss a single notification (persisted to backend).
   */
  dismissNotification(notificationId: string): void {
    const notification = this.notifications().find(
      (n) => n.id === notificationId,
    );
    const wasUnread = notification && !notification.read;

    // Optimistic UI update
    this.notifications.update((list) =>
      list.filter((n) => n.id !== notificationId),
    );
    if (wasUnread) {
      this.unreadCount.update((c) => Math.max(0, c - 1));
    }

    // Persist to backend
    this.http
      .delete<ApiResponse<void>>(`${this.apiUrl}/${notificationId}`, {
        withCredentials: true,
      })
      .subscribe({
        error: (err) =>
          console.error('Failed to dismiss notification:', err),
      });
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
        // Use persisted ID from backend if available, otherwise generate one
        id: payload.id || crypto.randomUUID(),
        type: payload.type || 'INFO',
        message: payload.message || 'Nouvelle notification',
        orderId: payload.orderId,
        serviceName: payload.serviceName,
        amount: payload.amount,
        timestamp: payload.timestamp || new Date().toISOString(),
        read: false,
      };

      // Avoid duplicates (check if this ID already exists from initial load)
      const existing = this.notifications().find(
        (n) => n.id === notification.id,
      );
      if (existing) return;

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
