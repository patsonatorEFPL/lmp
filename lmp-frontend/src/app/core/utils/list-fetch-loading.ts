import { signal, WritableSignal } from '@angular/core';

/**
 * Spinner de liste : affiché au premier chargement, masqué sur les rechargements
 * silencieux (polling, SSE) une fois une première réponse reçue (succès ou erreur).
 */
export function createListFetchLoading(loading: WritableSignal<boolean>) {
  const settled = signal(false);
  return {
    beforeFetch(silent: boolean): void {
      if (!silent || !settled()) {
        loading.set(true);
      }
    },
    afterFetch(): void {
      settled.set(true);
      loading.set(false);
    },
  };
}
