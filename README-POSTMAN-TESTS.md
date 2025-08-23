# 🚀 Tests Postman - Stripe Checkout API

## Démarrage rapide (5 minutes)

### 1. Importer la collection

1. Ouvrir Postman
2. Cliquer sur **Import**
3. Glisser-déposer le fichier `docs/LMP-Stripe-Checkout-API.postman_collection.json`

### 2. Configurer l'environnement

Créer un nouvel environnement avec ces variables :

| Variable | Valeur |
|----------|--------|
| `base_url` | `http://localhost:8081` |
| `api_path` | `/api` |
| `test_order_id` | `1` |

### 3. Tests essentiels à exécuter

#### ✅ Test 1: Fournisseurs supportés
```
GET {{base_url}}{{api_path}}/payments/providers
```
**Résultat attendu:** `["stripe-checkout"]`

#### ✅ Test 2: Calcul des frais
```
GET {{base_url}}{{api_path}}/payments/fees?amount=100.00&currency=CAD
```
**Résultat attendu:** Frais = 3.20$ (2.9% + 0.30$)

#### ✅ Test 3: Créer une session de paiement
```
POST {{base_url}}{{api_path}}/payments/process/1
Content-Type: application/json

{
  "amount": 150.00,
  "currency": "CAD",
  "paymentProvider": "stripe",
  "paymentMethod": "checkout_session"
}
```
**Résultat attendu:** URL de redirection Stripe Checkout

#### ✅ Test 4: Health check webhook
```
GET {{base_url}}{{api_path}}/webhooks/health
```
**Résultat attendu:** `{"status": "healthy"}`

## 🎯 Tests rapides en une ligne

### Via curl (Terminal)

```bash
# Test des fournisseurs
curl "http://localhost:8081/api/payments/providers"

# Test calcul des frais
curl "http://localhost:8081/api/payments/fees?amount=100&currency=CAD"

# Test création de paiement
curl -X POST "http://localhost:8081/api/payments/process/1" \
  -H "Content-Type: application/json" \
  -d '{"amount":150.00,"currency":"CAD","paymentProvider":"stripe","paymentMethod":"checkout_session"}'
```

## 📋 Checklist de validation

- [ ] L'application répond sur le port 8081
- [ ] `/api/payments/providers` retourne uniquement "stripe-checkout"
- [ ] `/api/payments/fees` calcule correctement les frais Stripe
- [ ] `/api/payments/process/{orderId}` crée une session et retourne une URL Stripe
- [ ] Les URLs de redirection utilisent le bon domaine
- [ ] Les webhooks répondent correctement

## 🔧 Dépannage express

### Port occupé
```bash
# Changer le port dans application.properties
server.port=8082
app.base.url=http://localhost:8082
```

### Base de données
```bash
# Vérifier MySQL
mysql -u root -p -e "SHOW DATABASES;"
```

### Logs détaillés
```bash
# Démarrer avec plus de logs
./mvnw spring-boot:run -Dlogging.level.com.lmp=DEBUG
```

## 🎉 C'est prêt !

Si tous les tests passent, votre API Stripe Checkout est **100% fonctionnelle** et prête pour l'intégration frontend !

**🔗 URLs importantes:**
- API Base: `http://localhost:8081/api`
- Health: `http://localhost:8081/api/webhooks/health`
- Docs: `docs/stripe-checkout-postman-guide.md`