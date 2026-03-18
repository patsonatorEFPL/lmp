import {
  Component,
  inject,
  signal,
  effect,
  OnInit,
  OnDestroy,
  Inject,
  PLATFORM_ID,
  DestroyRef,
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
import { environment } from '../../../environments/environment';

interface ToastWithProgress extends AppNotification {
  progressPercent: number;
  paused: boolean;
  exiting: boolean;
}

@Component({
  selector: 'lmp-notification-toast',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  template: `
    <!-- Floating toasts for new notifications -->
    @for (toast of visibleToasts(); track toast.id; let i = $index) {
      <div
        class="fixed right-4 z-[250] w-[21rem] sm:w-[22rem] overflow-hidden rounded-xl border bg-(--card)/95 backdrop-blur-md shadow-2xl"
        [class]="getToastClasses(toast)"
        [style.bottom.px]="96 + i * 94"
        (mouseenter)="pauseToast(toast.id)"
        (mouseleave)="resumeToast(toast.id)"
        role="alert"
        aria-live="assertive"
      >
        <!-- Color accent bar at top -->
        <div class="h-1" [class]="getToastAccentBg(toast.type)"></div>

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
            aria-label="Fermer la notification"
          >
            <lucide-icon [img]="XIcon" [size]="14"></lucide-icon>
          </button>
        </div>

        <!-- Progress bar for auto-dismiss countdown -->
        <div class="h-1 w-full bg-(--muted)/30">
          <div
            class="h-full rounded-r-full"
            [class]="getProgressBarClasses(toast)"
            [style.width.%]="toast.progressPercent"
          ></div>
        </div>
      </div>
    }

    <!-- FAB bell removed — notifications are accessed via the navbar bell icon -->
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

    // Reactively connect/disconnect when auth state changes.
    // This fixes the bug where logging in after app bootstrap would never
    // trigger connect() because ngOnInit had already run with isAuthenticated=false.
    if (this.isBrowser) {
      effect(() => {
        const authenticated = this.authService.isAuthenticated();
        if (authenticated) {
          this.notificationService.connect();
          this.waitForLoadThenPoll();
        } else {
          this.notificationService.disconnect();
          this.stopPolling();
        }
      });
    }
  }

  ngOnInit(): void {
    if (!this.isBrowser) return;

    // Expose test function globally only in non-production
    if (!environment.production) {
      (window as any).__lmpTestNotification = (
        type?: string,
        message?: string,
      ) => {
        const types = [
          'PAYMENT_SUCCESS',
          'NEW_PENDING_ORDER',
          'STATUS_CHANGED',
          'REFUND',
        ];
        const messages: Record<string, string> = {
          PAYMENT_SUCCESS:
            'Le paiement de 250,00€ pour "Création de site web" a été confirmé.',
          NEW_PENDING_ORDER:
            'Nouvelle commande #4521 reçue pour "SEO Local".',
          STATUS_CHANGED:
            'La commande "Audit technique" est en cours de traitement.',
          REFUND:
            'Remboursement de 75,00€ traité pour la commande #3210.',
        };
        const t = type || types[Math.floor(Math.random() * types.length)];
        this.notificationService.addNotification({
          type: t,
          message: message || messages[t] || 'Notification de test',
          orderId: 'test-' + Date.now(),
          serviceName: 'Service Test',
          amount: 100,
        });
      };
    }
  }

  /**
   * Wait until the initial API load completes, then snapshot the count
   * so that only truly new (real-time) notifications trigger toasts.
   */
  private waitForLoadThenPoll(): void {
    const waitCheck = () => {
      if (this.notificationService.loaded()) {
        // Snapshot current count — these are persisted, not new
        this.lastNotificationCount =
          this.notificationService.notifications().length;
        this.pollForNewNotifications();
      } else {
        requestAnimationFrame(waitCheck);
      }
    };
    requestAnimationFrame(waitCheck);
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
      paused: false,
      exiting: false,
    };

    this.visibleToasts.update((list) => {
      return [toastWithProgress, ...list].slice(0, 3);
    });

    // Start progress countdown
    let startTime = Date.now();
    let elapsedBeforePause = 0;

    const interval = setInterval(() => {
      const toast = this.visibleToasts().find((t) => t.id === notification.id);
      if (toast?.paused) return;

      const elapsed = elapsedBeforePause + (Date.now() - startTime);
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

    // Store pause tracking data
    (this as any)[`_pauseData_${notification.id}`] = {
      startTime,
      elapsedBeforePause: 0,
      get elapsed() {
        return this.elapsedBeforePause + (Date.now() - this.startTime);
      },
    };

    // Auto-dismiss after duration
    const timeout = setTimeout(() => {
      this.startExitAnimation(notification.id);
    }, this.TOAST_DURATION);
    this.toastTimeouts.set(notification.id, timeout);
  }

  /** Pause toast countdown on hover */
  pauseToast(id: string): void {
    const pauseData = (this as any)[`_pauseData_${id}`];
    if (pauseData) {
      pauseData.elapsedBeforePause += Date.now() - pauseData.startTime;
    }

    this.visibleToasts.update((list) =>
      list.map((t) => (t.id === id ? { ...t, paused: true } : t)),
    );

    // Clear the auto-dismiss timeout
    const timeout = this.toastTimeouts.get(id);
    if (timeout) {
      clearTimeout(timeout);
      this.toastTimeouts.delete(id);
    }
  }

  /** Resume toast countdown when mouse leaves */
  resumeToast(id: string): void {
    const pauseData = (this as any)[`_pauseData_${id}`];
    if (pauseData) {
      pauseData.startTime = Date.now();
    }

    this.visibleToasts.update((list) =>
      list.map((t) => (t.id === id ? { ...t, paused: false } : t)),
    );

    // Restart timeout for remaining time
    const toast = this.visibleToasts().find((t) => t.id === id);
    if (toast) {
      const remainingMs = (toast.progressPercent / 100) * this.TOAST_DURATION;
      const timeout = setTimeout(() => {
        this.startExitAnimation(id);
      }, remainingMs);
      this.toastTimeouts.set(id, timeout);
    }
  }

  /** Start exit animation before removing */
  private startExitAnimation(id: string): void {
    this.visibleToasts.update((list) =>
      list.map((t) => (t.id === id ? { ...t, exiting: true } : t)),
    );

    // Remove after animation completes
    setTimeout(() => {
      this.removeToast(id);
    }, 350);
  }

  dismissToast(id: string): void {
    this.startExitAnimation(id);
  }

  private removeToast(id: string): void {
    this.visibleToasts.update((list) => list.filter((t) => t.id !== id));

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

    delete (this as any)[`_pauseData_${id}`];
  }

  showLatestNotification(): void {
    const unread = this.notificationService
      .notifications()
      .filter((n) => !n.read);
    if (unread.length > 0) {
      this.showToast(unread[0]);
    }
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

  getToastClasses(toast: ToastWithProgress): string {
    const border = this.getToastBorderClass(toast.type);
    const anim = toast.exiting
      ? 'notification-toast-exit'
      : 'notification-toast-enter';
    return `${border} ${anim}`;
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

  getProgressBarClasses(toast: ToastWithProgress): string {
    const bg = this.getToastAccentBg(toast.type);
    const transition = toast.paused
      ? ''
      : 'transition-all duration-100 ease-linear';
    return `${bg} ${transition}`;
  }

  private playNotificationSound(): void {
    if (!this.isBrowser) return;
    try {
      const audioCtx = new (window.AudioContext ||
        (window as any).webkitAudioContext)();
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

  private stopPolling(): void {
    if (this._rafId) {
      cancelAnimationFrame(this._rafId);
      this._rafId = 0;
    }
    this.lastNotificationCount = 0;
  }

  ngOnDestroy(): void {
    this.stopPolling();
    for (const timeout of this.toastTimeouts.values()) {
      clearTimeout(timeout);
    }
    for (const interval of this.progressIntervals.values()) {
      clearInterval(interval);
    }
    this.notificationService.disconnect();

    // Cleanup global test function
    if (!environment.production && this.isBrowser) {
      delete (window as any).__lmpTestNotification;
    }
  }
}
