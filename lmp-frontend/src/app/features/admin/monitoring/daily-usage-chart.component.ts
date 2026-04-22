import {
  Component,
  Input,
  computed,
  signal,
  HostListener,
  ElementRef,
  inject,
} from '@angular/core';

export interface DailyDataPoint {
  date: string;       // e.g. "2026-04-12"
  calls: number;
  successRate: number; // 0–100
}

@Component({
  selector: 'lmp-daily-usage-chart',
  standalone: true,
  template: `
    <div class="rounded border border-zinc-200/90 bg-white p-5 shadow-sm dark:border-zinc-800 dark:bg-zinc-950/80">
      <h3 class="text-sm font-semibold text-(--foreground)">
        Appels quotidiens (7 derniers jours)
      </h3>

      @if (data().length === 0) {
        <div class="flex items-center justify-center py-12">
          <p class="text-xs text-(--muted-foreground)">Aucune donnée disponible</p>
        </div>
      } @else {
        <div class="relative mt-4">
          <svg
            [attr.viewBox]="'0 0 ' + svgWidth + ' ' + svgHeight"
            class="w-full"
            preserveAspectRatio="xMidYMid meet"
            (mousemove)="onMouseMove($event)"
            (mouseleave)="onMouseLeave()"
          >
            <!-- Grid lines -->
            @for (tick of yTicks(); track tick) {
              <line
                [attr.x1]="margin.left"
                [attr.y1]="yScale(tick)"
                [attr.x2]="svgWidth - margin.right"
                [attr.y2]="yScale(tick)"
                stroke="currentColor"
                class="text-zinc-100 dark:text-zinc-800"
                stroke-width="1"
              />
              <text
                [attr.x]="margin.left - 8"
                [attr.y]="yScale(tick) + 3"
                text-anchor="end"
                class="fill-zinc-400 dark:fill-zinc-500"
                font-size="10"
              >{{ tick }}</text>
            }

            <!-- X-axis labels -->
            @for (point of data(); track point.date; let i = $index) {
              <text
                [attr.x]="xScale(i)"
                [attr.y]="svgHeight - 4"
                text-anchor="middle"
                class="fill-zinc-400 dark:fill-zinc-500"
                font-size="10"
              >{{ formatDateLabel(point.date) }}</text>
            }

            <!-- Calls line -->
            <polyline
              [attr.points]="callsPolyline()"
              fill="none"
              stroke="#3b82f6"
              stroke-width="2"
              stroke-linejoin="round"
              stroke-linecap="round"
            />

            <!-- Area fill under calls line -->
            <polygon
              [attr.points]="callsAreaPolygon()"
              fill="url(#callsGradient)"
              opacity="0.15"
            />

            <!-- Success rate line -->
            <polyline
              [attr.points]="successPolyline()"
              fill="none"
              stroke="#14b8a6"
              stroke-width="2"
              stroke-linejoin="round"
              stroke-linecap="round"
              stroke-dasharray="4 3"
            />

            <!-- Data point dots -->
            @for (point of data(); track point.date; let i = $index) {
              <circle
                [attr.cx]="xScale(i)"
                [attr.cy]="yScale(point.calls)"
                r="3"
                fill="#3b82f6"
                class="transition-all"
                [attr.opacity]="hoveredIndex() === i ? 1 : 0.6"
              />
              <circle
                [attr.cx]="xScale(i)"
                [attr.cy]="successYScale(point.successRate)"
                r="3"
                fill="#14b8a6"
                class="transition-all"
                [attr.opacity]="hoveredIndex() === i ? 1 : 0.6"
              />
            }

            <!-- Hover vertical line -->
            @if (hoveredIndex() !== null) {
              <line
                [attr.x1]="xScale(hoveredIndex()!)"
                [attr.y1]="margin.top"
                [attr.x2]="xScale(hoveredIndex()!)"
                [attr.y2]="svgHeight - margin.bottom"
                stroke="currentColor"
                class="text-zinc-300 dark:text-zinc-600"
                stroke-width="1"
                stroke-dasharray="3 3"
              />
            }

            <!-- Gradient defs -->
            <defs>
              <linearGradient id="callsGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#3b82f6" stop-opacity="0.4"/>
                <stop offset="100%" stop-color="#3b82f6" stop-opacity="0"/>
              </linearGradient>
            </defs>
          </svg>

          <!-- Tooltip -->
          @if (hoveredIndex() !== null && hoveredPoint()) {
            <div
              class="pointer-events-none absolute z-10 rounded border border-zinc-200 bg-white px-3 py-2 shadow-sm dark:border-zinc-700 dark:bg-zinc-800"
              [style.left.px]="tooltipX()"
              [style.top.px]="8"
            >
              <p class="text-[10px] font-medium text-(--muted-foreground)">
                {{ formatDateFull(hoveredPoint()!.date) }}
              </p>
              <div class="mt-1 flex items-center gap-3">
                <div class="flex items-center gap-1.5">
                  <span class="h-2 w-2 rounded-full bg-blue-500"></span>
                  <span class="text-xs font-semibold text-(--foreground)">{{ hoveredPoint()!.calls }} appels</span>
                </div>
                <div class="flex items-center gap-1.5">
                  <span class="h-2 w-2 rounded-full bg-teal-500"></span>
                  <span class="text-xs font-semibold text-(--foreground)">{{ hoveredPoint()!.successRate }}%</span>
                </div>
              </div>
            </div>
          }
        </div>

        <!-- Legend -->
        <div class="mt-3 flex items-center gap-5">
          <div class="flex items-center gap-1.5">
            <span class="h-0.5 w-4 rounded bg-blue-500"></span>
            <span class="text-[11px] text-(--muted-foreground)">Appels</span>
          </div>
          <div class="flex items-center gap-1.5">
            <span class="h-0.5 w-4 rounded bg-teal-500" style="background: repeating-linear-gradient(90deg, #14b8a6 0, #14b8a6 4px, transparent 4px, transparent 7px);"></span>
            <span class="text-[11px] text-(--muted-foreground)">Taux de succès</span>
          </div>
        </div>
      }
    </div>
  `,
})
export class DailyUsageChartComponent {
  private readonly el = inject(ElementRef);

  @Input() set dataInput(value: DailyDataPoint[]) {
    this.data.set(value);
  }

  readonly data = signal<DailyDataPoint[]>([]);
  readonly hoveredIndex = signal<number | null>(null);

  // SVG dimensions
  readonly svgWidth = 800;
  readonly svgHeight = 220;
  readonly margin = { top: 16, right: 16, bottom: 28, left: 40 };

  readonly chartWidth = this.svgWidth - this.margin.left - this.margin.right;
  readonly chartHeight = this.svgHeight - this.margin.top - this.margin.bottom;

  readonly maxCalls = computed(() => {
    const max = Math.max(...this.data().map(d => d.calls), 1);
    return Math.ceil(max * 1.2);
  });

  readonly yTicks = computed(() => {
    const max = this.maxCalls();
    if (max <= 5) return Array.from({ length: max + 1 }, (_, i) => i);
    const step = Math.ceil(max / 5);
    const ticks: number[] = [];
    for (let i = 0; i <= max; i += step) ticks.push(i);
    return ticks;
  });

  xScale(index: number): number {
    const count = this.data().length;
    if (count <= 1) return this.margin.left + this.chartWidth / 2;
    return this.margin.left + (index / (count - 1)) * this.chartWidth;
  }

  yScale(value: number): number {
    return this.margin.top + this.chartHeight - (value / this.maxCalls()) * this.chartHeight;
  }

  successYScale(rate: number): number {
    // Map 0-100% to chart height
    return this.margin.top + this.chartHeight - (rate / 100) * this.chartHeight;
  }

  readonly callsPolyline = computed(() =>
    this.data().map((d, i) => `${this.xScale(i)},${this.yScale(d.calls)}`).join(' ')
  );

  readonly callsAreaPolygon = computed(() => {
    const pts = this.data().map((d, i) => `${this.xScale(i)},${this.yScale(d.calls)}`);
    const last = this.data().length - 1;
    const baseline = this.yScale(0);
    return [...pts, `${this.xScale(last)},${baseline}`, `${this.xScale(0)},${baseline}`].join(' ');
  });

  readonly successPolyline = computed(() =>
    this.data().map((d, i) => `${this.xScale(i)},${this.successYScale(d.successRate)}`).join(' ')
  );

  readonly hoveredPoint = computed(() => {
    const idx = this.hoveredIndex();
    if (idx === null) return null;
    return this.data()[idx] ?? null;
  });

  readonly tooltipX = computed(() => {
    const idx = this.hoveredIndex();
    if (idx === null) return 0;
    const containerWidth = this.el.nativeElement.querySelector('svg')?.clientWidth ?? this.svgWidth;
    const ratio = containerWidth / this.svgWidth;
    return this.xScale(idx) * ratio - 60;
  });

  onMouseMove(event: MouseEvent): void {
    const svg = event.currentTarget as SVGSVGElement;
    const rect = svg.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const ratio = this.svgWidth / rect.width;
    const svgX = x * ratio;

    const count = this.data().length;
    if (count === 0) return;

    let closest = 0;
    let minDist = Infinity;
    for (let i = 0; i < count; i++) {
      const dist = Math.abs(this.xScale(i) - svgX);
      if (dist < minDist) {
        minDist = dist;
        closest = i;
      }
    }
    this.hoveredIndex.set(closest);
  }

  onMouseLeave(): void {
    this.hoveredIndex.set(null);
  }

  formatDateLabel(dateStr: string): string {
    try {
      const d = new Date(dateStr + 'T00:00:00');
      return d.toLocaleDateString('fr-FR', { month: '2-digit', day: '2-digit' });
    } catch {
      return dateStr;
    }
  }

  formatDateFull(dateStr: string): string {
    try {
      const d = new Date(dateStr + 'T00:00:00');
      return d.toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric', month: 'short' });
    } catch {
      return dateStr;
    }
  }
}
