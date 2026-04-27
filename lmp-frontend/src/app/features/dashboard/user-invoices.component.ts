import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  LucideAngularModule, Receipt, RefreshCw, Filter, X, Download, CreditCard, Loader2,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { AppCurrencyPipe } from '../../shared/pipes/app-currency.pipe';
import { PortalStubService } from '../../core/stubs/portal-stub.service';
import { Invoice, InvoiceStatus } from '../../core/stubs/portal.models';

@Component({
  selector: 'lmp-user-invoices',
  standalone: true,
  imports: [DatePipe, NgClass, FormsModule, LucideAngularModule, HlmButton, AppCurrencyPipe],
  template: `
    <div class="flex h-full flex-col overflow-hidden">
      <!-- Summary strip -->
      @if (items().length > 0) {
        <div class="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
          <div class="rounded-md border border-(--border) bg-(--card) p-4">
            <p class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">À régler</p>
            <p class="mt-1 text-xl font-semibold text-(--foreground)">
              {{ totalOutstanding() | appCurrency }}
            </p>
          </div>
          <div class="rounded-md border border-(--border) bg-(--card) p-4">
            <p class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">En retard</p>
            <p class="mt-1 text-xl font-semibold"
              [ngClass]="totalOverdue() > 0 ? 'text-red-600 dark:text-red-400' : 'text-(--foreground)'"
            >
              {{ totalOverdue() | appCurrency }}
            </p>
          </div>
          <div class="rounded-md border border-(--border) bg-(--card) p-4">
            <p class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Payé 2026</p>
            <p class="mt-1 text-xl font-semibold text-(--foreground)">
              {{ totalPaid() | appCurrency }}
            </p>
          </div>
        </div>
      }

      <!-- Toolbar -->
      <div class="flex items-center justify-between gap-2 pb-4">
        <div></div>
        <div class="flex items-center gap-0.5">
          <button hlmBtn variant="ghost" size="icon" type="button"
            class="h-7 w-7 cursor-pointer text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-300"
            title="Actualiser" (click)="load()"
          ><lucide-icon [img]="RefreshCwIcon" [size]="15" [ngClass]="{ 'animate-spin': loading() }"></lucide-icon></button>
          <button hlmBtn variant="ghost" size="sm" type="button"
            class="h-7 cursor-pointer gap-1.5 px-2"
            [ngClass]="showFilter() ? 'bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200' : 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-800 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200'"
            (click)="showFilter.set(!showFilter())"
          ><lucide-icon [img]="FilterIcon" [size]="14"></lucide-icon><span class="text-sm">Filtre</span></button>
        </div>
      </div>

      @if (showFilter()) {
        <div class="flex items-center gap-2 border-b border-zinc-100 pb-3 dark:border-zinc-800">
          <select [(ngModel)]="statusFilter"
            class="h-8 w-44 cursor-pointer rounded-lg border border-zinc-200 bg-zinc-50 px-2 text-sm text-zinc-700 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
          >
            <option value="">Tous les statuts</option>
            @for (s of STATUS_OPTIONS; track s) { <option [value]="s">{{ s }}</option> }
          </select>
          @if (statusFilter) {
            <button type="button"
              class="flex h-8 cursor-pointer items-center gap-1 rounded-lg px-2 text-sm text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800"
              (click)="statusFilter = ''"
            ><lucide-icon [img]="XIcon" [size]="14"></lucide-icon>Effacer</button>
          }
        </div>
      }

      <!-- CRM-style list -->
      <div class="flex-1 overflow-auto">
        @if (loading()) {
          <div class="flex items-center justify-center py-16">
            <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-zinc-400"></lucide-icon>
          </div>
        } @else {
          <div class="mb-2 flex min-w-max items-center rounded-lg bg-zinc-100 py-1.5 text-sm font-normal leading-none text-zinc-500 dark:bg-zinc-800/70 dark:text-zinc-400">
            <div class="w-32 shrink-0 px-2">Facture</div>
            <div class="w-32 shrink-0 px-2 text-center">Date</div>
            <div class="w-32 shrink-0 px-2 text-center">Échéance</div>
            <div class="w-32 shrink-0 px-2 text-center">Total</div>
            <div class="w-32 shrink-0 px-2 text-center">Restant</div>
            <div class="w-32 shrink-0 px-2 text-center">Statut</div>
            <div class="w-32 shrink-0 px-2 text-center">Last Modified</div>
          </div>

          <div>
            @for (inv of paginated(); track inv.id) {
              <div
                class="group flex h-10 min-w-max cursor-pointer items-center border-b border-zinc-50 transition-colors hover:bg-zinc-50 dark:border-zinc-800/30 dark:hover:bg-zinc-900/50"
                (click)="selected.set(inv)"
              >
                <div class="w-32 shrink-0 truncate px-2 font-mono text-xs leading-normal text-zinc-700 dark:text-zinc-300">{{ inv.id }}</div>
                <div class="w-32 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ inv.postingDate | date: 'dd/MM/yyyy' }}
                </div>
                <div class="w-32 shrink-0 truncate px-2 text-center text-sm leading-normal"
                  [ngClass]="isOverdue(inv) ? 'text-red-500 font-medium' : 'text-zinc-600 dark:text-zinc-400'"
                >{{ inv.dueDate | date: 'dd/MM/yyyy' }}</div>
                <div class="w-32 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-900 dark:text-zinc-100">
                  {{ inv.grandTotal | appCurrency:inv.currency }}
                </div>
                <div class="w-32 shrink-0 truncate px-2 text-center text-sm leading-normal"
                  [ngClass]="inv.outstandingAmount > 0 ? 'font-medium text-zinc-900 dark:text-zinc-100' : 'text-zinc-500 dark:text-zinc-500'"
                >
                  {{ inv.outstandingAmount | appCurrency:inv.currency }}
                </div>
                <div class="w-32 shrink-0 px-2 text-center">
                  <span class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium" [ngClass]="getStatusClass(inv.status)">
                    {{ inv.status }}
                  </span>
                </div>
                <div class="w-32 shrink-0 px-2 text-center text-sm leading-normal text-zinc-500 dark:text-zinc-400">
                  {{ formatRelativeTimeFr(inv.postingDate) }}
                </div>
              </div>
            } @empty {
              <div class="py-12 text-center text-sm text-zinc-500 dark:text-zinc-400">
                @if (statusFilter) { Aucune facture ne correspond à ce filtre. }
                @else { Aucune facture trouvée. }
              </div>
            }
          </div>
        }
      </div>

      <!-- Footer pagination -->
      <div class="flex items-center justify-between border-t border-zinc-200 py-2 dark:border-zinc-800">
        <div class="inline-flex rounded-md border border-zinc-200 dark:border-zinc-700">
          @for (size of pageSizes; track size; let first = $first; let last = $last) {
            <button type="button"
              class="h-7 min-w-[2.25rem] px-2.5 text-sm font-normal transition-colors"
              [ngClass]="{
                'rounded-l-md': first,
                'rounded-r-md': last,
                'border-r border-zinc-200 dark:border-zinc-700': !last,
                'bg-zinc-100 text-zinc-900 dark:bg-zinc-800 dark:text-zinc-100': pageSize() === size,
                'bg-white text-zinc-600 hover:bg-zinc-50 hover:text-zinc-900 dark:bg-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200': pageSize() !== size
              }"
              (click)="changePageSize(size)"
            >{{ size }}</button>
          }
        </div>
        <div class="flex items-center gap-1 text-sm text-zinc-500 dark:text-zinc-400">
          <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ filtered().length }}</span>
          <span>of</span>
          <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ items().length }}</span>
        </div>
      </div>
    </div>

    @if (selected()) {
      <div class="fixed inset-0 z-50 bg-black/40" (click)="selected.set(null)"></div>
      <aside class="fixed inset-y-0 right-0 z-50 flex w-full max-w-xl flex-col border-l border-(--border) bg-(--card) shadow-2xl">
        <div class="flex items-center justify-between border-b border-(--border) px-5 py-3">
          <div>
            <p class="font-mono text-[11px] uppercase tracking-wider text-(--muted-foreground)">{{ selected()!.id }}</p>
            <h2 class="text-base font-semibold text-(--foreground)">Facture</h2>
          </div>
          <button class="cursor-pointer rounded-md p-1 text-zinc-500 hover:bg-(--accent)" (click)="selected.set(null)">
            <lucide-icon [img]="XIcon" [size]="18"></lucide-icon>
          </button>
        </div>
        <div class="flex-1 overflow-y-auto p-5">
          <div class="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Date</p>
              <p class="mt-0.5 text-(--foreground)">{{ selected()!.postingDate | date: 'mediumDate' }}</p>
            </div>
            <div>
              <p class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Échéance</p>
              <p class="mt-0.5" [ngClass]="isOverdue(selected()!) ? 'text-red-600 dark:text-red-400 font-medium' : 'text-(--foreground)'">
                {{ selected()!.dueDate | date: 'mediumDate' }}
              </p>
            </div>
            <div>
              <p class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Statut</p>
              <span class="mt-1 inline-flex rounded-full px-2 py-0.5 text-xs font-medium" [ngClass]="getStatusClass(selected()!.status)">
                {{ selected()!.status }}
              </span>
            </div>
          </div>

          <div class="mt-5">
            <h3 class="mb-2 text-sm font-medium text-(--foreground)">Lignes</h3>
            <div class="overflow-hidden rounded-md border border-(--border)">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-(--border) bg-(--muted)/40">
                    <th class="px-3 py-2 text-left text-[11px] uppercase tracking-wider text-(--muted-foreground)">Description</th>
                    <th class="px-3 py-2 text-right text-[11px] uppercase tracking-wider text-(--muted-foreground)">Qté</th>
                    <th class="px-3 py-2 text-right text-[11px] uppercase tracking-wider text-(--muted-foreground)">Total</th>
                  </tr>
                </thead>
                <tbody>
                  @for (it of selected()!.items; track it.itemCode) {
                    <tr class="border-b border-(--border) last:border-0">
                      <td class="px-3 py-2 text-(--foreground)">{{ it.description }}</td>
                      <td class="px-3 py-2 text-right text-(--muted-foreground)">{{ it.qty }}</td>
                      <td class="px-3 py-2 text-right font-medium text-(--foreground)">{{ it.amount | appCurrency }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>

          <div class="mt-4 rounded-md border border-(--border) bg-(--background) p-4 text-sm">
            <div class="flex justify-between text-(--muted-foreground)">
              <span>Total facture</span><span>{{ selected()!.grandTotal | appCurrency:selected()!.currency }}</span>
            </div>
            <div class="mt-1 flex justify-between text-(--muted-foreground)">
              <span>Payé</span><span>{{ selected()!.paidAmount | appCurrency:selected()!.currency }}</span>
            </div>
            <div class="mt-2 flex justify-between border-t border-(--border) pt-2 text-base font-semibold"
              [ngClass]="selected()!.outstandingAmount > 0 ? 'text-(--foreground)' : 'text-emerald-600 dark:text-emerald-400'">
              <span>Restant dû</span><span>{{ selected()!.outstandingAmount | appCurrency:selected()!.currency }}</span>
            </div>
          </div>
        </div>
        <div class="flex justify-end gap-2 border-t border-(--border) p-4">
          @if (selected()!.pdfUrl) {
            <a hlmBtn variant="outline" class="cursor-pointer gap-1" [href]="selected()!.pdfUrl">
              <lucide-icon [img]="DownloadIcon" [size]="14"></lucide-icon>
              Télécharger
            </a>
          }
          @if (canPay(selected()!)) {
            <button hlmBtn class="cursor-pointer gap-1">
              <lucide-icon [img]="CreditCardIcon" [size]="14"></lucide-icon>
              Payer maintenant
            </button>
          }
        </div>
      </aside>
    }
  `,
})
export class UserInvoicesComponent implements OnInit {
  private readonly stub = inject(PortalStubService);

  readonly STATUS_OPTIONS: InvoiceStatus[] = ['Draft', 'Submitted', 'Paid', 'Partly Paid', 'Unpaid', 'Overdue', 'Return', 'Credit Note Issued', 'Cancelled'];

  readonly RefreshCwIcon = RefreshCw;
  readonly FilterIcon = Filter;
  readonly XIcon = X;
  readonly DownloadIcon = Download;
  readonly CreditCardIcon = CreditCard;
  readonly ReceiptIcon = Receipt;
  readonly Loader2Icon = Loader2;

  readonly loading = signal(false);
  readonly showFilter = signal(false);
  readonly items = signal<Invoice[]>([]);
  readonly selected = signal<Invoice | null>(null);
  statusFilter = '';

  readonly pageSize = signal(20);
  readonly currentPage = signal(0);
  readonly pageSizes = [20, 50, 100];

  readonly filtered = computed(() =>
    this.statusFilter ? this.items().filter((i) => i.status === this.statusFilter) : this.items(),
  );

  readonly paginated = computed(() => {
    const start = this.currentPage() * this.pageSize();
    return this.filtered().slice(start, start + this.pageSize());
  });

  readonly totalOutstanding = computed(() =>
    this.items().reduce((sum, i) => sum + (i.outstandingAmount || 0), 0),
  );
  readonly totalOverdue = computed(() =>
    this.items().filter((i) => i.status === 'Overdue').reduce((sum, i) => sum + i.outstandingAmount, 0),
  );
  readonly totalPaid = computed(() =>
    this.items().reduce((sum, i) => sum + (i.paidAmount || 0), 0),
  );

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.stub.listInvoices().subscribe({
      next: (list) => { this.items.set(list); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  canPay(i: Invoice): boolean {
    return (i.status === 'Unpaid' || i.status === 'Overdue' || i.status === 'Partly Paid') && !!i.paymentUrl;
  }

  isOverdue(i: Invoice): boolean {
    return i.status === 'Overdue';
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
  }



  getStatusClass(status: InvoiceStatus): string {
    switch (status) {
      case 'Paid': return 'bg-green-500/10 text-green-500';
      case 'Unpaid': case 'Submitted': return 'bg-blue-500/10 text-blue-500';
      case 'Partly Paid': return 'bg-orange-500/10 text-orange-500';
      case 'Overdue': return 'bg-red-500/10 text-red-500';
      case 'Cancelled': case 'Return': case 'Credit Note Issued': case 'Draft':
        return 'bg-(--muted) text-(--foreground)';
      default: return 'bg-gray-500/10 text-gray-400';
    }
  }

  formatRelativeTimeFr(iso: string | null | undefined): string {
    if (!iso) return '—';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '—';
    const diffMs = Date.now() - d.getTime();
    const sec = Math.floor(diffMs / 1000);
    if (sec < 45) return 'À l\'instant';
    const min = Math.floor(sec / 60);
    const hours = Math.floor(min / 60);
    const days = Math.floor(hours / 24);
    if (min < 60) return min <= 1 ? 'Il y a 1 min' : `Il y a ${min} min`;
    if (hours < 24) return hours <= 1 ? 'Il y a 1 h' : `Il y a ${hours} h`;
    if (days < 7) return days === 1 ? 'Il y a 1 jour' : `Il y a ${days} j`;
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  }
}
