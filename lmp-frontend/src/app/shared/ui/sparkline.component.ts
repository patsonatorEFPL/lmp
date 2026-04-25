import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Mini-courbe décorative affichée au coin des stats cards.
 * Rend un SVG inline — pas de dépendance lourde type chart.js.
 */
@Component({
  selector: 'lmp-sparkline',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      class="lmpd-spark"
      [attr.viewBox]="viewBox()"
      preserveAspectRatio="none"
      aria-hidden="true"
    >
      <path [attr.d]="areaPath()" fill="var(--lmpd-accent)" opacity="0.12" />
      <path
        [attr.d]="linePath()"
        fill="none"
        stroke="var(--lmpd-accent)"
        stroke-width="1.25"
        stroke-linejoin="round"
        stroke-linecap="round"
      />
    </svg>
  `,
})
export class SparklineComponent {
  readonly points = input.required<readonly number[]>();
  readonly width = input(80);
  readonly height = input(28);

  readonly viewBox = computed(() => `0 0 ${this.width()} ${this.height()}`);

  private readonly coords = computed(() => {
    const pts = this.points();
    if (pts.length === 0) return [] as [number, number][];
    const w = this.width();
    const h = this.height();
    const max = Math.max(...pts);
    const min = Math.min(...pts);
    const range = max - min || 1;
    const denom = Math.max(pts.length - 1, 1);
    return pts.map(
      (v, i) => [(i / denom) * w, h - ((v - min) / range) * h] as [number, number],
    );
  });

  readonly linePath = computed(() =>
    this.coords()
      .map((p, i) => `${i === 0 ? 'M' : 'L'}${p[0].toFixed(1)},${p[1].toFixed(1)}`)
      .join(' '),
  );

  readonly areaPath = computed(() => {
    const line = this.linePath();
    if (!line) return '';
    const w = this.width();
    const h = this.height();
    return `${line} L${w},${h} L0,${h} Z`;
  });
}
