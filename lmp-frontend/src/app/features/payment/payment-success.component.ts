import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgClass, CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {
  LucideAngularModule,
  CheckCircle,
  Loader2,
  XCircle,
  ArrowRight,
  ShoppingCart,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { environment } from '../../../environments/environment';

interface PaymentStatusResponse {
  orderId: string;
  status: string;
  paymentStatus: string;
  ready: boolean;
  paymentConfirmed: boolean;
}

interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
}

@Component({
  selector: 'lmp-payment-success',
  standalone: true,
  imports: [NgClass, CurrencyPipe, DatePipe, LucideAngularModule, HlmButton, RouterLink],
  template: `
    <section class="flex min-h-screen items-center justify-center bg-(--background) px-4">
      <div class="w-full max-w-md text-center">
        <!-- Loading / Polling state -->
        @if (polling()) {
          <div class="flex flex-col items-center gap-6 rounded-2xl border border-(--border) bg-(--card) p-10 shadow-lg">
            <div class="flex h-16 w-16 items-center justify-center rounded-full bg-(--primary)/10">
              <lucide-icon
                [img]="Loader2Icon"
                [size]="32"
                class="animate-spin text-(--primary)"
              ></lucide-icon>
            </div>
            <div>
              <h2 class="font-display text-xl font-bold text-(--foreground)">
                Confirmation en cours…
              </h2>
              <p class="mt-2 text-sm text-(--muted-foreground)">
                Nous vérifions votre paiement auprès de Stripe. Cela ne prendra que quelques secondes.
              </p>
            </div>
            <div class="h-1.5 w-full overflow-hidden rounded-full bg-(--muted)">
              <div class="h-full w-1/2 animate-pulse rounded-full bg-(--primary)"></div>
            </div>
          </div>
        }

        <!-- Success state -->
        @if (confirmed()) {
          <div class="flex flex-col items-center gap-6 rounded-2xl border border-emerald-500/20 bg-(--card) p-10 shadow-lg">
            <div class="flex h-20 w-20 items-center justify-center rounded-full bg-emerald-500/10">
              <lucide-icon
                [img]="CheckCircleIcon"
                [size]="40"
                class="text-emerald-500"
              ></lucide-icon>
            </div>
            <div>
              <h2 class="font-display text-2xl font-bold text-(--foreground)">
                Paiement confirmé !
              </h2>
              <p class="mt-2 text-sm text-(--muted-foreground)">
                Votre commande a été enregistrée avec succès. Vous recevrez un email de confirmation avec votre facture.
              </p>
            </div>

            @if (orderStatus()) {
              <div class="w-full rounded-lg border border-(--border) bg-(--background) p-4 text-left">
                <div class="flex items-center gap-3 text-sm">
                  <span class="text-(--muted-foreground)">Statut :</span>
                  <span class="inline-flex rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-xs font-semibold text-emerald-500">
                    Paiement confirmé
                  </span>
                </div>
                <div class="mt-2 flex items-center gap-3 text-sm">
                  <span class="text-(--muted-foreground)">Commande :</span>
                  <span class="font-mono text-xs text-(--foreground)">{{ orderId()?.substring(0, 8) }}...</span>
                </div>
              </div>
            }

            <div class="flex w-full flex-col gap-2 sm:flex-row">
              <a
                hlmBtn
                variant="default"
                size="default"
                routerLink="/dashboard"
                class="w-full cursor-pointer gap-2"
              >
                Mon tableau de bord
                <lucide-icon [img]="ArrowRightIcon" [size]="16"></lucide-icon>
              </a>
              <a
                hlmBtn
                variant="outline"
                size="default"
                routerLink="/services"
                class="w-full cursor-pointer gap-2"
              >
                <lucide-icon [img]="ShoppingCartIcon" [size]="16"></lucide-icon>
                Nos services
              </a>
            </div>
          </div>
        }

        <!-- Error state -->
        @if (error()) {
          <div class="flex flex-col items-center gap-6 rounded-2xl border border-red-500/20 bg-(--card) p-10 shadow-lg">
            <div class="flex h-20 w-20 items-center justify-center rounded-full bg-red-500/10">
              <lucide-icon
                [img]="XCircleIcon"
                [size]="40"
                class="text-red-500"
              ></lucide-icon>
            </div>
            <div>
              <h2 class="font-display text-xl font-bold text-(--foreground)">
                Erreur de vérification
              </h2>
              <p class="mt-2 text-sm text-(--muted-foreground)">
                {{ error() }}
              </p>
            </div>
            <a
              hlmBtn
              variant="default"
              size="default"
              routerLink="/services"
              class="cursor-pointer gap-2"
            >
              Retour aux services
              <lucide-icon [img]="ArrowRightIcon" [size]="16"></lucide-icon>
            </a>
          </div>
        }
      </div>
    </section>
  `,
})
export class PaymentSuccessComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);

  readonly CheckCircleIcon = CheckCircle;
  readonly Loader2Icon = Loader2;
  readonly XCircleIcon = XCircle;
  readonly ArrowRightIcon = ArrowRight;
  readonly ShoppingCartIcon = ShoppingCart;

  readonly polling = signal(true);
  readonly confirmed = signal(false);
  readonly error = signal<string | null>(null);
  readonly orderId = signal<string | null>(null);
  readonly orderStatus = signal<string | null>(null);

  private pollInterval: ReturnType<typeof setInterval> | null = null;
  private pollCount = 0;
  private readonly maxPolls = 30; // 30 attempts × 2s = 60s max

  ngOnInit(): void {
    const orderIdParam = this.route.snapshot.queryParamMap.get('order_id');
    const sessionId = this.route.snapshot.queryParamMap.get('session_id');

    if (!orderIdParam) {
      this.polling.set(false);
      this.error.set('Identifiant de commande manquant. Veuillez contacter le support.');
      return;
    }

    this.orderId.set(orderIdParam);
    this.startPolling(orderIdParam);
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  private startPolling(orderId: string): void {
    // Check immediately
    this.checkPaymentStatus(orderId);

    // Then poll every 2 seconds
    this.pollInterval = setInterval(() => {
      this.pollCount++;
      if (this.pollCount >= this.maxPolls) {
        this.stopPolling();
        this.polling.set(false);
        this.error.set(
          'La confirmation de votre paiement prend plus de temps que prévu. ' +
          'Votre paiement a bien été reçu par Stripe, mais la confirmation automatique n\'a pas encore été traitée. ' +
          'Veuillez vérifier votre tableau de bord ou contacter le support si le problème persiste.'
        );
        return;
      }
      this.checkPaymentStatus(orderId);
    }, 2000);
  }

  private stopPolling(): void {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
      this.pollInterval = null;
    }
  }

  private checkPaymentStatus(orderId: string): void {
    this.http
      .get<ApiResponse<PaymentStatusResponse>>(
        `${environment.apiUrl}/api/v1/payments/status/${orderId}`,
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          if (res.data?.ready) {
            this.stopPolling();
            this.polling.set(false);
            if (res.data.paymentConfirmed) {
              this.confirmed.set(true);
              this.orderStatus.set(res.data.status);
            } else {
              // Payment was processed but NOT confirmed (cancelled, refunded, or failed)
              this.error.set(
                'Votre paiement n\'a pas pu être confirmé. Statut : ' +
                res.data.status + '. Veuillez contacter le support si vous pensez qu\'il s\'agit d\'une erreur.'
              );
            }
          }
        },
        error: () => {
          // Don't stop polling on error, might be temporary
        },
      });
  }
}
