import { Component, signal } from '@angular/core';
import { LucideAngularModule, MessageCircle } from 'lucide-angular';
import { AppointmentModalComponent } from '../modals/appointment-modal.component';

@Component({
  selector: 'lmp-contact-fab',
  standalone: true,
  imports: [AppointmentModalComponent, LucideAngularModule],
  styles: [`
    .fab-btn {
      position: fixed;
      bottom: 24px;
      right: 24px;
      z-index: 1000;
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 10px 16px;
      border: 1px solid var(--border);
      border-radius: 8px;
      background: var(--card);
      color: var(--foreground);
      font-family: var(--font-sans), sans-serif;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: background-color 0.15s ease, border-color 0.15s ease;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
    }

    .fab-btn:hover {
      background: var(--accent);
      border-color: var(--primary);
    }

    @media (max-width: 480px) {
      .fab-btn {
        bottom: 16px;
        right: 16px;
        width: 40px;
        height: 40px;
        padding: 0;
        justify-content: center;
      }
      .contact-text { display: none; }
    }
  `],
  template: `
    <button class="fab-btn" (click)="openModal()">
      <lucide-icon [img]="MessageCircleIcon" [size]="16"></lucide-icon>
      <span class="contact-text">Contact</span>
    </button>

    <lmp-appointment-modal
      [isOpen]="isModalOpen()"
      (closed)="isModalOpen.set(false)"
    />
  `,
})
export class ContactFabComponent {
  readonly MessageCircleIcon = MessageCircle;
  readonly isModalOpen = signal(false);

  openModal(): void {
    this.isModalOpen.set(true);
  }
}
