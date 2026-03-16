import { Component, inject, signal, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, Bell, X, ShoppingCart, CreditCard, CheckCircle } from 'lucide-angular';
import { NotificationService, AppNotification } from '../../core/services/notification.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'lmp-notification-toast',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  template: `
    <!-- Floating toast for new notifications -->
    @for (toast of visibleToasts(); track toast.id) {
      <div
        class="fixed right-4 z-[250] flex w-80 items-start gap-3 rounded-xl border border-(--primary)/30 bg-(--card) p-4 shadow-2xl shadow-(--primary)/10 backdrop-blur-sm notification-toast-enter"
        [style.bottom.px]="80 + $index * 80"
      >
        <!-- Icon -->
        <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-(--primary)/10 text-(--primary)">
          @switch (toast.type) {
            @case ('NEW_PENDING_ORDER') {
              <lucide-icon [img]="ShoppingCartIcon" [size]="16"></lucide-icon>
            }
            @case ('PAYMENT_SUCCESS') {
              <lucide-icon [img]="CreditCardIcon" [size]="16"></lucide-icon>
            }
            @default {
              <lucide-icon [img]="BellIcon" [size]="16"></lucide-icon>
            }
          }
        </div>

        <!-- Content -->
        <div class="flex-1 min-w-0">
          <p class="text-sm font-semibold text-(--foreground)">
            {{ getNotificationTitle(toast.type) }}
          </p>
          <p class="mt-0.5 text-xs text-(--muted-foreground) line-clamp-2">
            {{ toast.message }}
          </p>
          @if (toast.orderId) {
            <a
              routerLink="/dashboard"
              class="mt-1.5 inline-flex text-xs font-medium text-(--primary) hover:underline"
              (click)="dismissToast(toast.id)"
            >
              Voir la commande →
            </a>
          }
        </div>

        <!-- Close -->
        <button
          class="shrink-0 cursor-pointer text-(--muted-foreground) hover:text-(--foreground) transition-colors"
          (click)="dismissToast(toast.id)"
        >
          <lucide-icon [img]="XIcon" [size]="14"></lucide-icon>
        </button>
      </div>
    }

    <!-- Notification bell indicator (shown in bottom-right as a small FAB when there are unread notifications) -->
    @if (notificationService.unreadCount() > 0 && visibleToasts().length === 0) {
      <button
        class="fixed right-4 bottom-4 z-[200] flex h-12 w-12 cursor-pointer items-center justify-center rounded-full bg-(--primary) text-white shadow-lg shadow-(--primary)/25 transition-transform hover:scale-110"
        (click)="showLatestNotification()"
      >
        <lucide-icon [img]="BellIcon" [size]="18"></lucide-icon>
        <span class="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white">
          {{ notificationService.unreadCount() > 9 ? '9+' : notificationService.unreadCount() }}
        </span>
      </button>
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

  readonly visibleToasts = signal<AppNotification[]>([]);
  private toastTimeouts = new Map<string, ReturnType<typeof setTimeout>>();
  private lastNotificationCount = 0;

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    if (!this.isBrowser) return;

    // Connect to WebSocket if user is authenticated
    if (this.authService.isAuthenticated()) {
      this.notificationService.connect();
    }

    // Watch for new notifications by polling the signal
    this.lastNotificationCount = this.notificationService.notifications().length;
    this.pollForNewNotifications();
  }

  /**
   * Poll the notifications signal to detect new additions and show toasts.
   * Using requestAnimationFrame loop since we can't use effect() in a non-signal context easily.
   */
  private pollForNewNotifications(): void {
    if (!this.isBrowser) return;

    const check = () => {
      const current = this.notificationService.notifications();
      if (current.length > this.lastNotificationCount) {
        // New notifications arrived
        const newOnes = current.slice(0, current.length - this.lastNotificationCount);
        for (const n of newOnes) {
          this.showToast(n);
        }
      }
      this.lastNotificationCount = current.length;
      this._rafId = requestAnimationFrame(check);
    };
    this._rafId = requestAnimationFrame(check);
  }

  private _rafId = 0;

  showToast(notification: AppNotification): void {
    this.visibleToasts.update((list) => {
      // Only show max 3 toasts at a time
      const updated = [notification, ...list].slice(0, 3);
      return updated;
    });

    // Auto-dismiss after 8 seconds
    const timeout = setTimeout(() => {
      this.dismissToast(notification.id);
    }, 8000);
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
  }

  showLatestNotification(): void {
    const unread = this.notificationService.notifications().filter((n) => !n.read);
    if (unread.length > 0) {
      this.showToast(unread[0]);
    }
    this.notificationService.markAllRead();
  }

  getNotificationTitle(type: string): string {
    switch (type) {
      case 'NEW_PENDING_ORDER':
        return 'Nouvelle commande';
      case 'PAYMENT_SUCCESS':
        return 'Paiement confirmé';
      case 'STATUS_CHANGED':
        return 'Statut mis à jour';
      case 'REFUND':
        return 'Remboursement';
      default:
        return 'Notification';
    }
  }

  ngOnDestroy(): void {
    if (this._rafId) {
      cancelAnimationFrame(this._rafId);
    }
    for (const timeout of this.toastTimeouts.values()) {
      clearTimeout(timeout);
    }
    this.notificationService.disconnect();
  }
}
