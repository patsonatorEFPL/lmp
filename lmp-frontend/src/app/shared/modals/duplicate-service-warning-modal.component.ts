import { Component, EventEmitter, Input, Output } from '@angular/core';
import { HlmButton } from '@spartan-ng/helm/button';

export interface ActiveServiceInfo {
  orderId: string;
  status: string;
  createdAt: string | null;
  serviceName: string;
}

const STATUS_LABEL: Record<string, string> = {
  CONFIRMED: 'Confirmée',
  PROCESSING: 'En traitement',
  IN_PROGRESS: 'En cours',
  SHIPPED: 'Expédiée',
  DELIVERED: 'Livrée',
  COMPLETED: 'Terminée',
  UNDER_REVIEW: 'En révision',
};

@Component({
  selector: 'lmp-duplicate-service-warning-modal',
  standalone: true,
  imports: [HlmButton],
  template: `
    @if (isOpen && service) {
      <div
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
        (click)="onCancel()"
      >
        <div
          class="relative w-full max-w-md rounded-sm border border-(--border) bg-(--card) p-6 sm:p-8 shadow-sm"
          (click)="$event.stopPropagation()"
          role="dialog"
          aria-modal="true"
          aria-labelledby="dup-modal-title"
        >
          <div class="flex items-start gap-3 mb-4">
            <div class="flex-shrink-0 mt-0.5">
              <svg class="h-5 w-5 text-amber-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <h2 id="dup-modal-title" class="text-base font-semibold text-(--foreground)">
                Service déjà actif
              </h2>
              <p class="mt-1 text-sm text-(--muted-foreground)">
                Vous avez déjà une commande active pour <span class="font-medium text-(--foreground)">{{ service.serviceName }}</span>.
              </p>
            </div>
          </div>

          <div class="rounded-sm border border-(--border) bg-(--background) px-4 py-3 mb-5">
            <dl class="space-y-1.5 text-sm">
              <div class="flex items-center justify-between gap-4">
                <dt class="text-(--muted-foreground)">Statut</dt>
                <dd class="font-medium text-(--foreground)">{{ statusLabel() }}</dd>
              </div>
              @if (formattedDate()) {
                <div class="flex items-center justify-between gap-4">
                  <dt class="text-(--muted-foreground)">Depuis le</dt>
                  <dd class="font-medium text-(--foreground)">{{ formattedDate() }}</dd>
                </div>
              }
              <div class="flex items-center justify-between gap-4">
                <dt class="text-(--muted-foreground)">Référence</dt>
                <dd class="font-mono text-xs text-(--foreground)">{{ shortRef() }}</dd>
              </div>
            </dl>
          </div>

          <p class="text-sm text-(--muted-foreground) mb-6">
            Souhaitez-vous voir cette commande, en passer une nouvelle, ou annuler&nbsp;?
          </p>

          <div class="flex flex-col-reverse sm:flex-row gap-2 sm:justify-end">
            <button
              type="button"
              hlmBtn
              variant="ghost"
              size="sm"
              (click)="onCancel()"
            >
              Annuler
            </button>
            <button
              type="button"
              hlmBtn
              variant="outline"
              size="sm"
              (click)="onProceedAnyway()"
            >
              Commander quand même
            </button>
            <button
              type="button"
              hlmBtn
              variant="default"
              size="sm"
              (click)="onViewOrder()"
            >
              Voir ma commande
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class DuplicateServiceWarningModalComponent {
  @Input() isOpen = false;
  @Input() service: ActiveServiceInfo | null = null;

  @Output() readonly cancel = new EventEmitter<void>();
  @Output() readonly proceedAnyway = new EventEmitter<void>();
  @Output() readonly viewOrder = new EventEmitter<string>();

  statusLabel(): string {
    if (!this.service) return '';
    return STATUS_LABEL[this.service.status] ?? this.service.status;
  }

  formattedDate(): string {
    if (!this.service?.createdAt) return '';
    try {
      const d = new Date(this.service.createdAt);
      return d.toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: 'long',
        year: 'numeric',
      });
    } catch {
      return '';
    }
  }

  shortRef(): string {
    return this.service?.orderId.slice(0, 8) ?? '';
  }

  onCancel(): void {
    this.cancel.emit();
  }

  onProceedAnyway(): void {
    this.proceedAnyway.emit();
  }

  onViewOrder(): void {
    if (this.service) this.viewOrder.emit(this.service.orderId);
  }
}
