import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, LucideIconData, ArrowRight } from 'lucide-angular';

/**
 * Tuile d'action rapide, ancrée sur un route Angular.
 * Uniforme entre les dashboards admin et utilisateur.
 */
@Component({
  selector: 'lmp-quick-action',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, LucideAngularModule],
  template: `
    <a class="lmpd-qa" [routerLink]="route()">
      <span class="lmpd-qa-ic">
        <lucide-icon [img]="icon()" [size]="15"></lucide-icon>
      </span>
      <div class="lmpd-qa-body">
        <div class="lmpd-qa-tt">{{ title() }}</div>
        <div class="lmpd-qa-ds">{{ description() }}</div>
      </div>
      <span class="lmpd-qa-ar">
        <lucide-icon [img]="ArrowRightIcon" [size]="14"></lucide-icon>
      </span>
    </a>
  `,
})
export class QuickActionComponent {
  readonly icon = input.required<LucideIconData>();
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly route = input.required<string>();

  readonly ArrowRightIcon = ArrowRight;
}
