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
  /** Id métier pour dédoublonnage (égal à {@code eventId} SSE si fourni). */
  eventId?: string;
}

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}

/**
 * Reconnect delay constants for exponential backoff.
 */
const INITIAL_RECONNECT_DELAY = 1000;
const MAX_RECONNECT_DELAY = 30000;

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);
  private eventSource: EventSource | null = null;
  private isBrowser: boolean;
  private reconnectDelay = INITIAL_RECONNECT_DELAY;
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null;

  /** Live list of notifications (most recent first) */
  readonly notifications = signal<AppNotification[]>([]);
  /** Unread count */
  readonly unreadCount = signal(0);
  /** Whether the SSE connection is active */
  readonly connected = signal(false);
  /** Whether initial load from API is done */
  readonly loaded = signal(false);
  /** Activité récente liée aux commandes (pour badge menu). */
  readonly liveOrderHint = signal(0);
  /** Activité récente liée aux RDV (pour badge menu). */
  readonly liveAppointmentHint = signal(0);

  private readonly apiUrl = `${environment.apiUrl}/api/v1/notifications`;
  private readonly sseUrl = `${environment.apiUrl}/api/v1/sse/notifications`;

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  /**
   * Connect to SSE and load persisted notifications from backend.
   * Should be called once the user is authenticated.
   */
  connect(): void {
    if (!this.isBrowser) return;

    const user = this.authService.user();
    if (!user) return;

    // Load persisted notifications from API (guard against duplicate calls)
    if (!this.loaded()) {
      this.loadNotificationsFromApi();
    }

    // Avoid duplicate connections
    if (this.eventSource) return;

    this.createEventSource();
  }

  /**
   * Creates the EventSource connection to the SSE endpoint.
   * EventSource sends cookies automatically (same-origin), so session auth works natively.
   */
  private createEventSource(): void {
    // Construct SSE URL — use same origin for relative paths
    const url = this.sseUrl || '/api/v1/sse/notifications';

    this.eventSource = new EventSource(url, { withCredentials: true });

    // Listen for user notification events
    this.eventSource.addEventListener('notification', (event: MessageEvent) => {
      this.handleSseMessage(event.data);
    });

    this.eventSource.onopen = () => {
      this.connected.set(true);
      this.reconnectDelay = INITIAL_RECONNECT_DELAY; // Reset backoff on success
    };

    this.eventSource.onerror = () => {
      this.connected.set(false);

      // EventSource auto-reconnects for network errors, but if the connection
      // is closed (readyState === CLOSED), we need to reconnect manually.
      if (this.eventSource?.readyState === EventSource.CLOSED) {
        this.eventSource.close();
        this.eventSource = null;
        this.scheduleReconnect();
      }
    };
  }

  /**
   * Schedules a reconnection with exponential backoff.
   */
  private scheduleReconnect(): void {
    if (this.reconnectTimeout) return;

    // Only reconnect if user is still authenticated
    if (!this.authService.user()) return;

    this.reconnectTimeout = setTimeout(() => {
      this.reconnectTimeout = null;
      this.createEventSource();
    }, this.reconnectDelay);

    // Exponential backoff, capped at max
    this.reconnectDelay = Math.min(this.reconnectDelay * 2, MAX_RECONNECT_DELAY);
  }

  /**
   * Disconnect the SSE client.
   */
  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    this.connected.set(false);
    this.reconnectDelay = INITIAL_RECONNECT_DELAY;
  }

  /**
   * Full teardown: disconnect SSE and purge all notification state.
   * Must be called on logout to prevent stale data leaking to the public UI.
   */
  reset(): void {
    this.disconnect();
    this.notifications.set([]);
    this.unreadCount.set(0);
    this.loaded.set(false);
    this.liveOrderHint.set(0);
    this.liveAppointmentHint.set(0);
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

  /**
   * Handle incoming SSE notification message.
   */
  private handleSseMessage(data: string): void {
    try {
      const payload = JSON.parse(data);

      const evName = typeof payload.event === 'string' ? payload.event : '';

      const eventId =
        typeof payload.eventId === 'string' ? payload.eventId : undefined;
      const notification: AppNotification = {
        // Use persisted ID from backend if available, otherwise generate one
        id: payload.id || eventId || crypto.randomUUID(),
        type: payload.type || 'INFO',
        message: payload.message || 'Nouvelle notification',
        orderId: payload.orderId,
        serviceName: payload.serviceName,
        amount: payload.amount,
        timestamp: payload.timestamp || new Date().toISOString(),
        read: false,
        eventId,
      };

      // Hints drive list reload (user orders / RDV). Must run even when we skip
      // adding a duplicate (same persisted id as REST already loaded).
      if (evName.startsWith('order:')) {
        this.liveOrderHint.update((n) => n + 1);
      }
      if (evName.startsWith('appointment:')) {
        this.liveAppointmentHint.update((n) => n + 1);
      }

      // Avoid duplicates (DB id or event bus id)
      const existing = this.notifications().find(
        (n) =>
          n.id === notification.id ||
          (eventId != null && n.eventId === eventId) ||
          (eventId != null && n.id === eventId),
      );
      if (existing) return;

      // Prepend to the list (most recent first), max 50 notifications
      this.notifications.update((list) =>
        [notification, ...list].slice(0, 50),
      );
      this.unreadCount.update((c) => c + 1);
    } catch (e) {
      console.error('Failed to parse SSE notification:', e);
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
