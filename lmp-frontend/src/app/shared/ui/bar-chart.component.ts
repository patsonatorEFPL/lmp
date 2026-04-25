import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Histogramme empilé (barre principale + segment secondaire), utilisé
 * pour les revenus admin sur 16 semaines (ponctuel vs récurrent).
 */
@Component({
  selector: 'lmp-bar-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="lmpd-bars">
      @for (v of normalized(); track $index) {
        <div class="lmpd-bar2">
          <span class="lmpd-st" [style.height.%]="v"></span>
          <span class="lmpd-st is-s2" [style.height.%]="v * secondaryFactor()"></span>
        </div>
      }
    </div>
    @if (labels()?.length) {
      <div class="lmpd-bars-foot">
        @for (l of labels(); track $index) {
          <span>{{ l }}</span>
        }
      </div>
    }
  `,
})
export class BarChartComponent {
  readonly values = input.required<readonly number[]>();
  readonly labels = input<readonly string[] | null>(null);
  readonly secondaryFactor = input<number>(0.35);

  readonly normalized = computed(() => {
    const vals = this.values();
    if (vals.length === 0) return [] as number[];
    const max = Math.max(...vals, 1);
    return vals.map((v) => Math.round((v / max) * 100));
  });
}
