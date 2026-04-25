import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export interface DonutSegment {
  label: string;
  value: number;
  color: string;
}

interface DonutArc {
  dash: number;
  offset: number;
  color: string;
}

/**
 * Diagramme en anneau avec légende intégrée (utilisé pour la répartition
 * des commandes par catégorie, côté utilisateur).
 */
@Component({
  selector: 'lmp-donut-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="lmpd-donut-wrap">
      <svg [attr.width]="size" [attr.height]="size" [attr.viewBox]="'0 0 ' + size + ' ' + size" role="img">
        <circle [attr.cx]="cx" [attr.cy]="cy" [attr.r]="r" fill="none" stroke="var(--lmpd-bg-sub)" stroke-width="14" />
        @for (a of arcs(); track $index) {
          <circle
            [attr.cx]="cx"
            [attr.cy]="cy"
            [attr.r]="r"
            fill="none"
            [attr.stroke]="a.color"
            stroke-width="14"
            [attr.stroke-dasharray]="a.dash + ' ' + (circ - a.dash)"
            [attr.stroke-dashoffset]="-a.offset"
            [attr.transform]="'rotate(-90 ' + cx + ' ' + cy + ')'"
            stroke-linecap="butt"
          />
        }
        <text [attr.x]="cx" [attr.y]="cy - 2" text-anchor="middle" style="font-size:20px;font-weight:600;fill:var(--lmpd-fg)">{{ total() }}</text>
        <text [attr.x]="cx" [attr.y]="cy + 14" text-anchor="middle" style="font-size:10px;fill:var(--lmpd-fg-mute)">{{ centerLabel() }}</text>
      </svg>
      <div class="lmpd-donut-legend">
        @for (s of segments(); track s.label) {
          <div class="lmpd-lg">
            <span class="lmpd-sw" [style.background]="s.color"></span>
            <span>{{ s.label }}</span>
            <span class="lmpd-vl">{{ s.value }}</span>
          </div>
        }
      </div>
    </div>
  `,
})
export class DonutChartComponent {
  readonly segments = input.required<readonly DonutSegment[]>();
  readonly centerLabel = input<string>('');

  readonly size = 140;
  readonly r = 54;
  readonly cx = this.size / 2;
  readonly cy = this.size / 2;
  readonly circ = 2 * Math.PI * this.r;

  readonly total = computed(() => this.segments().reduce((s, x) => s + x.value, 0));

  readonly arcs = computed<DonutArc[]>(() => {
    const total = this.total() || 1;
    let offset = 0;
    return this.segments().map((s) => {
      const dash = (s.value / total) * this.circ;
      const arc: DonutArc = { dash, offset, color: s.color };
      offset += dash;
      return arc;
    });
  });
}
