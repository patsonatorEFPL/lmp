import { isPlatformBrowser } from '@angular/common';
import { DestroyRef, Injectable, PLATFORM_ID, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { fromEvent, interval, merge } from 'rxjs';
import { filter } from 'rxjs/operators';

/**
 * Déclenche un rafraîchissement périodique tant que l’onglet est visible,
 * plus un tick à la reprise du focus (visibility) pour rattraper l’état.
 */
@Injectable({ providedIn: 'root' })
export class VisiblePollService {
  private readonly platformId = inject(PLATFORM_ID);

  /**
   * @param intervalMs 0 ou négatif = désactivé
   */
  subscribeWhileVisible(destroyRef: DestroyRef, intervalMs: number, tick: () => void): void {
    if (!isPlatformBrowser(this.platformId) || intervalMs <= 0) {
      return;
    }

    const whenVisible = () => !document.hidden;

    merge(
      interval(intervalMs).pipe(filter(whenVisible)),
      fromEvent(document, 'visibilitychange').pipe(filter(whenVisible)),
    )
      .pipe(takeUntilDestroyed(destroyRef))
      .subscribe(() => tick());
  }
}
