import { isPlatformBrowser, NgClass } from '@angular/common';
import { Component, inject, OnDestroy, OnInit, PLATFORM_ID, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs/operators';
import {
  LucideAngularModule,
  LayoutDashboard,
  Package,
  Users,
  ShoppingCart,
  Calendar,
  Activity,
  Settings,
  Menu,
  X,
  Bell,
  ChevronDown,
  PanelLeftClose,
  PanelLeftOpen,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { AdminSseService } from '../../core/services/admin-sse.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { NotificationPanelComponent } from './notification-panel.component';
import { ShellAccountMenuComponent } from './shell-account-menu.component';

@Component({
  selector: 'lmp-admin-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    NgClass,
    LucideAngularModule,
    HlmButton,
    NotificationPanelComponent,
    ShellAccountMenuComponent,
  ],
  template: `
    @if (adminSse.lastToast(); as toast) {
      <div
        class="fixed bottom-4 right-4 z-[260] max-w-sm rounded-xl border border-(--border) bg-(--card) p-4 shadow-lg ring-1 ring-(--foreground)/5"
        role="alert"
      >
        <p class="text-xs font-semibold text-(--primary)">{{ toast.title }}</p>
        <p class="mt-1 text-sm text-(--foreground)">{{ toast.message }}</p>
      </div>
    }
    <!--
      Desktop : une seule border-b sur la rangée marque + outils (pas deux traits distincts).
      La ligne verticale = border-r sur la colonne 220px (marque puis nav) — un seul axe.
    -->
    <div
      class="flex min-h-screen flex-col bg-surface-white dark:bg-zinc-950 lg:h-screen lg:overflow-hidden"
    >
      <!-- Bandeau unique desktop : trait horizontal unique + séparation marque | titre -->
      <div
        class="hidden h-14 shrink-0 border-b border-zinc-200/90 dark:border-zinc-800 lg:flex lg:items-stretch"
      >
        <div
          class="flex shrink-0 items-center border-r border-zinc-200/90 bg-surface-menu-bar transition-[width] duration-200 ease-out dark:border-zinc-800 dark:bg-zinc-900"
          [ngClass]="
            sidebarCollapsed()
              ? 'w-16 justify-center px-1'
              : 'w-[220px] justify-between gap-2 px-2'
          "
        >
          <a
            routerLink="/"
            class="flex items-center gap-2 rounded-sm px-1 py-0.5 focus-visible:ring-2 focus-visible:ring-zinc-400"
            [class.min-w-0]="!sidebarCollapsed()"
            [class.flex-1]="!sidebarCollapsed()"
            [class.justify-center]="sidebarCollapsed()"
            [title]="sidebarCollapsed() ? 'LMP Digital Services — Administrator' : ''"
          >
            <img src="/images/logo-lmp.webp" alt="LMP" class="h-7 w-auto shrink-0 rounded-sm" />
            @if (!sidebarCollapsed()) {
              <div class="min-w-0 text-left leading-tight">
                <span class="block truncate text-[13px] font-medium text-zinc-900 dark:text-zinc-100"
                  >LMP Digital Services</span
                >
                <span class="mt-0.5 block truncate text-[11px] text-zinc-500 dark:text-zinc-400"
                  >Administrator</span
                >
              </div>
            }
          </a>
          @if (!sidebarCollapsed()) {
            <span
              class="flex h-7 shrink-0 items-center rounded p-0.5 text-zinc-400 dark:text-zinc-500"
              aria-hidden="true"
            >
              <lucide-icon [img]="ChevronDownIcon" [size]="16"></lucide-icon>
            </span>
          }
        </div>
        <div
          class="flex min-w-0 flex-1 items-center justify-between gap-3 bg-white px-3 sm:pl-5 sm:pr-6 dark:bg-zinc-950"
        >
          <div class="flex min-w-0 items-center gap-2 sm:gap-3">
            <h1
              class="min-w-0 truncate text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400"
            >
              {{ adminPageTitle() }}
            </h1>
            <span
              class="hidden shrink-0 items-center gap-1.5 text-[11px] text-zinc-500 sm:inline-flex"
              title="Flux temps réel (SSE)"
            >
              <span
                class="h-2 w-2 rounded-full"
                [class.bg-emerald-500]="adminSse.connected()"
                [class.bg-red-500]="!adminSse.connected()"
              ></span>
              Live
            </span>
          </div>
          <div class="flex shrink-0 items-center gap-1.5 sm:gap-2">
            <div class="relative hidden lg:block" (click)="$event.stopPropagation()">
              <button
                hlmBtn
                variant="ghost"
                size="icon"
                type="button"
                class="relative cursor-pointer rounded-full"
                (click)="onNotificationButtonClick()"
                aria-label="Notifications"
              >
                <lucide-icon [img]="BellIcon" [size]="18"></lucide-icon>
                @if (notificationService.unreadCount() > 0) {
                  <span
                    class="absolute -top-0.5 -right-0.5 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white ring-2 ring-white dark:ring-zinc-950"
                  >
                    {{ notificationService.unreadCount() > 9 ? '9+' : notificationService.unreadCount() }}
                  </span>
                }
              </button>
              <lmp-notification-panel
                [isOpen]="showNotificationPanel()"
                (panelClosed)="showNotificationPanel.set(false)"
              />
            </div>
            <lmp-shell-account-menu
              variant="admin"
              (menuOpenChange)="onAccountMenuOpenChange($event)"
              (logoutRequest)="onLogout()"
            />
          </div>
        </div>
      </div>

      <div class="flex min-h-0 flex-1 flex-col overflow-hidden lg:flex-row">
      <!-- Sidebar desktop : navigation uniquement (marque dans le bandeau ci-dessus) -->
      <aside
        class="z-30 hidden shrink-0 flex-col overflow-hidden border-r border-zinc-200/90 bg-surface-menu-bar transition-[width] duration-200 ease-out dark:border-zinc-800 dark:bg-zinc-900 lg:z-auto lg:flex"
        [ngClass]="sidebarCollapsed() ? 'w-16' : 'w-[220px]'"
      >
        <nav
          class="flex min-h-0 flex-1 flex-col gap-0 overflow-y-auto overflow-x-hidden pb-2 pt-1"
          [ngClass]="sidebarCollapsed() ? '[scrollbar-gutter:auto]' : '[scrollbar-gutter:stable]'"
          [class.px-2]="!sidebarCollapsed()"
          [class.px-0]="sidebarCollapsed()"
        >
          @if (!sidebarCollapsed()) {
            <p
              class="px-4 pb-2 pt-3 text-xs font-medium text-zinc-500 dark:text-zinc-500"
            >
              Vues
            </p>
          }

          <a
            routerLink="/admin"
            [routerLinkActive]="sidebarLinkActive"
            [routerLinkActiveOptions]="{ exact: true }"
            class="my-[1.5px] flex min-h-[30px] cursor-pointer items-center rounded py-[7px] text-sm text-zinc-700 transition-colors duration-200 ease-in-out hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
            [class.justify-center]="sidebarCollapsed()"
            [class.gap-2]="!sidebarCollapsed()"
            [class.px-2]="!sidebarCollapsed()"
            [class.mx-0.5]="!sidebarCollapsed()"
            [class.w-full]="sidebarCollapsed()"
            [attr.aria-label]="sidebarCollapsed() ? 'Tableau de bord' : undefined"
            title="Tableau de bord"
          >
            <lucide-icon [img]="DashboardIcon" [size]="16" class="inline-flex shrink-0"></lucide-icon>
            @if (!sidebarCollapsed()) {
              <span class="truncate">Tableau de bord</span>
            }
          </a>
          <a
            routerLink="/admin/services"
            [routerLinkActive]="sidebarLinkActive"
            class="my-[1.5px] flex min-h-[30px] cursor-pointer items-center rounded py-[7px] text-sm text-zinc-700 transition-colors duration-200 ease-in-out hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
            [class.justify-center]="sidebarCollapsed()"
            [class.gap-2]="!sidebarCollapsed()"
            [class.px-2]="!sidebarCollapsed()"
            [class.mx-0.5]="!sidebarCollapsed()"
            [class.w-full]="sidebarCollapsed()"
            [attr.aria-label]="sidebarCollapsed() ? 'Services' : undefined"
            title="Services"
          >
            <lucide-icon [img]="PackageIcon" [size]="16" class="inline-flex shrink-0"></lucide-icon>
            @if (!sidebarCollapsed()) {
              <span class="truncate">Services</span>
            }
          </a>
          <a
            routerLink="/admin/users"
            [routerLinkActive]="sidebarLinkActive"
            (click)="adminSse.badgeUsers.set(0)"
            class="relative my-[1.5px] flex min-h-[30px] cursor-pointer items-center rounded py-[7px] text-sm text-zinc-700 transition-colors duration-200 ease-in-out hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
            [class.justify-center]="sidebarCollapsed()"
            [class.gap-2]="!sidebarCollapsed()"
            [class.px-2]="!sidebarCollapsed()"
            [class.mx-0.5]="!sidebarCollapsed()"
            [class.w-full]="sidebarCollapsed()"
            [attr.aria-label]="sidebarCollapsed() ? 'Utilisateurs' : undefined"
            title="Utilisateurs"
          >
            <lucide-icon [img]="UsersIcon" [size]="16" class="inline-flex shrink-0"></lucide-icon>
            @if (!sidebarCollapsed()) {
              <span class="truncate">Utilisateurs</span>
            }
            @if (adminSse.badgeUsers() > 0) {
              <span
                class="absolute flex h-4 min-w-4 items-center justify-center rounded bg-red-500 px-1 text-[10px] font-bold text-white"
                [ngClass]="sidebarCollapsed() ? 'right-1 top-1' : 'right-2 top-1.5'"
                >{{ adminSse.badgeUsers() > 9 ? '9+' : adminSse.badgeUsers() }}</span
              >
            }
          </a>
          <a
            routerLink="/admin/orders"
            [routerLinkActive]="sidebarLinkActive"
            (click)="adminSse.badgeOrders.set(0)"
            class="relative my-[1.5px] flex min-h-[30px] cursor-pointer items-center rounded py-[7px] text-sm text-zinc-700 transition-colors duration-200 ease-in-out hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
            [class.justify-center]="sidebarCollapsed()"
            [class.gap-2]="!sidebarCollapsed()"
            [class.px-2]="!sidebarCollapsed()"
            [class.mx-0.5]="!sidebarCollapsed()"
            [class.w-full]="sidebarCollapsed()"
            [attr.aria-label]="sidebarCollapsed() ? 'Commandes' : undefined"
            title="Commandes"
          >
            <lucide-icon [img]="OrdersIcon" [size]="16" class="inline-flex shrink-0"></lucide-icon>
            @if (!sidebarCollapsed()) {
              <span class="truncate">Commandes</span>
            }
            @if (adminSse.badgeOrders() > 0) {
              <span
                class="absolute flex h-4 min-w-4 items-center justify-center rounded bg-red-500 px-1 text-[10px] font-bold text-white"
                [ngClass]="sidebarCollapsed() ? 'right-1 top-1' : 'right-2 top-1.5'"
                >{{ adminSse.badgeOrders() > 9 ? '9+' : adminSse.badgeOrders() }}</span
              >
            }
          </a>
          <a
            routerLink="/admin/appointments"
            [routerLinkActive]="sidebarLinkActive"
            (click)="adminSse.badgeAppointments.set(0)"
            class="relative my-[1.5px] flex min-h-[30px] cursor-pointer items-center rounded py-[7px] text-sm text-zinc-700 transition-colors duration-200 ease-in-out hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
            [class.justify-center]="sidebarCollapsed()"
            [class.gap-2]="!sidebarCollapsed()"
            [class.px-2]="!sidebarCollapsed()"
            [class.mx-0.5]="!sidebarCollapsed()"
            [class.w-full]="sidebarCollapsed()"
            [attr.aria-label]="sidebarCollapsed() ? 'Rendez-vous' : undefined"
            title="Rendez-vous"
          >
            <lucide-icon [img]="CalendarIcon" [size]="16" class="inline-flex shrink-0"></lucide-icon>
            @if (!sidebarCollapsed()) {
              <span class="truncate">Rendez-vous</span>
            }
            @if (adminSse.badgeAppointments() > 0) {
              <span
                class="absolute flex h-4 min-w-4 items-center justify-center rounded bg-red-500 px-1 text-[10px] font-bold text-white"
                [ngClass]="sidebarCollapsed() ? 'right-1 top-1' : 'right-2 top-1.5'"
                >{{ adminSse.badgeAppointments() > 9 ? '9+' : adminSse.badgeAppointments() }}</span
              >
            }
          </a>
          <a
            routerLink="/admin/monitoring"
            [routerLinkActive]="sidebarLinkActive"
            class="my-[1.5px] flex min-h-[30px] cursor-pointer items-center rounded py-[7px] text-sm text-zinc-700 transition-colors duration-200 ease-in-out hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
            [class.justify-center]="sidebarCollapsed()"
            [class.gap-2]="!sidebarCollapsed()"
            [class.px-2]="!sidebarCollapsed()"
            [class.mx-0.5]="!sidebarCollapsed()"
            [class.w-full]="sidebarCollapsed()"
            [attr.aria-label]="sidebarCollapsed() ? 'Monitoring' : undefined"
            title="Monitoring"
          >
            <lucide-icon [img]="MonitoringIcon" [size]="16" class="inline-flex shrink-0"></lucide-icon>
            @if (!sidebarCollapsed()) {
              <span class="truncate">Monitoring</span>
            }
          </a>
          <a
            routerLink="/admin/settings"
            [routerLinkActive]="sidebarLinkActive"
            class="my-[1.5px] flex min-h-[30px] cursor-pointer items-center rounded py-[7px] text-sm text-zinc-700 transition-colors duration-200 ease-in-out hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
            [class.justify-center]="sidebarCollapsed()"
            [class.gap-2]="!sidebarCollapsed()"
            [class.px-2]="!sidebarCollapsed()"
            [class.mx-0.5]="!sidebarCollapsed()"
            [class.w-full]="sidebarCollapsed()"
            [attr.aria-label]="sidebarCollapsed() ? 'Paramètres' : undefined"
            title="Paramètres"
          >
            <lucide-icon [img]="SettingsIcon" [size]="16" class="inline-flex shrink-0"></lucide-icon>
            @if (!sidebarCollapsed()) {
              <span class="truncate">Paramètres</span>
            }
          </a>
        </nav>

        <div class="shrink-0 border-t border-zinc-200/80 dark:border-zinc-800">
          <button
            type="button"
            hlmBtn
            variant="ghost"
            size="sm"
            class="my-1 flex w-full cursor-pointer text-zinc-600 hover:bg-zinc-200/70 hover:text-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-100"
            [class.gap-2]="!sidebarCollapsed()"
            [class.gap-0]="sidebarCollapsed()"
            [class.justify-center]="sidebarCollapsed()"
            [class.justify-start]="!sidebarCollapsed()"
            [class.px-2]="!sidebarCollapsed()"
            [class.px-0]="sidebarCollapsed()"
            (click)="toggleSidebarCollapsed()"
            [attr.aria-expanded]="!sidebarCollapsed()"
            [attr.aria-label]="sidebarCollapsed() ? 'Développer le menu latéral' : 'Réduire le menu latéral'"
          >
            @if (sidebarCollapsed()) {
              <lucide-icon [img]="PanelLeftOpenIcon" [size]="16" class="inline-flex shrink-0"></lucide-icon>
              <span class="sr-only">Développer le menu</span>
            } @else {
              <lucide-icon [img]="PanelLeftCloseIcon" [size]="16" class="inline-flex shrink-0"></lucide-icon>
              <span>Réduire</span>
            }
          </button>
          @if (!sidebarCollapsed()) {
            <p class="px-3 pb-2.5 pt-0 text-center text-[10px] text-zinc-400 dark:text-zinc-500">
              LMP Administration
            </p>
          }
        </div>
      </aside>

      @if (mobileMenuOpen()) {
        <button
          type="button"
          tabindex="-1"
          class="fixed inset-0 z-40 cursor-default touch-none bg-black/40 lg:hidden"
          aria-label="Fermer le menu"
          (click)="mobileMenuOpen.set(false)"
        ></button>
        <aside
          class="fixed inset-y-0 left-0 z-50 flex w-[min(18rem,calc(100vw-2.5rem))] flex-col border-r border-zinc-200/90 bg-surface-menu-bar shadow-xl dark:border-zinc-800 dark:bg-zinc-900 lg:hidden"
        >
          <div
            class="box-border flex h-14 min-h-14 shrink-0 items-center justify-between gap-2 border-b border-zinc-200/90 px-2 dark:border-zinc-800"
          >
            <a
              routerLink="/"
              class="flex min-w-0 flex-1 items-center gap-2 rounded-sm px-1 py-0.5 focus-visible:ring-2 focus-visible:ring-zinc-400"
              (click)="mobileMenuOpen.set(false)"
            >
              <img src="/images/logo-lmp.webp" alt="LMP" class="h-7 w-auto shrink-0 rounded-sm" />
              <div class="min-w-0 text-left leading-tight">
                <span class="block truncate text-[13px] font-medium text-zinc-900 dark:text-zinc-100"
                  >LMP Digital Services</span
                >
                <span class="mt-0.5 block truncate text-[11px] text-zinc-500 dark:text-zinc-400"
                  >Administrator</span
                >
              </div>
            </a>
            <button
              hlmBtn
              variant="ghost"
              size="icon"
              type="button"
              class="shrink-0 cursor-pointer"
              (click)="mobileMenuOpen.set(false)"
              aria-label="Fermer"
            >
              <lucide-icon [img]="XIcon" [size]="18"></lucide-icon>
            </button>
          </div>
          <nav class="flex flex-1 flex-col gap-0 overflow-y-auto px-2 pb-2 pt-1">
            <p class="px-4 pb-2 pt-3 text-xs font-medium text-zinc-500">Vues</p>
            <a
              routerLink="/admin"
              [routerLinkActive]="sidebarLinkActive"
              [routerLinkActiveOptions]="{ exact: true }"
              class="mx-0.5 my-[1.5px] flex min-h-[30px] cursor-pointer items-center gap-2 rounded px-2 py-[7px] text-sm text-zinc-700 transition-colors hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
              (click)="mobileMenuOpen.set(false)"
            >
              <lucide-icon [img]="DashboardIcon" [size]="16"></lucide-icon>
              Tableau de bord
            </a>
            <a
              routerLink="/admin/services"
              [routerLinkActive]="sidebarLinkActive"
              class="mx-0.5 my-[1.5px] flex min-h-[30px] cursor-pointer items-center gap-2 rounded px-2 py-[7px] text-sm text-zinc-700 transition-colors hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
              (click)="mobileMenuOpen.set(false)"
            >
              <lucide-icon [img]="PackageIcon" [size]="16"></lucide-icon>
              Services
            </a>
            <a
              routerLink="/admin/users"
              [routerLinkActive]="sidebarLinkActive"
              class="relative mx-0.5 my-[1.5px] flex min-h-[30px] cursor-pointer items-center gap-2 rounded px-2 py-[7px] text-sm text-zinc-700 transition-colors hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
              (click)="mobileMenuOpen.set(false); adminSse.badgeUsers.set(0)"
            >
              <lucide-icon [img]="UsersIcon" [size]="16"></lucide-icon>
              Utilisateurs
              @if (adminSse.badgeUsers() > 0) {
                <span
                  class="absolute right-2 top-1.5 flex h-4 min-w-4 items-center justify-center rounded bg-red-500 px-1 text-[10px] font-bold text-white"
                  >{{ adminSse.badgeUsers() > 9 ? '9+' : adminSse.badgeUsers() }}</span
                >
              }
            </a>
            <a
              routerLink="/admin/orders"
              [routerLinkActive]="sidebarLinkActive"
              class="relative mx-0.5 my-[1.5px] flex min-h-[30px] cursor-pointer items-center gap-2 rounded px-2 py-[7px] text-sm text-zinc-700 transition-colors hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
              (click)="mobileMenuOpen.set(false); adminSse.badgeOrders.set(0)"
            >
              <lucide-icon [img]="OrdersIcon" [size]="16"></lucide-icon>
              Commandes
              @if (adminSse.badgeOrders() > 0) {
                <span
                  class="absolute right-2 top-1.5 flex h-4 min-w-4 items-center justify-center rounded bg-red-500 px-1 text-[10px] font-bold text-white"
                  >{{ adminSse.badgeOrders() > 9 ? '9+' : adminSse.badgeOrders() }}</span
                >
              }
            </a>
            <a
              routerLink="/admin/appointments"
              [routerLinkActive]="sidebarLinkActive"
              class="relative mx-0.5 my-[1.5px] flex min-h-[30px] cursor-pointer items-center gap-2 rounded px-2 py-[7px] text-sm text-zinc-700 transition-colors hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
              (click)="mobileMenuOpen.set(false); adminSse.badgeAppointments.set(0)"
            >
              <lucide-icon [img]="CalendarIcon" [size]="16"></lucide-icon>
              Rendez-vous
              @if (adminSse.badgeAppointments() > 0) {
                <span
                  class="absolute right-2 top-1.5 flex h-4 min-w-4 items-center justify-center rounded bg-red-500 px-1 text-[10px] font-bold text-white"
                  >{{ adminSse.badgeAppointments() > 9 ? '9+' : adminSse.badgeAppointments() }}</span
                >
              }
            </a>
            <a
              routerLink="/admin/monitoring"
              [routerLinkActive]="sidebarLinkActive"
              class="mx-0.5 my-[1.5px] flex min-h-[30px] cursor-pointer items-center gap-2 rounded px-2 py-[7px] text-sm text-zinc-700 transition-colors hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
              (click)="mobileMenuOpen.set(false)"
            >
              <lucide-icon [img]="MonitoringIcon" [size]="16"></lucide-icon>
              Monitoring
            </a>
            <a
              routerLink="/admin/settings"
              [routerLinkActive]="sidebarLinkActive"
              class="mx-0.5 my-[1.5px] flex min-h-[30px] cursor-pointer items-center gap-2 rounded px-2 py-[7px] text-sm text-zinc-700 transition-colors hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
              (click)="mobileMenuOpen.set(false)"
            >
              <lucide-icon [img]="SettingsIcon" [size]="16"></lucide-icon>
              Paramètres
            </a>
          </nav>
        </aside>
      }

      <div
        class="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden bg-white dark:bg-zinc-950 lg:min-h-0"
      >
        <header
          class="sticky top-0 z-20 shrink-0 border-b border-zinc-200/90 bg-white lg:hidden dark:border-zinc-800 dark:bg-zinc-950"
        >
          <div
            class="box-border flex h-14 min-h-14 shrink-0 items-center justify-between gap-3 px-3 sm:pl-5 sm:pr-6"
          >
            <div class="flex min-w-0 flex-1 items-center gap-3">
              <button
                hlmBtn
                variant="ghost"
                size="icon"
                type="button"
                class="shrink-0 cursor-pointer lg:hidden"
                (click)="mobileMenuOpen.set(!mobileMenuOpen())"
                [attr.aria-expanded]="mobileMenuOpen()"
                aria-label="Menu de navigation"
              >
                <lucide-icon [img]="MenuIcon" [size]="18"></lucide-icon>
              </button>
              <div class="flex min-w-0 items-center gap-2 sm:gap-3">
                <h1
                  class="min-w-0 truncate text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400"
                >
                  {{ adminPageTitle() }}
                </h1>
                <span
                  class="hidden shrink-0 items-center gap-1.5 text-[11px] text-zinc-500 sm:inline-flex"
                  title="Flux temps réel (SSE)"
                >
                  <span
                    class="h-2 w-2 rounded-full"
                    [class.bg-emerald-500]="adminSse.connected()"
                    [class.bg-red-500]="!adminSse.connected()"
                  ></span>
                  Live
                </span>
              </div>
            </div>

            <div class="flex shrink-0 items-center gap-1.5 sm:gap-2">
              <div class="relative lg:hidden" (click)="$event.stopPropagation()">
                <button
                  hlmBtn
                  variant="ghost"
                  size="icon"
                  type="button"
                  class="relative cursor-pointer rounded-full"
                  (click)="onNotificationButtonClick()"
                  aria-label="Notifications"
                >
                  <lucide-icon [img]="BellIcon" [size]="18"></lucide-icon>
                  @if (notificationService.unreadCount() > 0) {
                    <span
                      class="absolute -top-0.5 -right-0.5 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white ring-2 ring-white dark:ring-zinc-950"
                    >
                      {{ notificationService.unreadCount() > 9 ? '9+' : notificationService.unreadCount() }}
                    </span>
                  }
                </button>
                <lmp-notification-panel
                  [isOpen]="showNotificationPanel()"
                  (panelClosed)="showNotificationPanel.set(false)"
                />
              </div>

              <lmp-shell-account-menu
                variant="admin"
                (menuOpenChange)="onAccountMenuOpenChange($event)"
                (logoutRequest)="onLogout()"
              />
            </div>
          </div>
        </header>

        <main
          class="lmp-dashboard-theme min-h-0 flex-1 overflow-x-hidden overflow-y-auto bg-white dark:bg-zinc-950"
        >
          <router-outlet />
        </main>
      </div>
      </div>
    </div>
  `,
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  private static readonly SIDEBAR_STORAGE_KEY = 'lmp-admin-sidebar-collapsed';

  readonly authService = inject(AuthService);
  readonly adminSse = inject(AdminSseService);
  readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);

  /** Desktop : sidebar étroite (icônes seules). */
  readonly sidebarCollapsed = signal(false);

  /** Titre de la vue courante (bandeau desktop + mobile). */
  readonly adminPageTitle = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.titleForAdminUrl(this.router.url)),
    ),
    { initialValue: this.titleForAdminUrl(this.router.url) },
  );

  private titleForAdminUrl(rawUrl: string): string {
    const path = rawUrl.split('?')[0].replace(/\/+$/, '') || '/';
    const parts = path.split('/').filter(Boolean);
    if (parts.length === 1 && parts[0] === 'admin') {
      return 'Tableau de bord';
    }
    if (parts.length >= 2 && parts[0] === 'admin') {
      switch (parts[1]) {
        case 'services':
          return 'Services';
        case 'users':
          return 'Utilisateurs';
        case 'orders':
          return 'Commandes';
        case 'appointments':
          return 'Rendez-vous';
        case 'settings':
          return 'Paramètres';
        default:
          return 'Administration';
      }
    }
    return 'Administration';
  }

  private readonly accountMenu = viewChild(ShellAccountMenuComponent);

  readonly showNotificationPanel = signal(false);
  readonly mobileMenuOpen = signal(false);

  readonly DashboardIcon = LayoutDashboard;
  readonly PackageIcon = Package;
  readonly UsersIcon = Users;
  readonly OrdersIcon = ShoppingCart;
  readonly CalendarIcon = Calendar;
  readonly MonitoringIcon = Activity;
  readonly SettingsIcon = Settings;
  readonly MenuIcon = Menu;
  readonly XIcon = X;
  readonly BellIcon = Bell;
  readonly ChevronDownIcon = ChevronDown;
  readonly PanelLeftCloseIcon = PanelLeftClose;
  readonly PanelLeftOpenIcon = PanelLeftOpen;

  /** Lien actif : fond blanc, léger relief (style liste / navigation) */
  readonly sidebarLinkActive =
    '!bg-white font-medium text-zinc-900 shadow-sm ring-1 ring-zinc-200/70 dark:!bg-zinc-800 dark:!text-white dark:ring-zinc-600';

  ngOnInit(): void {
    this.adminSse.connect();
    if (isPlatformBrowser(this.platformId)) {
      try {
        if (localStorage.getItem(AdminLayoutComponent.SIDEBAR_STORAGE_KEY) === '1') {
          this.sidebarCollapsed.set(true);
        }
      } catch {
        /* private mode */
      }
    }
  }

  toggleSidebarCollapsed(): void {
    this.sidebarCollapsed.update((c) => {
      const next = !c;
      if (isPlatformBrowser(this.platformId)) {
        try {
          localStorage.setItem(
            AdminLayoutComponent.SIDEBAR_STORAGE_KEY,
            next ? '1' : '0',
          );
        } catch {
          /* ignore */
        }
      }
      return next;
    });
  }

  ngOnDestroy(): void {
    this.adminSse.disconnect();
  }

  onAccountMenuOpenChange(open: boolean): void {
    if (open) {
      this.showNotificationPanel.set(false);
    }
  }

  onNotificationButtonClick(): void {
    this.accountMenu()?.closeMenu();
    this.showNotificationPanel.update((v) => !v);
  }

  onLogout(): void {
    this.adminSse.disconnect();
    this.notificationService.reset();
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
