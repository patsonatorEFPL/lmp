import {
  Component,
  inject,
  signal,
  OnInit,
  OnDestroy,
  Inject,
  PLATFORM_ID,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  Bell,
  X,
  ShoppingCart,
  CreditCard,
  CheckCircle,
  RefreshCw,
  AlertCircle,
} from 'lucide-angular';
import {
  NotificationService,
  AppNotification,
} from '../../core/services/notification.service';
import { AuthService } from '../../core/services/auth.service';

interface ToastWithProgress extends AppNotification {
  progressPercent: number;
}

@Component({
  selector: 'lmp-notification-toast',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  template: `
    <!-- Floating toasts for new notifications -->
    @for (toast of visibleToasts(); track toast.id; let i = $index) {
      <div
        class="fixed right-4 z-[250] w-[21rem] sm:w-[22rem] overflow-hidden rounded-xl border bg-(--card) shadow-2xl backdrop-blur-sm notification-toast-enter"
        [class]="getToastBorderClass(toast.type)"
        [style.bottom.px]="80 + i * 90"
      >
        <!-- Color accent bar at top -->
        <div class="h-0.5" [class]="getToastAccentBg(toast.type)"></div>

        <div class="flex items-start gap-3 p-4">
          <!-- Animated icon -->
          <div
            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl notification-icon-pulse"
            [class]="getToastIconBg(toast.type)"
          >
            @switch (toast.type) {
              @case ('NEW_PENDING_ORDER') {
                <lucide-icon
                  [img]="ShoppingCartIcon"
                  [size]="18"
                ></lucide-icon>
              }
              @case ('PAYMENT_SUCCESS') {
                <lucide-icon
                  [img]="CreditCardIcon"
                  [size]="18"
                ></lucide-icon>
              }
              @case ('STATUS_CHANGED') {
                <lucide-icon
                  [img]="RefreshCwIcon"
                  [size]="18"
                ></lucide-icon>
              }
              @case ('REFUND') {
                <lucide-icon
                  [img]="AlertCircleIcon"
                  [size]="18"
                ></lucide-icon>
              }
              @default {
                <lucide-icon [img]="BellIcon" [size]="18"></lucide-icon>
              }
            }
          </div>

          <!-- Content -->
          <div class="flex-1 min-w-0">
            <p class="text-sm font-bold text-(--foreground)">
              {{ getNotificationTitle(toast.type) }}
            </p>
            <p
              class="mt-0.5 text-xs text-(--muted-foreground) line-clamp-2"
            >
              {{ toast.message }}
            </p>
            @if (toast.orderId) {
              <a
                routerLink="/dashboard"
                class="mt-1.5 inline-flex items-center gap-1 text-xs font-semibold text-(--primary) hover:underline"
                (click)="dismissToast(toast.id)"
              >
                Voir la commande →
              </a>
            }
          </div>

          <!-- Close button -->
          <button
            class="shrink-0 mt-0.5 cursor-pointer rounded-md p-1 text-(--muted-foreground) hover:bg-(--muted) hover:text-(--foreground) transition-colors"
            (click)="dismissToast(toast.id)"
          >
            <lucide-icon [img]="XIcon" [size]="14"></lucide-icon>
          </button>
        </div>

        <!-- Progress bar for auto-dismiss countdown -->
        <div class="h-1 w-full bg-(--muted)/30">
          <div
            class="h-full transition-all duration-100 ease-linear rounded-r-full"
            [class]="getToastAccentBg(toast.type)"
            [style.width.%]="toast.progressPercent"
          ></div>
        </div>
      </div>
    }

    <!-- Notification bell FAB (when there are unread and no toasts visible) -->
    @if (
      notificationService.unreadCount() > 0 &&
      visibleToasts().length === 0
    ) {
      <button
        class="fixed right-4 bottom-4 z-[200] flex h-14 w-14 cursor-pointer items-center justify-center rounded-full bg-(--primary) text-white shadow-lg shadow-(--primary)/30 transition-all hover:scale-110 hover:shadow-xl hover:shadow-(--primary)/40 notification-bell-ring"
        (click)="showLatestNotification()"
      >
        <lucide-icon [img]="BellIcon" [size]="20"></lucide-icon>
        <span
          class="absolute -top-1 -right-1 flex h-6 w-6 items-center justify-center rounded-full bg-red-500 text-[11px] font-bold text-white ring-2 ring-(--card)"
        >
          {{
            notificationService.unreadCount() > 9
              ? '9+'
              : notificationService.unreadCount()
          }}
        </span>
      </button>
    }
  `,
  styles: `
    :host {
      display: contents;
    }
  `,
})
export class NotificationToastComponent implements OnInit, OnDestroy {
  readonly notificationService = inject(NotificationService);
  private readonly authService = inject(AuthService);
  private isBrowser: boolean;

  readonly BellIcon = Bell;
  readonly XIcon = X;
  readonly ShoppingCartIcon = ShoppingCart;
  readonly CreditCardIcon = CreditCard;
  readonly CheckCircleIcon = CheckCircle;
  readonly RefreshCwIcon = RefreshCw;
  readonly AlertCircleIcon = AlertCircle;

  readonly visibleToasts = signal<ToastWithProgress[]>([]);
  private toastTimeouts = new Map<string, ReturnType<typeof setTimeout>>();
  private progressIntervals = new Map<
    string,
    ReturnType<typeof setInterval>
  >();
  private lastNotificationCount = 0;
  private _rafId = 0;

  private readonly TOAST_DURATION = 8000; // 8 seconds
  private readonly PROGRESS_INTERVAL = 50; // update every 50ms

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    if (!this.isBrowser) return;

    // Connect to WebSocket if user is authenticated
    if (this.authService.isAuthenticated()) {
      this.notificationService.connect();
    }

    this.lastNotificationCount =
      this.notificationService.notifications().length;
    this.pollForNewNotifications();
  }

  private pollForNewNotifications(): void {
    if (!this.isBrowser) return;

    const check = () => {
      const current = this.notificationService.notifications();
      if (current.length > this.lastNotificationCount) {
        const newOnes = current.slice(
          0,
          current.length - this.lastNotificationCount,
        );
        for (const n of newOnes) {
          this.showToast(n);
          this.playNotificationSound();
        }
      }
      this.lastNotificationCount = current.length;
      this._rafId = requestAnimationFrame(check);
    };
    this._rafId = requestAnimationFrame(check);
  }

  showToast(notification: AppNotification): void {
    const toastWithProgress: ToastWithProgress = {
      ...notification,
      progressPercent: 100,
    };

    this.visibleToasts.update((list) => {
      return [toastWithProgress, ...list].slice(0, 3);
    });

    // Start progress countdown
    const startTime = Date.now();
    const interval = setInterval(() => {
      const elapsed = Date.now() - startTime;
      const remaining = Math.max(
        0,
        ((this.TOAST_DURATION - elapsed) / this.TOAST_DURATION) * 100,
      );
      this.visibleToasts.update((list) =>
        list.map((t) =>
          t.id === notification.id
            ? { ...t, progressPercent: remaining }
            : t,
        ),
      );
      if (remaining <= 0) {
        clearInterval(interval);
      }
    }, this.PROGRESS_INTERVAL);
    this.progressIntervals.set(notification.id, interval);

    // Auto-dismiss after duration
    const timeout = setTimeout(() => {
      this.dismissToast(notification.id);
    }, this.TOAST_DURATION);
    this.toastTimeouts.set(notification.id, timeout);
  }

  dismissToast(id: string): void {
    this.visibleToasts.update((list) => list.filter((t) => t.id !== id));
    this.notificationService.markAsRead(id);

    const timeout = this.toastTimeouts.get(id);
    if (timeout) {
      clearTimeout(timeout);
      this.toastTimeouts.delete(id);
    }

    const interval = this.progressIntervals.get(id);
    if (interval) {
      clearInterval(interval);
      this.progressIntervals.delete(id);
    }
  }

  showLatestNotification(): void {
    const unread = this.notificationService
      .notifications()
      .filter((n) => !n.read);
    if (unread.length > 0) {
      this.showToast(unread[0]);
    }
    this.notificationService.markAllRead();
  }

  getNotificationTitle(type: string): string {
    switch (type) {
      case 'NEW_PENDING_ORDER':
        return '🛒 Nouvelle commande';
      case 'PAYMENT_SUCCESS':
        return '💳 Paiement confirmé';
      case 'STATUS_CHANGED':
        return '🔄 Statut mis à jour';
      case 'REFUND':
        return '↩️ Remboursement';
      default:
        return '🔔 Notification';
    }
  }

  getToastBorderClass(type: string): string {
    switch (type) {
      case 'NEW_PENDING_ORDER':
        return 'border-blue-500/30';
      case 'PAYMENT_SUCCESS':
        return 'border-emerald-500/30';
      case 'STATUS_CHANGED':
        return 'border-amber-500/30';
      case 'REFUND':
        return 'border-violet-500/30';
      default:
        return 'border-(--primary)/30';
    }
  }

  getToastAccentBg(type: string): string {
    switch (type) {
      case 'NEW_PENDING_ORDER':
        return 'bg-blue-500';
      case 'PAYMENT_SUCCESS':
        return 'bg-emerald-500';
      case 'STATUS_CHANGED':
        return 'bg-amber-500';
      case 'REFUND':
        return 'bg-violet-500';
      default:
        return 'bg-(--primary)';
    }
  }

  getToastIconBg(type: string): string {
    switch (type) {
      case 'NEW_PENDING_ORDER':
        return 'bg-blue-500/10 text-blue-500';
      case 'PAYMENT_SUCCESS':
        return 'bg-emerald-500/10 text-emerald-500';
      case 'STATUS_CHANGED':
        return 'bg-amber-500/10 text-amber-500';
      case 'REFUND':
        return 'bg-violet-500/10 text-violet-500';
      default:
        return 'bg-(--primary)/10 text-(--primary)';
    }
  }

  private playNotificationSound(): void {
    if (!this.isBrowser) return;
    try {
      const audioCtx = new (window.AudioContext ||
        (window as any).webkitAudioContext)();
      // Play a pleasant two-tone chime
      const playTone = (freq: number, startTime: number, duration: number) => {
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.connect(gain);
        gain.connect(audioCtx.destination);
        osc.frequency.value = freq;
        osc.type = 'sine';
        gain.gain.setValueAtTime(0, startTime);
        gain.gain.linearRampToValueAtTime(0.08, startTime + 0.02);
        gain.gain.exponentialRampToValueAtTime(
          0.001,
          startTime + duration,
        );
        osc.start(startTime);
        osc.stop(startTime + duration);
      };
      const now = audioCtx.currentTime;
      playTone(880, now, 0.15); // A5
      playTone(1174.66, now + 0.1, 0.2); // D6
    } catch {
      // AudioContext not available — silent fallback
    }
  }

  ngOnDestroy(): void {
    if (this._rafId) {
      cancelAnimationFrame(this._rafId);
    }
    for (const timeout of this.toastTimeouts.values()) {
      clearTimeout(timeout);
    }
    for (const interval of this.progressIntervals.values()) {
      clearInterval(interval);
    }
    this.notificationService.disconnect();
  }
}
