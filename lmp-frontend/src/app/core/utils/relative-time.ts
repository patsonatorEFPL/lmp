/**
 * Formate une date ISO en temps relatif français : « À l'instant »,
 * « Il y a 5 min », « Dans 2 h »… Au-delà de {@code maxDays} jours,
 * bascule sur la date absolue (ex. « 12 juin 2026 »).
 */
export function formatRelativeTimeFr(iso: string | null | undefined, maxDays = 7): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  const diffMs = d.getTime() - Date.now();
  const abs = Math.abs(diffMs);
  const sec = Math.floor(abs / 1000);
  if (sec < 45) return 'À l\'instant';
  const min = Math.floor(sec / 60);
  const hours = Math.floor(min / 60);
  const days = Math.floor(hours / 24);
  const prefix = diffMs > 0 ? 'Dans ' : 'Il y a ';
  if (min < 60) return `${prefix}${min <= 1 ? '1 min' : min + ' min'}`;
  if (hours < 24) return `${prefix}${hours <= 1 ? '1 h' : hours + ' h'}`;
  if (days < maxDays) return `${prefix}${days === 1 ? '1 jour' : days + ' j'}`;
  return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
}
