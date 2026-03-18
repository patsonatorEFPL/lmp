import {
  Component,
  inject,
  input,
  output,
  computed,
  ElementRef,
  HostListener,
} from '@angular/core';
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
  Inbox,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import {
  NotificationService,
  AppNotification,
} from '../../core/services/notification.service';

interface NotificationGroup {
  label: string;
  notifications: AppNotification[];
}

@Component({
  selector: 'lmp-notification-panel',
  standalone: true,
  imports: [RouterLink, LucideAngularModule, HlmButton],
  template: `
    @if (isOpen()) {
      <div
        class="absolute right-0 top-2 z-[200] w-[22rem] sm:w-96 rounded-2xl border border-(--border) bg-(--card) shadow-2xl shadow-black/20 notification-panel-enter overflow-hidden"
        role="dialog"
        aria-label="Panneau de notifications"
      >
        <!-- Header -->
        <div
          class="flex items-center justify-between border-b border-(--border) bg-(--card) px-5 py-3.5"
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
              aria-label="Fermer"
            >
              <lucide-icon [img]="XIcon" [size]="14"></lucide-icon>
            </button>
          </div>
        </div>

        <!-- Notification list -->
        <div class="max-h-80 overflow-y-auto overscroll-contain notification-panel-scroll">
          @if (notificationService.notifications().length === 0) {
            <!-- Empty state -->
            <div class="flex flex-col items-center justify-center py-14 px-6">
              <div
                class="flex h-16 w-16 items-center justify-center rounded-full bg-gradient-to-br from-(--muted) to-(--muted)/60"
              >
                <lucide-icon
                  [img]="InboxIcon"
                  [size]="28"
                  class="text-(--muted-foreground)"
                ></lucide-icon>
              </div>
              <p class="mt-4 text-sm font-semibold text-(--foreground)">
                Aucune notification
              </p>
              <p class="mt-1.5 text-xs text-(--muted-foreground) text-center max-w-[220px]">
                Vous recevrez des notifications pour vos commandes, paiements et mises à jour.
              </p>
            </div>
          } @else {
            @for (group of groupedNotifications(); track group.label) {
              <!-- Date group header -->
              <div class="sticky top-0 z-10 bg-(--card)/95 backdrop-blur-sm px-5 py-2 border-b border-(--border)/50">
                <span class="text-[10px] font-semibold uppercase tracking-wider text-(--muted-foreground)">
                  {{ group.label }}
                </span>
              </div>

              @for (notification of group.notifications; track notification.id) {
                <div
                  class="group flex items-start gap-3 border-b border-(--border)/30 px-5 py-3.5 transition-all cursor-pointer"
                  [class]="getNotificationRowClasses(notification)"
                  (click)="onNotificationClick(notification)"
                >
                  <!-- Unread accent bar -->
                  @if (!notification.read) {
                    <div
                      class="absolute left-0 top-0 bottom-0 w-[3px] rounded-r-full"
                      [class]="getTypeAccentBg(notification.type)"
                    ></div>
                  }

                  <!-- Type icon -->
                  <div
                    class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg transition-transform group-hover:scale-105"
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
                        class="text-sm truncate"
                        [class]="notification.read ? 'font-medium text-(--foreground)' : 'font-bold text-(--foreground)'"
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
                      class="mt-0.5 text-xs line-clamp-2"
                      [class]="notification.read ? 'text-(--muted-foreground)/70' : 'text-(--muted-foreground)'"
                    >
                      {{ notification.message }}
                    </p>
                    <p class="mt-1 text-[10px] text-(--muted-foreground)/60">
                      {{ formatTimeAgo(notification.timestamp) }}
                    </p>
                  </div>

                  <!-- Actions on hover -->
                  <button
                    class="shrink-0 opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer rounded-md p-1 text-(--muted-foreground) hover:text-(--destructive) hover:bg-(--destructive)/10"
                    (click)="dismissNotification($event, notification.id)"
                    title="Supprimer"
                    aria-label="Supprimer la notification"
                  >
                    <lucide-icon [img]="XIcon" [size]="12"></lucide-icon>
                  </button>
                </div>
              }
            }
          }
        </div>

        <!-- Footer -->
        @if (notificationService.notifications().length > 0) {
          <div
            class="border-t border-(--border) px-5 py-2.5 flex items-center justify-between"
          >
            <span class="text-xs text-(--muted-foreground)">
              {{ notificationService.notifications().length }} notification(s)
            </span>
            @if (notificationService.unreadCount() > 0) {
              <button
                class="text-xs font-semibold text-(--primary) hover:underline cursor-pointer"
                (click)="markAllRead()"
              >
                Tout marquer comme lu
              </button>
            }
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

    .notification-panel-scroll {
      scrollbar-width: thin;
      scrollbar-color: var(--muted) transparent;
    }

    .notification-panel-scroll::-webkit-scrollbar {
      width: 4px;
    }

    .notification-panel-scroll::-webkit-scrollbar-track {
      background: transparent;
    }

    .notification-panel-scroll::-webkit-scrollbar-thumb {
      background: var(--muted);
      border-radius: 4px;
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
    // Check if click was inside the panel OR its parent container
    // (the parent holds both the bell button and the panel)
    const hostEl = this.elementRef.nativeElement as HTMLElement;
    const parentContainer = hostEl.parentElement;
    const target = event.target as Node;
    const clickedInsidePanel = hostEl.contains(target);
    const clickedInsideParent = parentContainer?.contains(target) ?? false;
    if (!clickedInsidePanel && !clickedInsideParent) {
      this.close();
    }
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.isOpen()) {
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
  readonly InboxIcon = Inbox;

  /** Group notifications by date: Aujourd'hui, Hier, Plus ancien */
  readonly groupedNotifications = computed<NotificationGroup[]>(() => {
    const notifications = this.notificationService.notifications();
    if (notifications.length === 0) return [];

    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const yesterday = new Date(today.getTime() - 86400000);

    const groups: Record<string, AppNotification[]> = {
      "Aujourd'hui": [],
      'Hier': [],
      'Plus ancien': [],
    };

    for (const n of notifications) {
      const date = new Date(n.timestamp);
      if (date >= today) {
        groups["Aujourd'hui"].push(n);
      } else if (date >= yesterday) {
        groups['Hier'].push(n);
      } else {
        groups['Plus ancien'].push(n);
      }
    }

    return Object.entries(groups)
      .filter(([, items]) => items.length > 0)
      .map(([label, items]) => ({ label, notifications: items }));
  });

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
    this.notificationService.dismissNotification(id);
  }

  getNotificationRowClasses(notification: AppNotification): string {
    const base = 'relative';
    const readState = notification.read
      ? 'hover:bg-(--muted)/40'
      : 'bg-(--primary)/5 hover:bg-(--primary)/8';
    return `${base} ${readState}`;
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

  getTypeAccentBg(type: string): string {
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
