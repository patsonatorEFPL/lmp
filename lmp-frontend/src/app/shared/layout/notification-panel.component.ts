import {
  Component,
  inject,
  signal,
  input,
  output,
  ElementRef,
  HostListener,
  Inject,
  PLATFORM_ID,
} from '@angular/core';
import { isPlatformBrowser, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  Bell,
  BellOff,
  X,
  Check,
  CheckCheck,
  ShoppingCart,
  CreditCard,
  RefreshCw,
  AlertCircle,
  Trash2,
  Info,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import {
  NotificationService,
  AppNotification,
} from '../../core/services/notification.service';

@Component({
  selector: 'lmp-notification-panel',
  standalone: true,
  imports: [RouterLink, LucideAngularModule, HlmButton, DatePipe],
  template: `
    <!-- Panel (positioned absolutely from parent) -->
    @if (isOpen()) {
      <div
        class="absolute right-0 top-2 z-[200] w-[22rem] sm:w-96 rounded-2xl border border-(--border) bg-(--card) shadow-2xl shadow-black/20 notification-panel-enter overflow-hidden"
      >
        <!-- Header -->
        <div
          class="flex items-center justify-between border-b border-(--border) px-5 py-3.5"
        >
          <div class="flex items-center gap-2">
            <lucide-icon
              [img]="BellIcon"
              [size]="18"
              class="text-(--primary)"
            ></lucide-icon>
            <h3 class="font-display text-sm font-bold text-(--foreground)">
              Notifications
            </h3>
            @if (notificationService.unreadCount() > 0) {
              <span
                class="flex h-5 min-w-5 items-center justify-center rounded-full bg-(--primary) px-1.5 text-[10px] font-bold text-white"
              >
                {{ notificationService.unreadCount() }}
              </span>
            }
          </div>
          <div class="flex items-center gap-1">
            @if (notificationService.notifications().length > 0) {
              <button
                hlmBtn
                variant="ghost"
                size="icon"
                class="h-7 w-7 cursor-pointer text-(--muted-foreground) hover:text-(--primary)"
                title="Tout marquer comme lu"
                (click)="markAllRead()"
              >
                <lucide-icon [img]="CheckCheckIcon" [size]="14"></lucide-icon>
              </button>
              <button
                hlmBtn
                variant="ghost"
                size="icon"
                class="h-7 w-7 cursor-pointer text-(--muted-foreground) hover:text-(--destructive)"
                title="Effacer tout"
                (click)="clearAll()"
              >
                <lucide-icon [img]="Trash2Icon" [size]="14"></lucide-icon>
              </button>
            }
            <button
              hlmBtn
              variant="ghost"
              size="icon"
              class="h-7 w-7 cursor-pointer text-(--muted-foreground) hover:text-(--foreground)"
              (click)="close()"
            >
              <lucide-icon [img]="XIcon" [size]="14"></lucide-icon>
            </button>
          </div>
        </div>

        <!-- Notification list -->
        <div class="max-h-80 overflow-y-auto overscroll-contain">
          @if (notificationService.notifications().length === 0) {
            <!-- Empty state -->
            <div class="flex flex-col items-center justify-center py-12 px-6">
              <div
                class="flex h-14 w-14 items-center justify-center rounded-full bg-(--muted)"
              >
                <lucide-icon
                  [img]="BellOffIcon"
                  [size]="24"
                  class="text-(--muted-foreground)"
                ></lucide-icon>
              </div>
              <p class="mt-3 text-sm font-medium text-(--foreground)">
                Aucune notification
              </p>
              <p class="mt-1 text-xs text-(--muted-foreground) text-center">
                Vos notifications apparaîtront ici.
              </p>
            </div>
          } @else {
            @for (
              notification of notificationService.notifications();
              track notification.id
            ) {
              <div
                class="group flex items-start gap-3 border-b border-(--border)/50 px-5 py-3.5 transition-colors hover:bg-(--muted)/50 cursor-pointer"
                [class.bg-primary/3]="!notification.read"
                (click)="onNotificationClick(notification)"
              >
                <!-- Type icon -->
                <div
                  class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg"
                  [class]="getTypeIconBg(notification.type)"
                >
                  <lucide-icon
                    [img]="getTypeIcon(notification.type)"
                    [size]="16"
                  ></lucide-icon>
                </div>

                <!-- Content -->
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2">
                    <p
                      class="text-sm font-semibold text-(--foreground) truncate"
                      [class.font-bold]="!notification.read"
                    >
                      {{ getNotificationTitle(notification.type) }}
                    </p>
                    @if (!notification.read) {
                      <span
                        class="h-2 w-2 shrink-0 rounded-full bg-(--primary) animate-pulse"
                      ></span>
                    }
                  </div>
                  <p
                    class="mt-0.5 text-xs text-(--muted-foreground) line-clamp-2"
                  >
                    {{ notification.message }}
                  </p>
                  <p class="mt-1 text-[10px] text-(--muted-foreground)/70">
                    {{ formatTimeAgo(notification.timestamp) }}
                  </p>
                </div>

                <!-- Actions on hover -->
                <button
                  class="shrink-0 opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer text-(--muted-foreground) hover:text-(--foreground)"
                  (click)="dismissNotification($event, notification.id)"
                  title="Supprimer"
                >
                  <lucide-icon [img]="XIcon" [size]="12"></lucide-icon>
                </button>
              </div>
            }
          }
        </div>

        <!-- Footer -->
        @if (notificationService.notifications().length > 0) {
          <div
            class="border-t border-(--border) px-5 py-2.5 text-center"
          >
            <span
              class="text-xs text-(--muted-foreground)"
            >
              {{ notificationService.notifications().length }} notification(s)
            </span>
          </div>
        }
      </div>
    }
  `,
  styles: `
    :host {
      display: block;
      position: relative;
    }
  `,
})
export class NotificationPanelComponent {
  readonly notificationService = inject(NotificationService);
  private readonly elementRef = inject(ElementRef);

  readonly isOpen = input<boolean>(false);
  readonly panelClosed = output<void>();

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.isOpen()) return;
    const clickedInside = this.elementRef.nativeElement.contains(
      event.target as Node,
    );
    if (!clickedInside) {
      this.close();
    }
  }

  readonly BellIcon = Bell;
  readonly BellOffIcon = BellOff;
  readonly XIcon = X;
  readonly CheckIcon = Check;
  readonly CheckCheckIcon = CheckCheck;
  readonly ShoppingCartIcon = ShoppingCart;
  readonly CreditCardIcon = CreditCard;
  readonly RefreshCwIcon = RefreshCw;
  readonly AlertCircleIcon = AlertCircle;
  readonly Trash2Icon = Trash2;
  readonly InfoIcon = Info;

  close(): void {
    this.panelClosed.emit();
  }

  markAllRead(): void {
    this.notificationService.markAllRead();
  }

  clearAll(): void {
    this.notificationService.clearAll();
  }

  onNotificationClick(notification: AppNotification): void {
    this.notificationService.markAsRead(notification.id);
  }

  dismissNotification(event: Event, id: string): void {
    event.stopPropagation();
    this.notificationService.notifications.update((list) =>
      list.filter((n) => n.id !== id),
    );
    if (!this.notificationService.notifications().find((n) => n.id === id && !n.read)) {
      // was unread
    }
  }

  getTypeIcon(type: string) {
    switch (type) {
      case 'NEW_PENDING_ORDER':
        return this.ShoppingCartIcon;
      case 'PAYMENT_SUCCESS':
        return this.CreditCardIcon;
      case 'STATUS_CHANGED':
        return this.RefreshCwIcon;
      case 'REFUND':
        return this.AlertCircleIcon;
      default:
        return this.BellIcon;
    }
  }

  getTypeIconBg(type: string): string {
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

  formatTimeAgo(timestamp: string): string {
    const now = new Date();
    const date = new Date(timestamp);
    const diffMs = now.getTime() - date.getTime();
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);

    if (diffSec < 60) return 'il y a quelques secondes';
    if (diffMin < 60) return `il y a ${diffMin} min`;
    if (diffHour < 24) return `il y a ${diffHour} h`;
    if (diffDay === 1) return 'hier';
    return date.toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'short',
    });
  }
}
