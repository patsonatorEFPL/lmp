import { Component, signal } from '@angular/core';
import { LucideAngularModule, MessageCircle } from 'lucide-angular';
import { AppointmentModalComponent } from '../modals/appointment-modal.component';

@Component({
  selector: 'lmp-contact-fab',
  standalone: true,
  imports: [AppointmentModalComponent, LucideAngularModule],
  styles: [`
    @keyframes slideInRight {
      from { transform: translateX(200%); opacity: 0; }
      to   { transform: translateX(0);    opacity: 1; }
    }

    @keyframes pulse-glow {
      0%   { box-shadow: 0 10px 40px rgba(16,185,129,.4), 0 0 0 0   rgba(16,185,129,.4); }
      50%  { box-shadow: 0 10px 40px rgba(16,185,129,.6), 0 0 0 20px rgba(16,185,129,0);  }
      100% { box-shadow: 0 10px 40px rgba(16,185,129,.4), 0 0 0 0   rgba(16,185,129,0);  }
    }

    @keyframes shake {
      0%,50%,100% { transform: rotate(0deg);   }
      10%          { transform: rotate(-10deg); }
      20%          { transform: rotate(10deg);  }
      30%          { transform: rotate(-10deg); }
      40%          { transform: rotate(10deg);  }
    }

    @keyframes shine {
      0%   { transform: translateX(-100%) translateY(-100%) rotate(45deg); }
      100% { transform: translateX(100%)  translateY(100%)  rotate(45deg); }
    }

    .fab-btn {
      position:      fixed;
      bottom:        30px;
      right:         30px;
      z-index:       1000;
      display:       flex;
      align-items:   center;
      gap:           8px;
      padding:       13px 21px;
      border:        none;
      border-radius: 42px;
      background:    linear-gradient(135deg, #059669 0%, #10B981 100%);
      box-shadow:    0 10px 40px rgba(16,185,129,.41);
      color:         #fff;
      font-family:   var(--font-sans), sans-serif;
      font-size:     11px;
      font-weight:   700;
      letter-spacing: 0.5px;
      text-transform: uppercase;
      cursor:        pointer;
      overflow:      hidden;
      transition:    all 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55);
      animation:     slideInRight 0.8s ease-out forwards,
                     pulse-glow   2s ease    infinite;
    }

    .fab-btn::after {
      content:    '';
      position:   absolute;
      top:        -50%;
      left:       -50%;
      width:      200%;
      height:     200%;
      background: linear-gradient(45deg,
        transparent 30%,
        rgba(255,255,255,.1) 50%,
        transparent 70%
      );
      transform: rotate(45deg);
      animation: shine 3s infinite;
    }

    .fab-btn:hover {
      transform:  translateY(-6px) scale(1.05);
      box-shadow: 0 14px 42px rgba(16,185,129,.5);
      background: linear-gradient(135deg, #047857 0%, #059669 100%);
    }

    .fab-btn:active {
      transform: translateY(-2px) scale(1.02);
    }

    .contact-text {
      position: relative;
      z-index:  2;
    }

    @media (max-width: 768px) {
      .fab-btn { bottom: 20px; right: 20px; padding: 10px 17px; font-size: 10px; }
    }

    @media (max-width: 480px) {
      .fab-btn {
        bottom:         15px;
        right:          15px;
        width:          48px;
        height:         48px;
        padding:        0;
        border-radius:  50%;
        justify-content: center;
      }
      .contact-text { display: none; }
    }
  `],
  template: `
    <button class="fab-btn" (click)="openModal()">
      <lucide-icon [img]="MessageCircleIcon" [size]="17" class="relative z-[2]"></lucide-icon>
      <span class="contact-text">CONTACTEZ-NOUS</span>
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
