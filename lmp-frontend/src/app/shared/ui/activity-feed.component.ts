import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideAngularModule, LucideIconData } from 'lucide-angular';

export interface ActivityFeedItem {
  icon: LucideIconData;
  title: string;
  detail?: string;
  meta: string;
}

/**
 * Flux chronologique d'événements (shared.jsx#Feed), partagé entre les deux dashboards.
 * Utilise les classes .lmpd-feed / .lmpd-feed-item déjà définies dans styles.css.
 */
@Component({
  selector: 'lmp-activity-feed',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LucideAngularModule],
  template: `
    <div class="lmpd-feed">
      @for (it of items(); track $index) {
        <div class="lmpd-feed-item">
          <span class="lmpd-feed-ic">
            <lucide-icon [img]="it.icon" [size]="13"></lucide-icon>
          </span>
          <div>
            <div class="lmpd-feed-tt">
              <b>{{ it.title }}</b>
              @if (it.detail) {
                — {{ it.detail }}
              }
            </div>
          </div>
          <span class="lmpd-feed-ts">{{ it.meta }}</span>
        </div>
      }
    </div>
  `,
})
export class ActivityFeedComponent {
  readonly items = input.required<readonly ActivityFeedItem[]>();
}
