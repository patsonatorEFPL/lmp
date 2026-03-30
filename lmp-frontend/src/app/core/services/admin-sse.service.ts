import {
  Injectable,
  inject,
  signal,
  OnDestroy,
  Inject,
  PLATFORM_ID,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

const INITIAL_RECONNECT_DELAY = 1000;
const MAX_RECONNECT_DELAY = 30000;
const TOAST_MS = 8000;

/**
 * SSE admin : flux /api/v1/sse/admin/events (événement unifié {@code lmp-admin} + legacy).
 */
@Injectable({ providedIn: 'root' })
export class AdminSseService implements OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly isBrowser: boolean;
  private eventSource: EventSource | null = null;
  private reconnectDelay = INITIAL_RECONNECT_DELAY;
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
  private toastTimeout: ReturnType<typeof setTimeout> | null = null;

  readonly connected = signal(false);
  readonly badgeOrders = signal(0);
  readonly badgeAppointments = signal(0);
  readonly badgeUsers = signal(0);
  readonly lastToast = signal<{ title: string; message: string } | null>(null);

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  connect(): void {
    if (!this.isBrowser) return;
    if (!this.authService.isAdmin()) return;
    if (this.eventSource) return;
    this.openEventSource();
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    if (this.toastTimeout) {
      clearTimeout(this.toastTimeout);
      this.toastTimeout = null;
    }
    this.connected.set(false);
    this.reconnectDelay = INITIAL_RECONNECT_DELAY;
  }

  resetBadges(): void {
    this.badgeOrders.set(0);
    this.badgeAppointments.set(0);
    this.badgeUsers.set(0);
  }

  private openEventSource(): void {
    const url = `${environment.apiUrl}/api/v1/sse/admin/events`;
    this.eventSource = new EventSource(url, { withCredentials: true });

    this.eventSource.onopen = () => {
      this.connected.set(true);
      this.reconnectDelay = INITIAL_RECONNECT_DELAY;
    };

    this.eventSource.addEventListener('lmp-admin', (e: MessageEvent) => {
      this.handlePayload(this.safeParse(e.data));
    });

    this.eventSource.addEventListener('admin-order', (e: MessageEvent) => {
      this.handlePayload(this.safeParse(e.data));
    });

    this.eventSource.addEventListener('admin-stats', () => {
      this.connected.set(true);
    });

    this.eventSource.addEventListener('admin-dashboard', () => {
      this.connected.set(true);
    });

    this.eventSource.onerror = () => {
      this.connected.set(false);
      if (this.eventSource?.readyState === EventSource.CLOSED) {
        this.eventSource.close();
        this.eventSource = null;
        this.scheduleReconnect();
      }
    };
  }

  private safeParse(data: string): Record<string, unknown> {
    try {
      return JSON.parse(data) as Record<string, unknown>;
    } catch {
      return {};
    }
  }

  private handlePayload(data: Record<string, unknown>): void {
    const ev = typeof data['event'] === 'string' ? (data['event'] as string) : '';
    const message =
      typeof data['message'] === 'string'
        ? (data['message'] as string)
        : JSON.stringify(data).slice(0, 120);

    if (ev.startsWith('order:')) {
      this.badgeOrders.update((n) => n + 1);
    } else if (ev.startsWith('appointment:')) {
      this.badgeAppointments.update((n) => n + 1);
    } else if (ev.startsWith('user:')) {
      this.badgeUsers.update((n) => n + 1);
    } else if (
      ev.startsWith('payment:') ||
      ev.startsWith('refund:') ||
      ev.startsWith('invoice:') ||
      ev.startsWith('review:')
    ) {
      this.badgeOrders.update((n) => n + 1);
    }

    const title = ev || (typeof data['type'] === 'string' ? (data['type'] as string) : 'Admin');
    this.showToast(title, message);
  }

  private showToast(title: string, message: string): void {
    if (this.toastTimeout) {
      clearTimeout(this.toastTimeout);
    }
    this.lastToast.set({ title, message });
    this.toastTimeout = setTimeout(() => {
      this.lastToast.set(null);
      this.toastTimeout = null;
    }, TOAST_MS);
  }

  private scheduleReconnect(): void {
    if (!this.isBrowser) return;
    if (!this.authService.isAdmin()) return;
    if (this.reconnectTimeout) return;

    this.reconnectTimeout = setTimeout(() => {
      this.reconnectTimeout = null;
      this.openEventSource();
    }, this.reconnectDelay);

    this.reconnectDelay = Math.min(this.reconnectDelay * 2, MAX_RECONNECT_DELAY);
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
