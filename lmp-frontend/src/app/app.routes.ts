import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/home.component';
import { PublicLayoutComponent } from './shared/layout/public-layout.component';
import { authGuard, adminGuard } from './core/guards';

export const routes: Routes = [
  // Public pages with navbar + footer
  {
    path: '',
    component: PublicLayoutComponent,
    children: [
      {
        path: '',
        // Eager : évite la course au refresh entre bundle initial et chunk lazy (flash intermittent).
        component: HomeComponent,
      },
      {
        path: 'services',
        loadComponent: () =>
          import('./features/services/services.component').then(
            (m) => m.ServicesComponent,
          ),
      },
      {
        path: 'map',
        loadComponent: () =>
          import('./features/map/map.component').then((m) => m.MapComponent),
      },
      {
        path: 'about',
        loadComponent: () =>
          import('./features/about/about.component').then(
            (m) => m.AboutComponent,
          ),
      },
      {
        path: 'contact',
        loadComponent: () =>
          import('./features/contact/contact.component').then(
            (m) => m.ContactComponent,
          ),
      },
      {
        path: 'privacy',
        loadComponent: () =>
          import('./features/privacy/privacy.component').then(
            (m) => m.PrivacyComponent,
          ),
      },
      {
        path: 'terms',
        loadComponent: () =>
          import('./features/terms/terms.component').then(
            (m) => m.TermsComponent,
          ),
      },
      {
        path: 'blog',
        loadComponent: () =>
          import('./features/blog/blog-list.component').then(
            (m) => m.BlogListComponent,
          ),
      },
      {
        path: 'blog/:slug',
        loadComponent: () =>
          import('./features/blog/blog-detail.component').then(
            (m) => m.BlogDetailComponent,
          ),
      },
      // 404 sous PublicLayout : navbar/footer conservés pour permettre la navigation.
      // Route nommée /not-found accessible via redirect explicite.
      // PAS de wildcard "**" ici : il interférerait avec les routes top-level
      // (login/register/forgot-password/dashboard/admin) car le PublicLayout
      // 'path: ""' parent matche tout et descendrait dans le wildcard avant
      // de laisser Angular essayer les routes sœurs.
      {
        path: 'not-found',
        loadComponent: () =>
          import('./features/errors/not-found.component').then(
            (m) => m.NotFoundComponent,
          ),
      },
    ],
  },

  // Auth pages (no navbar/footer)
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register.component').then(
        (m) => m.RegisterComponent,
      ),
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/auth/forgot-password.component').then(
        (m) => m.ForgotPasswordComponent,
      ),
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/auth/reset-password.component').then(
        (m) => m.ResetPasswordComponent,
      ),
  },
  {
    path: 'accept-invitation',
    loadComponent: () =>
      import('./features/auth/accept-invitation.component').then(
        (m) => m.AcceptInvitationComponent,
      ),
  },

  // Checkout for an existing order (must be before :offerId wildcard)
  {
    path: 'checkout/order/:orderId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/checkout/checkout.component').then(
        (m) => m.CheckoutComponent,
      ),
  },
  // Unified checkout page (no navbar/footer — standalone)
  {
    path: 'checkout/:offerId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/checkout/checkout.component').then(
        (m) => m.CheckoutComponent,
      ),
  },

  // Payment callback pages (no navbar/footer — standalone)
  {
    path: 'payment/success',
    loadComponent: () =>
      import('./features/payment/payment-success.component').then(
        (m) => m.PaymentSuccessComponent,
      ),
  },
  {
    path: 'payment/cancel',
    loadComponent: () =>
      import('./features/payment/payment-cancel.component').then(
        (m) => m.PaymentCancelComponent,
      ),
  },
  {
    path: 'payment/guest',
    loadComponent: () =>
      import('./features/payment/payment-guest.component').then(
        (m) => m.PaymentGuestComponent,
      ),
  },

  // Protected pages (user dashboard with sidebar layout)
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard-layout.component').then(
        (m) => m.DashboardLayoutComponent,
      ),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/dashboard/dashboard-overview.component').then(
            (m) => m.DashboardOverviewComponent,
          ),
      },
      {
        path: 'orders',
        loadComponent: () =>
          import('./features/dashboard/user-orders.component').then(
            (m) => m.UserOrdersComponent,
          ),
      },
      {
        path: 'appointments',
        loadComponent: () =>
          import('./features/dashboard/user-appointments.component').then(
            (m) => m.UserAppointmentsComponent,
          ),
      },
      {
        path: 'quotations',
        loadComponent: () =>
          import('./features/dashboard/user-quotations.component').then(
            (m) => m.UserQuotationsComponent,
          ),
      },
      {
        path: 'invoices',
        loadComponent: () =>
          import('./features/dashboard/user-invoices.component').then(
            (m) => m.UserInvoicesComponent,
          ),
      },
      {
        path: 'projects',
        loadComponent: () =>
          import('./features/dashboard/user-projects.component').then(
            (m) => m.UserProjectsComponent,
          ),
      },
      {
        path: 'projects/:id',
        loadComponent: () =>
          import('./features/dashboard/user-project-detail.component').then(
            (m) => m.UserProjectDetailComponent,
          ),
      },
      {
        path: 'tickets',
        loadComponent: () =>
          import('./features/dashboard/user-tickets.component').then(
            (m) => m.UserTicketsComponent,
          ),
      },
      {
        path: 'tickets/:id',
        loadComponent: () =>
          import('./features/dashboard/user-ticket-detail.component').then(
            (m) => m.UserTicketDetailComponent,
          ),
      },
      {
        path: 'addresses',
        loadComponent: () =>
          import('./features/dashboard/user-addresses.component').then(
            (m) => m.UserAddressesComponent,
          ),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings.component').then(
            (m) => m.SettingsComponent,
          ),
      },
      // Sous-route inconnue dans /dashboard/* → 404 dans le shell dashboard
      {
        path: '**',
        loadComponent: () =>
          import('./features/errors/not-found.component').then(
            (m) => m.NotFoundComponent,
          ),
      },
    ],
  },
  {
    path: 'settings',
    redirectTo: '/dashboard/settings',
    pathMatch: 'full',
  },

  // Admin pages (admin layout with sidebar)
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./shared/layout/admin-layout.component').then(
        (m) => m.AdminLayoutComponent,
      ),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/admin/admin-dashboard.component').then(
            (m) => m.AdminDashboardComponent,
          ),
      },
      {
        path: 'services',
        loadComponent: () =>
          import('./features/admin/admin-services.component').then(
            (m) => m.AdminServicesComponent,
          ),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./features/admin/admin-users.component').then(
            (m) => m.AdminUsersComponent,
          ),
      },
      {
        path: 'staff-invitations',
        loadComponent: () =>
          import('./features/admin/admin-staff-invitations.component').then(
            (m) => m.AdminStaffInvitationsComponent,
          ),
      },
      {
        path: 'orders',
        loadComponent: () =>
          import('./features/admin/admin-orders.component').then(
            (m) => m.AdminOrdersComponent,
          ),
      },
      {
        path: 'appointments',
        loadComponent: () =>
          import('./features/admin/admin-appointments.component').then(
            (m) => m.AdminAppointmentsComponent,
          ),
      },
      {
        path: 'monitoring',
        loadComponent: () =>
          import('./features/admin/admin-monitoring.component').then(
            (m) => m.AdminMonitoringComponent,
          ),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/admin/admin-settings.component').then(
            (m) => m.AdminSettingsComponent,
          ),
      },
      {
        path: 'security',
        loadComponent: () =>
          import('./features/admin/admin-security.component').then(
            (m) => m.AdminSecurityComponent,
          ),
      },
      {
        path: 'logs',
        loadComponent: () =>
          import('./features/admin/admin-logs.component').then(
            (m) => m.AdminLogsComponent,
          ),
      },
      {
        path: 'quotations',
        loadComponent: () =>
          import('./features/admin/admin-quotations.component').then(
            (m) => m.AdminQuotationsComponent,
          ),
      },
      {
        path: 'invoices',
        loadComponent: () =>
          import('./features/admin/admin-invoices.component').then(
            (m) => m.AdminInvoicesComponent,
          ),
      },
      {
        path: 'projects',
        loadComponent: () =>
          import('./features/admin/admin-projects.component').then(
            (m) => m.AdminProjectsComponent,
          ),
      },
      {
        path: 'tickets',
        loadComponent: () =>
          import('./features/admin/admin-tickets.component').then(
            (m) => m.AdminTicketsComponent,
          ),
      },
      // Sous-route inconnue dans /admin/* → 404 dans le shell admin
      {
        path: '**',
        loadComponent: () =>
          import('./features/errors/not-found.component').then(
            (m) => m.NotFoundComponent,
          ),
      },
    ],
  },

  // Catch-all racine : doit être la TOUTE DERNIÈRE route. Toute URL non
  // matchée par les routes ci-dessus tombe ici → page 404 stylisée.
  {
    path: '**',
    loadComponent: () =>
      import('./features/errors/not-found.component').then(
        (m) => m.NotFoundComponent,
      ),
  },
];
