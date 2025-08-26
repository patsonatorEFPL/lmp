# Guide Reconnexion Stripe CLI

## 🔑 Reconnexion à l'API Stripe

### 1. Vérifier le statut actuel
```bash
stripe auth
```

### 2. Se reconnecter (nouvelle session)
```bash
stripe login
```
Cette commande ouvrira votre navigateur pour vous authentifier avec votre compte Stripe.

### 3. Vérifier la connexion
```bash
stripe config --list
```

## 🎯 Configuration Actuelle dans application.properties

Voici les clés configurées dans votre projet :

```properties
# Clés API Stripe
stripe.api.secret-key=***STRIPE_TEST_SK5_REMOVED***
stripe.api.publishable-key=***STRIPE_TEST_PK4_REMOVED***

# Configuration Webhook (pour CLI local)
stripe.webhook.secret=***STRIPE_LIVE_WEBHOOK_SECRET_REMOVED***
```

## 🚀 Relancer l'écoute des webhooks

### 1. Après reconnexion, relancer les webhooks
```bash
stripe listen --forward-to localhost:8080/api/webhooks/stripe
```

### 2. Si nouveau webhook secret généré
Si la commande `stripe listen` génère un nouveau secret (whsec_...), mettre à jour dans `application.properties`.

## 🔄 Workflow Complet Post-Reconnexion

### 1. Reconnexion Stripe CLI
```bash
stripe login
stripe config --list
```

### 2. Démarrer l'écoute webhooks
```bash
stripe listen --forward-to localhost:8080/api/webhooks/stripe
```

### 3. Démarrer l'application Spring Boot
```bash
mvn spring-boot:run
```

### 4. Tester les endpoints
```bash
# Test calcul frais
curl "http://localhost:8080/api/payments/fees?amount=100&currency=CAD"

# Test providers
curl "http://localhost:8080/api/payments/providers"

# Test création session (avec Postman)
POST http://localhost:8080/stripe/checkout/create-session/1
```

## ⚡ Commandes Rapides

### Vérifier la connexion Stripe
```bash
stripe customers list --limit 1
```

### Voir les clés configurées
```bash
stripe config --list
```

### Tester un webhook manuel
```bash
stripe trigger payment_intent.succeeded
```

## 🛠️ Troubleshooting

### Si erreur d'authentification
1. Vérifier la connexion : `stripe auth`
2. Se reconnecter : `stripe login`
3. Redémarrer l'écoute : `stripe listen --forward-to localhost:8080/api/webhooks/stripe`

### Si nouveau webhook secret
1. Copier le nouveau `whsec_...` de la commande `stripe listen`
2. Mettre à jour dans `application.properties`
3. Redémarrer l'application Spring Boot

La configuration actuelle devrait fonctionner après reconnexion !