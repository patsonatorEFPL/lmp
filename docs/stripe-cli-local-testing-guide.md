# Guide de test local avec Stripe CLI

## 🚀 Configuration Stripe CLI

### 1. Connexion à votre compte Stripe

```bash
# Connecter Stripe CLI à votre compte
stripe login

# Vérifier la connexion
stripe config --list
```

### 2. Écouter les webhooks en local

```bash
# Forwarder les webhooks vers votre application locale
stripe listen --forward-to localhost:8081/api/webhooks/stripe
```

Cette commande va :
- Créer un endpoint webhook temporaire sur Stripe
- Forwarder tous les événements vers votre application
- Afficher le **signing secret** nécessaire

### 3. Récupérer le signing secret

La commande `stripe listen` affichera quelque chose comme :
```
> Ready! Your webhook signing secret is whsec_1234567890abcdef...
```

**Copiez ce secret** et mettez-le dans votre `application.properties` :

```properties
stripe.webhook.secret=whsec_1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef
```

## 🧪 Tests complets avec Stripe CLI

### Test 1: Créer une session de checkout

```bash
# Avec curl
curl -X POST "http://localhost:8081/api/payments/process/1" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.00,
    "currency": "CAD",
    "paymentProvider": "stripe",
    "paymentMethod": "checkout_session"
  }'
```

Récupérez l'ID de session depuis la réponse (ex: `cs_test_...`)

### Test 2: Simuler un paiement réussi

```bash
# Simuler l'événement checkout.session.completed
stripe trigger checkout.session.completed --override checkout_session:cs_test_VOTRE_SESSION_ID
```

### Test 3: Vérifier dans les logs

Votre application devrait afficher :
```
INFO  --- Stripe webhook processed - Event: evt_..., Type: checkout.session.completed
```

## 🔄 Workflow de test complet

### Terminal 1: Démarrer l'application
```bash
./mvnw spring-boot:run
```

### Terminal 2: Écouter les webhooks
```bash
stripe listen --forward-to localhost:8081/api/webhooks/stripe
```

### Terminal 3: Tester les API
```bash
# 1. Vérifier les fournisseurs
curl "http://localhost:8081/api/payments/providers"

# 2. Calculer les frais
curl "http://localhost:8081/api/payments/fees?amount=100&currency=CAD"

# 3. Créer une session
curl -X POST "http://localhost:8081/api/payments/process/1" \
  -H "Content-Type: application/json" \
  -d '{"amount":100.00,"currency":"CAD","paymentProvider":"stripe","paymentMethod":"checkout_session"}'

# 4. Simuler un paiement (remplacez SESSION_ID)
stripe trigger checkout.session.completed --override checkout_session:SESSION_ID
```

## 📋 Événements Stripe à tester

### Événements de checkout
```bash
# Checkout session créée
stripe trigger checkout.session.async_payment_succeeded

# Checkout session complétée
stripe trigger checkout.session.completed

# Checkout session expirée
stripe trigger checkout.session.expired
```

### Événements de paiement
```bash
# Paiement réussi
stripe trigger payment_intent.succeeded

# Paiement échoué
stripe trigger payment_intent.payment_failed

# Remboursement créé
stripe trigger charge.dispute.created
```

## 🛠️ Outils de debugging

### 1. Voir les événements en temps réel
```bash
# Dans un terminal séparé
stripe logs tail
```

### 2. Lister les événements récents
```bash
stripe events list --limit 10
```

### 3. Détails d'un événement spécifique
```bash
stripe events retrieve evt_VOTRE_EVENT_ID
```

## 🎯 Tests de validation

### Checklist de validation complète

- [ ] ✅ Application démarre sur port 8081
- [ ] ✅ Stripe CLI connecté et écoute les webhooks
- [ ] ✅ Signing secret configuré dans application.properties
- [ ] ✅ POST `/api/payments/process/1` crée une session checkout
- [ ] ✅ Session retourne une URL Stripe valide
- [ ] ✅ `stripe trigger checkout.session.completed` déclenche le webhook
- [ ] ✅ Application traite le webhook sans erreur
- [ ] ✅ Logs montrent le traitement réussi de l'événement

### Tests d'erreur à valider

```bash
# Webhook avec signature invalide
curl -X POST "http://localhost:8081/api/webhooks/stripe" \
  -H "Content-Type: application/json" \
  -H "Stripe-Signature: t=1629794400,v1=invalid_signature" \
  -d '{"type":"test"}'

# Montant invalide
curl -X POST "http://localhost:8081/api/payments/process/1" \
  -H "Content-Type: application/json" \
  -d '{"amount":-50,"currency":"CAD"}'

# Devise non supportée
curl -X POST "http://localhost:8081/api/payments/process/1" \
  -H "Content-Type: application/json" \
  -d '{"amount":100,"currency":"XYZ"}'
```

## 🚨 Troubleshooting

### Problème: Webhook non reçu
```bash
# Vérifier que Stripe CLI écoute
stripe listen --print-secret

# Vérifier les logs Stripe CLI
# Rechercher des erreurs de connexion
```

### Problème: Signature webhook invalide
```bash
# Récupérer le nouveau signing secret
stripe listen --print-secret

# Mettre à jour application.properties
stripe.webhook.secret=whsec_NOUVEAU_SECRET
```

### Problème: Session checkout invalide
```bash
# Lister les sessions récentes
stripe checkout sessions list --limit 5

# Voir les détails d'une session
stripe checkout sessions retrieve cs_test_VOTRE_SESSION_ID
```

## 📱 Tester la redirection complète

### 1. Créer une session et visiter l'URL

```bash
# 1. Créer la session
RESPONSE=$(curl -s -X POST "http://localhost:8081/api/payments/process/1" \
  -H "Content-Type: application/json" \
  -d '{"amount":100.00,"currency":"CAD"}')

# 2. Extraire l'URL de redirection
echo $RESPONSE | jq -r '.redirectUrl'

# 3. Ouvrir l'URL dans le navigateur (copier-coller)
```

### 2. Tester avec une vraie carte de test

Sur la page Stripe Checkout, utiliser :
- **Carte réussie :** `4242 4242 4242 4242`
- **Date :** N'importe quelle date future
- **CVC :** N'importe quel 3 chiffres
- **Code postal :** N'importe quel code

### 3. Vérifier la redirection

Après paiement, vous devriez être redirigé vers :
```
http://localhost:8081/stripe/checkout/success?order_id=1&session_id=cs_test_...&type=success
```

## 🎉 Test complet réussi !

Si tous ces tests passent, votre intégration Stripe Checkout est **100% fonctionnelle** :

✅ **Backend :** Sessions créées correctement  
✅ **Webhooks :** Événements traités en temps réel  
✅ **Frontend :** Redirections fonctionnelles  
✅ **Sécurité :** Signatures webhooks validées  
✅ **Robustesse :** Gestion d'erreurs opérationnelle  

Votre application est prête pour la production !