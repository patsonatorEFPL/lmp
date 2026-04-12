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
    path: 'payment/process',
    loadComponent: () =>
      import('./features/payment/payment-process.component').then(
        (m) => m.PaymentProcessComponent,
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
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings.component').then(
            (m) => m.SettingsComponent,
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
        path: 'settings',
        loadComponent: () =>
          import('./features/admin/admin-settings.component').then(
            (m) => m.AdminSettingsComponent,
          ),
      },
    ],
  },

  // Fallback
  {
    path: '**',
    redirectTo: '',
  },
];
