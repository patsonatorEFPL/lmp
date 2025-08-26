# Nouveau Flux d'Authentification et de Checkout Sécurisé

## Vue d'ensemble

Ce document décrit le nouveau système d'authentification et de checkout intégré mis en place pour sécuriser le processus d'achat de services. Le nouveau flux élimine les utilisateurs temporaires et garantit que tous les achats sont effectués par des utilisateurs authentifiés.

## Problèmes résolus

### Problèmes de sécurité identifiés dans l'ancien système :

1. **Création d'utilisateurs temporaires non sécurisée**
   - Mots de passe en clair stockés (`TEMP_PASSWORD`)
   - Emails basés sur les sessions sans validation
   - Aucune authentification requise pour créer des commandes

2. **Endpoints publics exposés**
   - `/api/orders/create-temp` accessible sans authentification
   - Possibilité de création illimitée de commandes
   - Aucun contrôle d'accès aux informations de commande

3. **Flux d'achat non sécurisé**
   - Les visiteurs pouvaient commander sans créer de compte réel
   - Aucune traçabilité des commandes
   - Risque de spam et d'abus du système

## Nouveau Flux Sécurisé

### Processus d'inscription et d'achat intégré :

1. **Visiteur clique sur "Commander un service"**
   - Le modal d'inscription complète s'ouvre
   - Tous les champs requis sont affichés (informations personnelles, adresse, mot de passe)

2. **Remplissage du formulaire d'inscription**
   - Validation en temps réel côté client
   - Vérification de la correspondance des mots de passe
   - Validation des champs obligatoires

3. **Soumission du formulaire**
   - Envoi des données vers `/register-and-checkout`
   - Création du compte utilisateur avec mot de passe haché (BCrypt)
   - Connexion automatique de l'utilisateur
   - Création de la commande liée au compte

4. **Redirection vers Stripe Checkout**
   - Session Stripe créée automatiquement
   - Redirection immédiate vers la page de paiement
   - Retour vers le dashboard utilisateur après paiement

## Architecture Technique

### Nouveaux Composants

#### 1. RegisterWithOrderDto
```java
// DTO pour l'inscription avec commande intégrée
- Informations personnelles (prénom, nom, email, téléphone)
- Informations d'adresse de facturation
- Sécurité (mot de passe, confirmation)
- Informations de commande (service, montant, devise)
- Acceptation des conditions d'utilisation
```

#### 2. AuthController - Endpoint `/register-and-checkout`
```java
@PostMapping("/register-and-checkout")
@ResponseBody
public ResponseEntity<?> registerAndCheckout(@Valid @RequestBody RegisterWithOrderDto registerWithOrderDto, HttpServletRequest request)
```

**Fonctionnalités :**
- Validation complète des données d'inscription
- Création sécurisée du compte utilisateur
- Connexion automatique après inscription
- Création de la commande liée au compte
- Retour des informations pour redirection Stripe

#### 3. JavaScript côté client (register-checkout.js)
```javascript
// Gestion du modal d'inscription/commande
- openBookingModal(serviceName, amount, currency)
- handleRegisterAndCheckout(event)
- createStripeSession(orderId)
- Validation en temps réel
- Gestion des états de chargement
```

### Sécurisations Mises en Place

#### 1. Authentification Obligatoire
```java
// Tous les endpoints de commande nécessitent une authentification
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
@PostMapping("/create-temp")
```

#### 2. Contrôle d'Accès aux Commandes
```java
// Vérification que l'utilisateur est propriétaire de la commande ou admin
boolean isOwner = order.getUser().getId().equals(authenticatedUser.getId());
boolean isAdmin = authenticatedUser.getRoles().stream()
    .anyMatch(role -> "ADMIN".equals(role.getName()));
```

#### 3. Configuration de Sécurité Mise à Jour
```java
// SecurityConfig.java
- Retrait de /api/orders/create-temp des endpoints publics
- Ajout de /register-and-checkout aux pages publiques
- Protection de tous les endpoints /api/orders/**
```

## Utilisation

### Pour les Développeurs

#### 1. Ajouter un bouton de commande
```html
<button onclick="openServiceModal('Nom du Service', 250.00)">
    Commander ce service
</button>
```

#### 2. Services prédéfinis disponibles
```javascript
// Fonctions d'aide pour les services courants
commanderDevelopmentWeb()     // 2500.00 CAD
commanderConseilTechnique()   // 150.00 CAD
commanderMaintenanceSite()    // 500.00 CAD
commanderOptimisationSEO()    // 800.00 CAD
commanderAuditSécurité()      // 1200.00 CAD
```

#### 3. Inclusion des scripts dans les templates
```html
<!-- Dans le template Thymeleaf -->
<div th:replace="fragments/booking-modal :: booking-modal"></div>
<div th:replace="fragments/scripts :: navigation-scripts"></div>
<div th:replace="fragments/scripts :: booking-helpers"></div>
```

### Pour les Utilisateurs

1. **Première commande :** Inscription complète requise
2. **Commandes suivantes :** Connexion simple au compte existant
3. **Sécurité :** Toutes les données sont protégées et chiffrées
4. **Traçabilité :** Accès à l'historique des commandes dans le dashboard

## Flux des Données

```
Visiteur → Modal d'inscription → Validation → Création compte
    ↓
Connexion automatique → Création commande → Session Stripe
    ↓
Paiement Stripe → Retour application → Dashboard utilisateur
```

## Sécurité et Conformité

### Mesures de Sécurité Implémentées

1. **Chiffrement des mots de passe** : BCrypt avec salt automatique
2. **Validation côté serveur** : Validation complète de tous les champs
3. **Protection CSRF** : Tokens CSRF pour les formulaires sensibles
4. **Authentification forcée** : Impossible d'acheter sans compte
5. **Audit trail** : Logging de toutes les actions sensibles

### Données Collectées

- **Informations personnelles** : Prénom, nom, email, téléphone
- **Adresse de facturation** : Adresse complète pour les factures
- **Informations de paiement** : Gérées entièrement par Stripe (PCI DSS)
- **Historique des commandes** : Accessible dans le dashboard utilisateur

## Tests et Validation

### Tests Recommandés

1. **Test d'inscription complète**
   - Vérifier tous les champs obligatoires
   - Tester la validation des mots de passe
   - Confirmer la création du compte

2. **Test de sécurité**
   - Tentative d'accès non autorisé aux commandes
   - Vérification de l'authentification forcée
   - Test des validations côté serveur

3. **Test du flux de paiement**
   - Inscription → Connexion → Commande → Stripe → Retour
   - Vérification des données de session
   - Test des différents modes de paiement Stripe

### Commandes de Test

```bash
# Test de l'endpoint d'inscription
curl -X POST http://localhost:8080/register-and-checkout \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "test@example.com",
    "password": "testpass123",
    "confirmPassword": "testpass123",
    "phone": "+1234567890",
    "address": "123 Test Street",
    "city": "Montreal",
    "postalCode": "H1A 1A1",
    "country": "Canada",
    "serviceName": "Test Service",
    "amount": 100.00,
    "currency": "CAD",
    "acceptTerms": true
  }'

# Test d'accès à une commande (nécessite authentification)
curl -X GET http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer <token>"
```

## Maintenance et Support

### Surveillance

- **Logs d'audit** : Toutes les actions d'inscription et de commande sont loggées
- **Métriques** : Suivi des taux de conversion et d'abandon
- **Alertes** : Notification en cas d'erreurs de paiement ou d'inscription

### Support Utilisateur

- **Dashboard** : Accès complet à l'historique des commandes
- **Profil** : Modification des informations personnelles
- **Support** : Contact direct depuis le dashboard

## Migration et Rétrocompatibilité

### Données Existantes

- **Utilisateurs temporaires** : Seront nettoyés automatiquement
- **Commandes orphelines** : Liaison manuelle si nécessaire
- **Sessions actives** : Aucun impact sur les utilisateurs connectés

### Dépréciation

- L'ancien endpoint `/api/orders/register-and-checkout` retourne HTTP 301
- Redirection automatique vers le nouveau système
- Messages d'avertissement dans les logs

## Conclusion

Le nouveau système d'authentification et de checkout offre :

- ✅ **Sécurité renforcée** : Élimination des vulnérabilités identifiées
- ✅ **Expérience utilisateur fluide** : Inscription et achat en une seule étape
- ✅ **Conformité** : Respect des bonnes pratiques de sécurité
- ✅ **Traçabilité** : Suivi complet des commandes et des utilisateurs
- ✅ **Scalabilité** : Architecture prête pour la croissance

Ce système garantit que tous les achats sont effectués par des utilisateurs authentifiés, tout en maintenant une expérience utilisateur optimale.