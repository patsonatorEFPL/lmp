# Guide de Migration - Unification de la Logique d'Achat

## 🎯 Objectif de la Migration
Harmoniser la logique d'achat entre la page d'accueil et la page services en adoptant l'architecture sécurisée de [`services.html`](src/main/resources/templates/services.html) dans [`index.html`](src/main/resources/templates/index.html).

## ⚠️ Pré-requis

### Vérifications Avant Migration
```bash
# 1. Vérifier l'existence des composants requis
ls src/main/resources/templates/fragments/booking-modal.html
ls src/main/resources/static/js/register-checkout.js

# 2. Sauvegarder le fichier original
cp src/main/resources/templates/index.html src/main/resources/templates/index.html.backup

# 3. Vérifier l'état du serveur
# S'assurer que l'application fonctionne correctement
```

### Composants Requis ✅
- ✅ [`fragments/booking-modal.html`](src/main/resources/templates/fragments/booking-modal.html) - Fragment modal unifié
- ✅ [`register-checkout.js`](src/main/resources/static/js/register-checkout.js) - Script de gestion d'achat
- ✅ APIs backend fonctionnelles (`/api/orders/*`, `/stripe/checkout/*`)

## 📝 Étapes de Migration

### Étape 1: Ajout des Métadonnées CSRF et AUTH_INFO

#### 1.1 Localiser la section `<head>` (ligne 3)
**Position**: Après `<div th:replace="fragments/styles :: global-styles"></div>` (ligne 10)

#### 1.2 Ajouter les métadonnées CSRF
```html
<!-- CSRF Token pour sécurité -->
<meta name="_csrf" th:content="${_csrf.token}"/>
<meta name="_csrf_header" th:content="${_csrf.headerName}"/>
```

#### 1.3 Ajouter les informations d'authentification
```html
<!-- Authentication Info pour JavaScript -->
<script th:inline="javascript">
    window.AUTH_INFO = {
        isAuthenticated: [[${#authorization.expression('isAuthenticated()')}]],
        currentUser: [[${#authentication.name}]],
        roles: [[${#authentication.authorities}]]
    };
</script>
```

### Étape 2: Modification des Boutons d'Achat

#### 2.1 Service 1 - Sécurisations Google My Business (ligne 142)
**AVANT**:
```html
<button onclick="selectService('Sécurisations et Accès Google My Business', 353.89)" class="flex-1 bg-primary text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-primary/90 transition-all">
    Acheter
</button>
```

**APRÈS**:
```html
<button onclick="openBookingModal('Sécurisations et Accès Google My Business', 353.89)" class="flex-1 bg-primary text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-primary/90 transition-all">
    Acheter
</button>
```

#### 2.2 Service 2 - Référencement VIP+ (ligne 168)
**AVANT**: `selectService('Référencement Optimale avec Sécurisations Garantie VIP+', 750.79)`
**APRÈS**: `openBookingModal('Référencement Optimale avec Sécurisations Garantie VIP+', 750.79)`

#### 2.3 Service 3 - Gestion des Avis (ligne 194)
**AVANT**: `selectService('Gestion des Avis', 747.43)`
**APRÈS**: `openBookingModal('Gestion des Avis', 747.43)`

#### 2.4 Service 4 - Présence Locales (ligne 220)
**AVANT**: `selectService('Présence Locales', 2200.00)`
**APRÈS**: `openBookingModal('Présence Locales', 2200.00)`

#### 2.5 Service 5 - Création Site Web (ligne 246)
**AVANT**: `selectService('Création Site Web', 100.99)`
**APRÈS**: `openBookingModal('Création Site Web', 100.99)`

#### 2.6 Service 6 - SEO et Référencement (ligne 272)
**AVANT**: `selectService('SEO et Référencement Naturel', 5500.00)`
**APRÈS**: `openBookingModal('SEO et Référencement Naturel', 5500.00)`

#### 2.7 Service 7 - Optimisations Google+ (ligne 298)
**AVANT**: `selectService('Optimisations Google+', 11121.00)`
**APRÈS**: `openBookingModal('Optimisations Google+', 11121.00)`

#### 2.8 Service 8 - Assistance Technique (ligne 324)
**AVANT**: `selectService('Assistance Technique', 402.00)`
**APRÈS**: `openBookingModal('Assistance Technique', 402.00)`

### Étape 3: Modification du Bouton Contact (ligne 381)

**AVANT**:
```html
<button onclick="openBookingModal()" class="bg-yellow text-dark px-8 py-4 rounded-lg font-semibold hover:bg-yellow/90 transition-all duration-200">
    Prendre un rendez-vous
</button>
```

**APRÈS**:
```html
<button onclick="openBookingModal('Consultation Gratuite', 0)" class="bg-yellow text-dark px-8 py-4 rounded-lg font-semibold hover:bg-yellow/90 transition-all duration-200">
    Prendre un rendez-vous
</button>
```

### Étape 4: Suppression des Modals Obsolètes

#### 4.1 Supprimer le Modal de Rendez-vous (lignes 395-433)
**SUPPRIMER COMPLÈTEMENT**:
```html
<!-- Booking Modal -->
<div id="bookingModal" class="fixed inset-0 bg-black bg-opacity-50 z-50 hidden">
    <!-- ... tout le contenu du modal ... -->
</div>
```

#### 4.2 Supprimer le Modal de Paiement (lignes 435-456)
**SUPPRIMER COMPLÈTEMENT**:
```html
<!-- Payment Modal -->
<div id="paymentModal" class="fixed inset-0 bg-black bg-opacity-50 z-50 hidden">
    <!-- ... tout le contenu du modal ... -->
</div>
```

### Étape 5: Ajout du Fragment Unifié

#### 5.1 Ajouter avant `</main>` (après ligne 390)
```html
<!-- Modal unifié pour l'achat de services -->
<div th:replace="fragments/booking-modal :: booking-modal"></div>
```

### Étape 6: Nettoyage du JavaScript

#### 6.1 Supprimer les Fonctions Obsolètes (lignes 479-606)
**SUPPRIMER**:
- `function openBookingModal()` (ligne 480)
- `function closeBookingModal()` (ligne 484)
- `function openPaymentModal()` (ligne 489)
- `function closePaymentModal()` (ligne 494)
- `function selectService()` (ligne 498)
- `function processPayment()` (ligne 520)
- `function createOrderAndInitiatePayment()` (ligne 567)

#### 6.2 Modifier `handleEmailSubmit()` (ligne 509)
**GARDER INCHANGÉ** - Cette fonction reste valide

#### 6.3 Modifier `handleBookingSubmit()` (ligne 514)
**SUPPRIMER** - Cette fonction sera remplacée par [`register-checkout.js`](src/main/resources/static/js/register-checkout.js)

#### 6.4 Conserver les Event Listeners (lignes 608-620)
**MODIFIER** - Adapter pour le nouveau modal:
```javascript
// Close modals when clicking outside
document.addEventListener('click', function(event) {
    const bookingModal = document.getElementById('bookingModal');
    
    if (event.target === bookingModal) {
        closeBookingModal(); // Fonction fournie par register-checkout.js
    }
});
```

### Étape 7: Inclusion du Script Externe

#### 7.1 Ajouter avant `</body>` (après ligne 620)
```html
<!-- Script unifié pour la logique d'achat -->
<script th:src="@{/js/register-checkout.js}"></script>
```

## 🧪 Tests de Validation

### Phase de Tests 1: Vérifications Techniques

#### Test A: Chargement de la Page
```bash
# 1. Redémarrer l'application
./mvnw spring-boot:run

# 2. Ouvrir la page d'accueil
# Vérifier dans la console du navigateur:
# - Pas d'erreur JavaScript
# - AUTH_INFO est défini
# - Tokens CSRF présents
```

#### Test B: Fonctions JavaScript
```javascript
// Dans la console du navigateur
console.log(window.AUTH_INFO); // Doit afficher l'objet d'authentification
console.log(typeof openBookingModal); // Doit être "function"
console.log(typeof closeBookingModal); // Doit être "function"
```

### Phase de Tests 2: Flux Utilisateur Non Connecté

#### Test C: Achat sans Connexion
1. **Se déconnecter** de l'application
2. **Aller** sur la page d'accueil
3. **Cliquer** sur "Acheter" pour n'importe quel service
4. **Vérifier** que le modal d'inscription s'ouvre
5. **Remplir** le formulaire avec de vraies données
6. **Valider** l'inscription
7. **Vérifier** la redirection vers Stripe

#### Test D: Bouton Contact
1. **Cliquer** sur "Prendre un rendez-vous"
2. **Vérifier** que le modal s'ouvre avec "Consultation Gratuite"
3. **Vérifier** que le montant est 0,00 CAD

### Phase de Tests 3: Flux Utilisateur Connecté

#### Test E: Achat avec Connexion
1. **Se connecter** à l'application
2. **Aller** sur la page d'accueil
3. **Cliquer** sur "Acheter" pour n'importe quel service
4. **Vérifier** la redirection directe vers Stripe (pas de modal)

### Phase de Tests 4: Cohérence avec Services

#### Test F: Comparaison des Flux
1. **Tester** l'achat sur la page d'accueil
2. **Tester** l'achat sur la page services
3. **Vérifier** que le comportement est identique:
   - Même modal pour les non-connectés
   - Même redirection pour les connectés
   - Même gestion d'erreurs

## 🔍 Points de Contrôle

### ✅ Checklist Technique
- [ ] Métadonnées CSRF ajoutées
- [ ] Script AUTH_INFO ajouté
- [ ] 8 boutons d'achat modifiés
- [ ] Bouton contact adapté
- [ ] Anciens modals supprimés
- [ ] Fragment booking-modal ajouté
- [ ] JavaScript obsolète supprimé
- [ ] Script register-checkout.js inclus

### ✅ Checklist Fonctionnelle
- [ ] Page se charge sans erreur
- [ ] Modal s'ouvre pour les non-connectés
- [ ] Redirection directe pour les connectés
- [ ] Formulaire d'inscription fonctionne
- [ ] Redirection Stripe opérationnelle
- [ ] Gestion d'erreurs cohérente

### ✅ Checklist Sécurité
- [ ] Tokens CSRF présents et fonctionnels
- [ ] Authentification vérifiée correctement
- [ ] Pas de bypass de sécurité
- [ ] Validation côté serveur active

## 🚨 Dépannage

### Problèmes Courants

#### Erreur: "openBookingModal is not defined"
**Cause**: Script [`register-checkout.js`](src/main/resources/static/js/register-checkout.js) non chargé
**Solution**: Vérifier l'inclusion du script avant `</body>`

#### Erreur: "CSRF token not found"
**Cause**: Métadonnées CSRF manquantes
**Solution**: Vérifier l'ajout des balises `<meta>` dans `<head>`

#### Modal ne s'ouvre pas
**Cause**: Conflit entre ancien et nouveau code
**Solution**: Vérifier la suppression complète des anciennes fonctions

#### Redirection Stripe échoue
**Cause**: Problème de backend ou de données
**Solution**: Vérifier les logs serveur et les données envoyées

## 📊 Validation Finale

### Test de Régression Complet
1. **Naviguer** sur toutes les pages du site
2. **Tester** tous les flux d'achat
3. **Vérifier** la cohérence entre pages
4. **Valider** la sécurité CSRF
5. **Contrôler** les performances

### Critères de Succès ✅
- ✅ **Fonctionnalité**: Tous les achats fonctionnent
- ✅ **Sécurité**: CSRF et authentification opérationnels
- ✅ **Cohérence**: Comportement identique entre pages
- ✅ **Performance**: Pas de dégradation notable
- ✅ **UX**: Expérience utilisateur fluide

## 🎉 Résultat Final

Après cette migration, la page d'accueil aura:
- **Logique d'achat unifiée** avec la page services
- **Sécurité renforcée** avec CSRF et authentification
- **Architecture moderne** avec composants réutilisables
- **Expérience utilisateur cohérente** sur tout le site

La demande "*le fait de cliquer sur acheter sur la page d'accueil et sur la page service n'a pas la même logique*" sera ainsi entièrement résolue.