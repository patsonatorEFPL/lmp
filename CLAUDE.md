# LMP — Project Instructions

## Frontend rule (impeccable design skill)

**Toute modification frontend doit passer par le skill `impeccable`** — installé globalement (`~/.claude/skills/impeccable`).

- Pour design / refonte / nouveau composant : `/impeccable craft` ou `/impeccable shape`.
- Pour audit visuel ou critique : `/impeccable audit` ou `/impeccable critique`.
- Pour polish / refine d'un écran existant : `/impeccable polish [target]`.
- Pour setup projet (one-time) : `/impeccable teach` (génère `PRODUCT.md`) puis `/impeccable document` (génère `DESIGN.md`).
- 23 commandes au total — `/impeccable` pour la liste.

**Ne pas utiliser le skill `frontend-design` d'Anthropic en parallèle** — impeccable est un superset opinionated, mélanger les deux brouille les défauts (couleurs OKLCH vs hex, grille 8px, anti-patterns cards/gradients/glassmorphism, etc.).

**Workflow :**
1. Avant tout travail UI dans `lmp-frontend/` ou les templates `src/main/resources/templates/`, vérifier que `PRODUCT.md` + `DESIGN.md` existent à la racine ou dans `lmp-frontend/`. Sinon → lancer `/impeccable teach` puis `/impeccable document` d'abord.
2. Tout changement visuel passe par une commande `/impeccable …`, pas par génération CSS/HTML libre.
3. Avant de déclarer une UI terminée → `/impeccable audit` + tests E2E navigateur (cf. mémoire `e2e-browser-systematic`).

Doc : https://impeccable.style/ — repo : https://github.com/pbakaus/impeccable
