import { Pipe, PipeTransform, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { environment } from '../../../environments/environment';

/**
 * Pipe de formatage monétaire qui utilise la devise par défaut configurée
 * dans {@link environment.defaultCurrency}.
 *
 * <p>Wrap le {@link CurrencyPipe} natif d'Angular avec les paramètres
 * par défaut du projet (symbole, 2 décimales, locale fr).
 * Si un code devise explicite est fourni, il est utilisé à la place.
 *
 * <p>Exemples :
 * <ul>
 *   <li>{{ 1234.5 | appCurrency }} → 1 234,50 €</li>
 *   <li>{{ 1234.5 | appCurrency:'USD' }} → 1 234,50 $</li>
 * </ul>
 */
@Pipe({
  name: 'appCurrency',
  standalone: true,
})
export class AppCurrencyPipe implements PipeTransform {
  private readonly currencyPipe = inject(CurrencyPipe);

  transform(
    value: number | string | null | undefined,
    currencyCode?: string,
  ): string | null {
    if (value == null) return null;
    return this.currencyPipe.transform(
      value,
      currencyCode || environment.defaultCurrency,
      'symbol',
      '1.2-2',
      environment.defaultLocale,
    );
  }
}
