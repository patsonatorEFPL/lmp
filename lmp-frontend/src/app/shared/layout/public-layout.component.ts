import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './navbar.component';
import { FooterComponent } from './footer.component';
import { ContactFabComponent } from './contact-fab.component';

@Component({
  selector: 'lmp-public-layout',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, FooterComponent, ContactFabComponent],
  template: `
    <lmp-navbar />
    <main
      class="min-h-screen pt-14"
      style="min-height: 100vh; padding-top: 3.5rem; display: block;"
    >
      <router-outlet />
    </main>
    <lmp-footer />
    <lmp-contact-fab />
  `,
  styles: `
    :host {
      display: block;
    }
  `,
})
export class PublicLayoutComponent {}
