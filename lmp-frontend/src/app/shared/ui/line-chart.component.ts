import { AfterViewInit, ChangeDetectionStrategy, Component, computed, DestroyRef, ElementRef, inject, input, signal } from '@angular/core';

interface GridLine {
  y: number;
  v: number;
}

/**
 * Courbe de série temporelle avec zone remplie et série secondaire optionnelle
 * (ex. « période précédente ») — fidèle au design Claude.
 */
@Component({
  selector: 'lmp-line-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      class="lmpd-chart"
      [attr.viewBox]="'0 0 ' + svgWidth() + ' ' + height()"
      role="img"
    >
      <g class="lmpd-chart-grid">
        @for (g of gridLines(); track $index) {
          <line [attr.x1]="pad.l" [attr.x2]="svgWidth() - pad.r" [attr.y1]="g.y" [attr.y2]="g.y" />
        }
      </g>
      <g class="lmpd-chart-axis">
        @for (g of gridLines(); track $index) {
          <text [attr.x]="pad.l - 6" [attr.y]="g.y + 3" text-anchor="end">{{ g.v }}</text>
        }
        @if (labels(); as lb) {
          @for (l of lb; track $index; let i = $index) {
            <text [attr.x]="xAt(i)" [attr.y]="height() - pad.b + 14" text-anchor="middle">{{ l }}</text>
          }
        }
      </g>
      <path class="lmpd-chart-area" [attr.d]="areaPath()"></path>
      @if (secondaryPath(); as p) {
        <path class="lmpd-chart-line is-secondary" [attr.d]="p"></path>
      }
      <path class="lmpd-chart-line" [attr.d]="linePath()"></path>
      @for (d of data(); track $index; let i = $index) {
        @if (i % 2 === 0) {
          <circle class="lmpd-chart-dot" [attr.cx]="xAt(i)" [attr.cy]="yAt(d)" r="2.5"></circle>
        }
      }
    </svg>
  `,
})
export class LineChartComponent implements AfterViewInit {
  private readonly el = inject(ElementRef);
  private readonly destroyRef = inject(DestroyRef);

  readonly data = input.required<readonly number[]>();
  readonly secondary = input<readonly number[] | null>(null);
  readonly labels = input<readonly string[] | null>(null);
  readonly height = input(200);

  readonly svgWidth = signal(720);
  readonly pad = { l: 32, r: 16, t: 14, b: 22 };

  private readonly max = computed(() => {
    const d = this.data();
    const s = this.secondary() ?? [];
    if (d.length === 0 && s.length === 0) return 0;
    return Math.max(...d, ...s);
  });
  private readonly range = computed(() => {
    const m = this.max();
    return m > 0 ? m : 1;
  });

  ngAfterViewInit(): void {
    const ro = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const w = Math.floor(entry.contentRect.width);
        if (w > 0) {
          this.svgWidth.set(w);
        }
      }
    });
    ro.observe(this.el.nativeElement);
    this.destroyRef.onDestroy(() => ro.disconnect());
  }

  xAt(i: number): number {
    const n = this.data().length;
    const denom = Math.max(n - 1, 1);
    return this.pad.l + (i / denom) * (this.svgWidth() - this.pad.l - this.pad.r);
  }

  yAt(v: number): number {
    const usable = this.height() - this.pad.t - this.pad.b;
    return this.pad.t + (1 - v / this.range()) * usable;
  }

  readonly linePath = computed(() =>
    this.data()
      .map((v, i) => `${i === 0 ? 'M' : 'L'}${this.xAt(i).toFixed(1)},${this.yAt(v).toFixed(1)}`)
      .join(' '),
  );

  readonly areaPath = computed(() => {
    const line = this.linePath();
    const n = this.data().length;
    if (!line || n === 0) return '';
    const yBottom = (this.height() - this.pad.b).toFixed(1);
    return `${line} L${this.xAt(n - 1).toFixed(1)},${yBottom} L${this.xAt(0).toFixed(1)},${yBottom} Z`;
  });

  readonly secondaryPath = computed(() => {
    const s = this.secondary();
    if (!s?.length) return null;
    return s
      .map((v, i) => `${i === 0 ? 'M' : 'L'}${this.xAt(i).toFixed(1)},${this.yAt(v).toFixed(1)}`)
      .join(' ');
  });

  readonly gridLines = computed<GridLine[]>(() => {
    const ticks = 4;
    const max = this.max();
    const usable = this.height() - this.pad.t - this.pad.b;
    return Array.from({ length: ticks + 1 }, (_, i) => ({
      y: this.pad.t + (i / ticks) * usable,
      v: Math.round(max - (i / ticks) * max),
    }));
  });
}
