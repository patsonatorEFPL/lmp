import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Carte/panneau conteneur (header optionnel + zone de contenu).
 * Le contenu principal et les actions d'en-tête sont projetés via ng-content.
 * Utilise les classes .lmpd-panel / .lmpd-panel-head déjà présentes dans styles.css.
 */
@Component({
  selector: 'lmp-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="lmpd-panel">
      @if (title() || hasActions()) {
        <header class="lmpd-panel-head">
          @if (title()) {
            <h3>{{ title() }}</h3>
          }
          <ng-content select="[panel-actions]"></ng-content>
        </header>
      }
      <ng-content></ng-content>
    </section>
  `,
})
export class PanelComponent {
  readonly title = input<string>('');
  readonly hasActions = input<boolean>(false);
}
