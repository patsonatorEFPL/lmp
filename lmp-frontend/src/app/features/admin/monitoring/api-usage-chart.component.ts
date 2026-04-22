import { Component, Input, signal, computed } from '@angular/core';

export interface ApiUsageItem {
  name: string;
  successCalls: number;
  errorCalls: number;
}

@Component({
  selector: 'lmp-api-usage-chart',
  standalone: true,
  template: `
    <div class="rounded border border-zinc-200/90 bg-white p-5 shadow-sm dark:border-zinc-800 dark:bg-zinc-950/80">
      <h3 class="text-sm font-semibold text-(--foreground)">
        Utilisation par API
      </h3>

      @if (items().length === 0) {
        <div class="flex items-center justify-center py-12">
          <p class="text-xs text-(--muted-foreground)">Aucune donnée disponible</p>
        </div>
      } @else {
        <div class="mt-4 space-y-2.5">
          @for (item of items(); track item.name) {
            <div class="flex items-center gap-3">
              <!-- API name -->
              <span
                class="w-24 shrink-0 truncate text-right text-xs font-medium text-(--muted-foreground)"
                [title]="item.name"
              >{{ item.name }}</span>

              <!-- Bar -->
              <div class="relative h-5 flex-1 overflow-hidden rounded-xs bg-zinc-100 dark:bg-zinc-800">
                <!-- Success bar -->
                <div
                  class="absolute inset-y-0 left-0 rounded-xs bg-blue-500 transition-all duration-500"
                  [style.width.%]="barWidth(item.successCalls)"
                ></div>
                <!-- Error bar -->
                @if (item.errorCalls > 0) {
                  <div
                    class="absolute inset-y-0 rounded-xs bg-red-500 transition-all duration-500"
                    [style.left.%]="barWidth(item.successCalls)"
                    [style.width.%]="barWidth(item.errorCalls)"
                  ></div>
                }
              </div>

              <!-- Count -->
              <div class="flex w-20 shrink-0 items-center justify-end gap-1">
                <span class="text-xs font-semibold text-(--foreground)">
                  {{ item.successCalls + item.errorCalls }}
                </span>
                @if (item.errorCalls > 0) {
                  <span class="text-[11px] font-medium text-red-500 dark:text-red-400">
                    ({{ item.errorCalls }})
                  </span>
                }
              </div>
            </div>
          }
        </div>

        <!-- Legend -->
        <div class="mt-4 flex items-center gap-4">
          <div class="flex items-center gap-1.5">
            <span class="h-2.5 w-2.5 rounded-full bg-blue-500"></span>
            <span class="text-[11px] text-(--muted-foreground)">Succès</span>
          </div>
          <div class="flex items-center gap-1.5">
            <span class="h-2.5 w-2.5 rounded-full bg-red-500"></span>
            <span class="text-[11px] text-(--muted-foreground)">Erreurs</span>
          </div>
        </div>
      }
    </div>
  `,
})
export class ApiUsageChartComponent {
  @Input() set dataInput(value: ApiUsageItem[]) {
    this.items.set(value);
  }

  readonly items = signal<ApiUsageItem[]>([]);

  readonly maxCalls = computed(() =>
    Math.max(...this.items().map(i => i.successCalls + i.errorCalls), 1)
  );

  barWidth(count: number): number {
    return (count / this.maxCalls()) * 100;
  }
}
