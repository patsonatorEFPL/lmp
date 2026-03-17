import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NotificationToastComponent } from './shared/layout/notification-toast.component';

@Component({
  selector: 'lmp-root',
  imports: [RouterOutlet, NotificationToastComponent],
  template: `
    <router-outlet />
    <lmp-notification-toast />
  `,
  styles: `
    :host {
      display: block;
      min-height: 100vh;
    }
  `,
})
export class App {}
