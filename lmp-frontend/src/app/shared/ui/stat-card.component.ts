import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LucideAngularModule, LucideIconData, ArrowDown, ArrowUp } from 'lucide-angular';

import { SparklineComponent } from './sparkline.component';

/**
 * Carte statistique (valeur + delta + sparkline optionnelle).
 * Réutilisée par les deux dashboards (utilisateur & admin) pour éviter le boilerplate.
 */
@Component({
  selector: 'lmp-stat-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LucideAngularModule, SparklineComponent],
  template: `
    <div class="lmpd-stat" [class.is-accent]="accent()">
      <div class="lmpd-stat-row1">
        <span class="lmpd-stat-label">{{ label() }}</span>
        <span class="lmpd-stat-icon">
          <lucide-icon [img]="icon()" [size]="15"></lucide-icon>
        </span>
      </div>
      <div class="lmpd-stat-value">{{ value() }}</div>
      <div class="lmpd-stat-foot">
        @if (delta() !== null && delta() !== undefined) {
          <span class="lmpd-delta" [class.is-up]="isUp()" [class.is-down]="!isUp()">
            <lucide-icon [img]="isUp() ? ArrowUpIcon : ArrowDownIcon" [size]="10"></lucide-icon>
            {{ absoluteDelta() }}%
          </span>
        }
        <span>{{ footer() }}</span>
      </div>
      @if (spark()?.length) {
        <lmp-sparkline [points]="spark()!" />
      }
    </div>
  `,
})
export class StatCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<string | number>();
  readonly icon = input.required<LucideIconData>();
  readonly footer = input<string>('');
  readonly delta = input<number | null | undefined>(null);
  readonly accent = input<boolean>(false);
  readonly spark = input<readonly number[] | null>(null);

  readonly ArrowUpIcon = ArrowUp;
  readonly ArrowDownIcon = ArrowDown;

  readonly isUp = computed(() => (this.delta() ?? 0) >= 0);
  readonly absoluteDelta = computed(() => Math.abs(this.delta() ?? 0));
}
