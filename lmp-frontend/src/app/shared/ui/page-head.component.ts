import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * En-tête de page de dashboard : titre, sous-titre, actions à droite.
 * Les actions sont projetées via <ng-content select="[actions]">.
 * S'appuie sur les classes .lmpd-page-head / .lmpd-sub / .lmpd-actions
 * déjà définies dans styles.css.
 */
@Component({
  selector: 'lmp-page-head',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="lmpd-page-head">
      <div>
        <h1>{{ title() }}</h1>
        @if (subtitle()) {
          <p class="lmpd-sub">{{ subtitle() }}</p>
        }
      </div>
      <div class="lmpd-actions">
        <ng-content select="[actions]"></ng-content>
      </div>
    </header>
  `,
})
export class PageHeadComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string>('');
}
