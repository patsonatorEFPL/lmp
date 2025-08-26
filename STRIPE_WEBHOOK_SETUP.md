# Configuration des Webhooks Stripe pour Production

## 🎯 Vue d'ensemble

Ce guide vous aide à configurer les webhooks Stripe pour l'application LMP en production sur Railway.com.

## 📋 Prérequis

- [ ] Application déployée sur Railway.com
- [ ] Clés Stripe de production (live keys)
- [ ] Accès au Stripe Dashboard
- [ ] URL de l'application Railway

## 🔧 Configuration dans Stripe Dashboard

### 1. Accéder aux Webhooks
1. Connectez-vous au [Stripe Dashboard](https://dashboard.stripe.com)
2. Assurez-vous d'être en mode **Live** (pas Test)
3. Allez dans **Developers** → **Webhooks**

### 2. Créer un Nouveau Webhook
1. Cliquez sur **Add endpoint**
2. **Endpoint URL**: `https://votre-app.railway.app/api/stripe/webhook`
3. **Description**: `LMP Production Webhook`

### 3. Sélectionner les Événements
Ajoutez ces événements essentiels :

#### 💳 Paiements
- `checkout.session.completed`
- `checkout.session.expired`
- `payment_intent.succeeded`
- `payment_intent.payment_failed`
- `payment_intent.canceled`

#### 📄 Factures
- `invoice.payment_succeeded`
- `invoice.payment_failed`
- `invoice.finalized`

#### 💰 Remboursements
- `charge.dispute.created`
- `refund.created`
- `refund.updated`

#### 🔄 Subscriptions (si applicable)
- `customer.subscription.created`
- `customer.subscription.updated`
- `customer.subscription.deleted`

### 4. Finaliser la Configuration
1. Cliquez sur **Add endpoint**
2. **Copiez le Webhook Secret** (commence par `whsec_`)
3. Ajoutez ce secret dans Railway comme `STRIPE_WEBHOOK_SECRET`

## 🧪 Test des Webhooks

### 1. Test Automatique Stripe
1. Dans Stripe Dashboard → Webhooks
2. Cliquez sur votre endpoint
3. Onglet **Test** → **Send test webhook**
4. Choisissez `checkout.session.completed`
5. Cliquez **Send test webhook**

### 2. Vérification dans les Logs
```bash
# Dans Railway Dashboard → Logs, recherchez :
INFO  com.lmp.service.payment.webhook.StripeWebhookHandler - Webhook reçu: checkout.session.completed
INFO  com.lmp.service.payment.webhook.StripeWebhookHandler - Paiement traité avec succès
```

### 3. Test avec Transaction Réelle
1. Créez une commande test depuis votre application
2. Utilisez une carte de test Stripe: `4242 4242 4242 4242`
3. Vérifiez que le webhook est déclenché

## 🔍 URLs de Test

### Endpoints de Webhook
```
Production: https://votre-app.railway.app/api/stripe/webhook
Health Check: https://votre-app.railway.app/actuator/health
```

### Pages de Test
```
Accueil: https://votre-app.railway.app/
Services: https://votre-app.railway.app/services
Admin: https://votre-app.railway.app/admin
```

## 🚨 Dépannage

### Webhook Non Reçu
1. **Vérifiez l'URL**: Doit être accessible publiquement
2. **Vérifiez les logs Railway**: Erreurs 500/404 ?
3. **Testez manuellement**: `curl -X POST https://votre-app.railway.app/api/stripe/webhook`

### Erreur de Signature
```
ERROR: Webhook signature verification failed
```
**Solution**: Vérifiez que `STRIPE_WEBHOOK_SECRET` est correctement configuré

### Timeout
```
ERROR: Webhook timeout
```
**Solution**: Le traitement prend trop de temps, optimisez le code

## 📊 Monitoring des Webhooks

### Dans Stripe Dashboard
1. **Developers** → **Webhooks** → Votre endpoint
2. Onglet **Attempts** pour voir l'historique
3. Statuts à surveiller :
   - ✅ **Succeeded**: Tout va bien
   - ⚠️ **Failed**: Nécessite attention
   - 🔄 **Retrying**: Stripe réessaie

### Métriques Importantes
- **Success Rate**: Doit être > 98%
- **Response Time**: Doit être < 5 secondes
- **Failed Attempts**: Doit être minimal

## 🔐 Sécurité

### Vérification de Signature
Le code LMP vérifie automatiquement :
```java
// Dans StripeWebhookHandler.java
String payload = // corps de la requête
String sigHeader = request.getHeader("Stripe-Signature");
Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
```

### Bonnes Pratiques
- [ ] Ne jamais exposer le webhook secret
- [ ] Utiliser HTTPS uniquement
- [ ] Valider tous les événements reçus
- [ ] Logger les tentatives suspectes

## 📝 Variables d'Environnement Railway

Assurez-vous que ces variables sont configurées :

```env
# Stripe Production
STRIPE_SECRET_KEY=sk_live_...
STRIPE_PUBLISHABLE_KEY=pk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Application
APP_BASE_URL=https://votre-app.railway.app
```

## 🧪 Tests de Régression

### Après Chaque Déploiement
1. [ ] Test webhook manuel depuis Stripe
2. [ ] Transaction test complète
3. [ ] Vérification logs sans erreur
4. [ ] Check admin dashboard fonctionne

### Tests Mensuels
1. [ ] Performance des webhooks
2. [ ] Taux de succès > 98%
3. [ ] Aucun webhook en échec
4. [ ] Mise à jour des clés si nécessaire

## 📞 Support

### En Cas de Problème
1. **Logs Railway**: Premier point de vérification
2. **Stripe Dashboard**: Historique des tentatives
3. **Support Stripe**: Pour les problèmes côté Stripe
4. **Documentation**: [stripe.com/docs/webhooks](https://stripe.com/docs/webhooks)

### Contacts Utiles
- Support Stripe: support@stripe.com
- Documentation Railway: docs.railway.app
- Support Railway: help@railway.app

---

**⚠️ IMPORTANT**: Testez toujours en mode Test avant de basculer en production !

*Guide créé pour l'application LMP Digital Services*