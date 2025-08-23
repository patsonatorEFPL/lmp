# 🚀 Setup rapide pour tester avec Stripe CLI

## Étapes pour commencer immédiatement

### 1. Terminal 1 - Démarrer l'application
```bash
./mvnw spring-boot:run
```
✅ Application disponible sur `http://localhost:8080`

### 2. Terminal 2 - Stripe CLI Webhook Forwarding
```bash
stripe listen --forward-to localhost:8080/api/webhooks/stripe
```
📝 **Important :** Copiez le `webhook signing secret` affiché et mettez-le dans `application.properties`

### 3. Mettre à jour le webhook secret
Dans `src/main/resources/application.properties` :
```properties
stripe.webhook.secret=whsec_VOTRE_SECRET_ICI
```

### 4. Terminal 3 - Tests rapides
```bash
# Test 1: Vérifier l'API
curl "http://localhost:8080/api/payments/providers"

# Test 2: Calculer les frais
curl "http://localhost:8080/api/payments/fees?amount=100&currency=CAD"

# Test 3: Créer une session checkout
curl -X POST "http://localhost:8080/api/payments/process/1" \
  -H "Content-Type: application/json" \
  -d '{"amount":100.00,"currency":"CAD","paymentProvider":"stripe","paymentMethod":"checkout_session"}'
```

### 5. Test du webhook (après avoir créé une session)
```bash
# Remplacez SESSION_ID par l'ID de votre session
stripe trigger checkout.session.completed --override checkout_session:SESSION_ID
```

## ✅ Checklist de validation

- [ ] Application Spring Boot démarrée (port 8080)
- [ ] Stripe CLI connecté et forwarding actif
- [ ] Webhook secret configuré dans application.properties
- [ ] Test API providers retourne `["stripe-checkout"]`
- [ ] Test calcul frais retourne ~3.20$ pour 100$ CAD
- [ ] Création session retourne URL Stripe checkout
- [ ] Webhook trigger déclenche les logs dans l'application

## 🎯 Si tout fonctionne

Votre API Stripe Checkout est **prête** ! Vous pouvez maintenant :
- Intégrer le frontend
- Tester avec de vraies cartes de test
- Déployer en production

## 📚 Documentation complète

- **Guide Postman :** `docs/stripe-checkout-postman-guide.md`
- **Guide Stripe CLI :** `docs/stripe-cli-local-testing-guide.md`
- **Résumé technique :** `docs/stripe-checkout-implementation-summary.md`
- **Tests rapides :** `README-POSTMAN-TESTS.md`