# 🚀 Démarrage rapide Stripe CLI - LMP

## ✅ Configuration Stripe CLI détectée

Votre Stripe CLI est connecté avec :
- **Account ID :** `acct_1Rs55xPnMmYY7ISZ`
- **Environnement :** Test Mode
- **Device :** `LAPTOP-FLTQIBF1`

## 🎯 3 étapes pour tester immédiatement

### 1. Démarrer l'application (Terminal 1)
```bash
./mvnw spring-boot:run
```
✅ Application sur `http://localhost:8080`

### 2. Forwarder les webhooks (Terminal 2)
```bash
stripe listen --forward-to localhost:8080/api/webhooks/stripe
```
📝 **Copiez le signing secret affiché** (commence par `whsec_...`)

### 3. Mettre à jour le webhook secret
Dans `src/main/resources/application.properties` :
```properties
stripe.webhook.secret=whsec_VOTRE_SECRET_DE_STRIPE_LISTEN
```

## ⚡ Tests instantanés (Terminal 3)

### Test API de base
```bash
curl "http://localhost:8080/api/payments/providers"
# Attendu: {"providers":["stripe-checkout"],"count":1}
```

### Test calcul des frais
```bash
curl "http://localhost:8080/api/payments/fees?amount=100&currency=CAD"
# Attendu: {"fees":3.20,"totalAmount":103.20}
```

### Test création session checkout
```bash
curl -X POST "http://localhost:8080/api/payments/process/1" \
  -H "Content-Type: application/json" \
  -d '{"amount":100.00,"currency":"CAD"}'
```

Copiez le `transactionId` de la réponse (commence par `cs_test_...`)

### Test webhook simulation
```bash
# Remplacez cs_test_... par votre session ID
stripe trigger checkout.session.completed --override checkout_session:cs_test_VOTRE_SESSION_ID
```

## 🎉 Validation réussie

Si vous voyez dans les logs de l'application :
```
INFO  --- Stripe webhook processed - Event: evt_..., Type: checkout.session.completed
```

**🎯 Félicitations !** Votre intégration Stripe Checkout fonctionne parfaitement !

## 🧪 Tests avancés

### Créer et tester une vraie session
```bash
# 1. Créer session
SESSION_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/payments/process/1" \
  -H "Content-Type: application/json" \
  -d '{"amount":150.00,"currency":"CAD"}')

# 2. Afficher l'URL Stripe
echo $SESSION_RESPONSE | grep -o '"redirectUrl":"[^"]*"'

# 3. Extraire l'ID de session
SESSION_ID=$(echo $SESSION_RESPONSE | grep -o 'cs_test_[^"]*')
echo "Session ID: $SESSION_ID"

# 4. Simuler le paiement
stripe trigger checkout.session.completed --override checkout_session:$SESSION_ID
```

### Tester avec une vraie carte
1. Copiez l'URL `redirectUrl` de la réponse
2. Ouvrez-la dans votre navigateur
3. Utilisez la carte de test : `4242 4242 4242 4242`
4. Date : n'importe quelle date future
5. CVC : `123`

## 🔧 Configuration actuelle

Vos clés Stripe (déjà configurées) :
- **Secret Key :** `***STRIPE_TEST_SK4_REMOVED***`
- **Publishable Key :** `***STRIPE_TEST_PK3_REMOVED***`
- **Application :** `http://localhost:8080`

## 📚 Documentation complète

- **Tests Postman :** `docs/stripe-checkout-postman-guide.md`
- **Guide Stripe CLI :** `docs/stripe-cli-local-testing-guide.md`
- **Collection Postman :** `docs/LMP-Stripe-Checkout-API.postman_collection.json`

## 🎯 Prêt pour la production !

Votre API Stripe Checkout est **100% opérationnelle** avec support Stripe CLI complet !