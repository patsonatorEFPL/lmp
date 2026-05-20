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
        class="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/55 backdrop-blur-[2px] p-0 sm:p-4"
        (click)="onCancel()"
      >
        <div
          class="relative w-full sm:max-w-[400px] rounded-t-md sm:rounded-md border border-(--border) bg-(--card) shadow-sm"
          (click)="$event.stopPropagation()"
          role="dialog"
          aria-modal="true"
          aria-labelledby="dup-modal-title"
        >
          <div class="px-5 pt-5 pb-1">
            <h2 id="dup-modal-title" class="text-[15px] font-semibold leading-tight text-(--foreground)">
              Service déjà actif
            </h2>
            <p class="mt-1.5 text-[13px] leading-relaxed text-(--muted-foreground)">
              Vous avez déjà <span class="text-(--foreground)">{{ service.serviceName }}</span> en cours.
            </p>
          </div>

          <dl class="px-5 py-3 grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-[13px] border-t border-(--border)/60 mt-2">
            <dt class="text-(--muted-foreground)">Statut</dt>
            <dd class="text-right font-medium text-(--foreground)">{{ statusLabel() }}</dd>
            @if (formattedDate()) {
              <dt class="text-(--muted-foreground)">Depuis</dt>
              <dd class="text-right text-(--foreground)">{{ formattedDate() }}</dd>
            }
            <dt class="text-(--muted-foreground)">Référence</dt>
            <dd class="text-right font-mono text-[12px] text-(--foreground)">{{ shortRef() }}</dd>
          </dl>

          <div class="flex items-center justify-end gap-1 px-3 py-3 border-t border-(--border)/60">
            <button
              type="button"
              hlmBtn
              variant="ghost"
              size="sm"
              class="text-(--muted-foreground)"
              (click)="onCancel()"
            >
              Annuler
            </button>
            <button
              type="button"
              hlmBtn
              variant="ghost"
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
