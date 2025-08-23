# Guide de test Postman - Stripe Checkout API

Ce guide détaille comment tester l'API de paiement Stripe Checkout de l'application LMP avec Postman.

## Configuration de l'environnement Postman

### Variables d'environnement

Créez un environnement Postman avec les variables suivantes :

```json
{
  "base_url": "http://localhost:8080",
  "api_path": "/api",
  "stripe_webhook_secret": "whsec_1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
  "test_order_id": "1",
  "test_transaction_id": "cs_test_123456789",
  "auth_token": "your_jwt_token_here"
}
```

### Headers globaux recommandés

```json
{
  "Content-Type": "application/json",
  "Accept": "application/json",
  "Authorization": "Bearer {{auth_token}}"
}
```

## Tests des endpoints principaux

### 1. Calcul des frais de traitement

**GET** `{{base_url}}{{api_path}}/payments/fees`

#### Paramètres de requête (Query Parameters)
- `amount` (required): `100.00`
- `currency` (required): `CAD`
- `provider` (optional): `stripe` (valeur par défaut)

#### Exemple de requête
```
GET http://localhost:8080/api/payments/fees?amount=100.00&currency=CAD
```

#### Réponse attendue (200 OK)
```json
{
  "amount": 100.00,
  "currency": "CAD",
  "provider": "stripe",
  "paymentMethod": "checkout_session",
  "fees": 3.20,
  "totalAmount": 103.20,
  "note": "Frais calculés pour Stripe Checkout uniquement"
}
```

#### Tests Postman suggérés
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response contains fees calculation", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('fees');
    pm.expect(jsonData).to.have.property('totalAmount');
    pm.expect(jsonData.provider).to.eql('stripe');
});

pm.test("Fees are calculated correctly", function () {
    const jsonData = pm.response.json();
    const expectedFees = (100.00 * 0.029) + 0.30; // 2.9% + 30¢
    pm.expect(jsonData.fees).to.be.closeTo(expectedFees, 0.01);
});
```

### 2. Initier un paiement Stripe Checkout

**POST** `{{base_url}}{{api_path}}/payments/process/{{test_order_id}}`

#### Headers
- `Authorization: Bearer {{auth_token}}`
- `Content-Type: application/json`

#### Body (JSON)
```json
{
  "amount": 150.00,
  "currency": "CAD",
  "paymentProvider": "stripe",
  "paymentMethod": "checkout_session",
  "metadata": {
    "customer_note": "Test payment via Postman",
    "source": "postman_test"
  }
}
```

#### Réponse attendue (200 OK)
```json
{
  "success": true,
  "transactionId": "cs_test_a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8s9T0",
  "paymentId": "cs_test_a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8s9T0",
  "amount": 150.00,
  "currency": "CAD",
  "status": "PENDING",
  "paymentMethod": "checkout_session",
  "paymentProvider": "stripe-checkout",
  "processingFees": 4.65,
  "message": "Session Stripe Checkout créée avec succès",
  "redirectUrl": "https://checkout.stripe.com/c/pay/cs_test_a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8s9T0",
  "requiresRedirect": true,
  "providerResponse": {
    "checkout_session_id": "cs_test_a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8s9T0",
    "checkout_url": "https://checkout.stripe.com/c/pay/cs_test_a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8s9T0",
    "expires_at": "2025-08-18T14:30:00Z"
  }
}
```

#### Tests Postman suggérés
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Payment session created successfully", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.success).to.be.true;
    pm.expect(jsonData).to.have.property('transactionId');
    pm.expect(jsonData).to.have.property('redirectUrl');
    pm.expect(jsonData.requiresRedirect).to.be.true;
    
    // Sauvegarder l'ID de transaction pour les tests suivants
    pm.environment.set("checkout_session_id", jsonData.transactionId);
});

pm.test("Redirect URL is valid Stripe Checkout URL", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.redirectUrl).to.include('checkout.stripe.com');
});
```

### 3. Vérifier le statut d'une transaction

**GET** `{{base_url}}{{api_path}}/payments/transaction/{{checkout_session_id}}/status`

#### Headers
- `Authorization: Bearer {{auth_token}}`

#### Réponse attendue (200 OK)
```json
{
  "transactionId": "cs_test_a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8s9T0",
  "status": "open",
  "checkedAt": "2025-08-18T13:30:00"
}
```

#### Tests Postman suggérés
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Status response is valid", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('status');
    pm.expect(jsonData).to.have.property('checkedAt');
});
```

### 4. Récupérer les détails d'une transaction

**GET** `{{base_url}}{{api_path}}/payments/transaction/{{test_transaction_id}}`

#### Headers
- `Authorization: Bearer {{auth_token}}`

#### Réponse attendue (200 OK)
```json
{
  "id": 1,
  "orderId": 1,
  "amount": 150.00,
  "currency": "CAD",
  "status": "COMPLETED",
  "paymentMethod": "checkout_session",
  "paymentProvider": "stripe-checkout",
  "transactionId": "cs_test_a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8s9T0",
  "createdAt": "2025-08-18T13:25:00",
  "updatedAt": "2025-08-18T13:30:00"
}
```

### 5. Lister les fournisseurs supportés

**GET** `{{base_url}}{{api_path}}/payments/providers`

#### Réponse attendue (200 OK)
```json
{
  "providers": ["stripe-checkout"],
  "count": 1
}
```

#### Tests Postman suggérés
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Only Stripe Checkout is supported", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.providers).to.have.lengthOf(1);
    pm.expect(jsonData.providers[0]).to.eql('stripe-checkout');
});
```

### 6. Webhook Stripe

**POST** `{{base_url}}{{api_path}}/webhooks/stripe`

#### Headers
- `Content-Type: application/json`
- `Stripe-Signature: t=1629794400,v1=5f4f...` (généré par Stripe)

#### Body (JSON) - Exemple d'événement de succès de paiement
```json
{
  "id": "evt_1234567890",
  "object": "event",
  "api_version": "2023-10-16",
  "created": 1629794400,
  "data": {
    "object": {
      "id": "cs_test_a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8s9T0",
      "object": "checkout.session",
      "amount_total": 15000,
      "currency": "cad",
      "customer_email": "test@example.com",
      "payment_status": "paid",
      "payment_intent": "pi_1234567890",
      "metadata": {
        "order_id": "1"
      }
    }
  },
  "livemode": false,
  "pending_webhooks": 1,
  "request": {
    "id": "req_1234567890",
    "idempotency_key": null
  },
  "type": "checkout.session.completed"
}
```

#### Réponse attendue (200 OK)
```json
{
  "received": true,
  "eventId": "evt_1234567890",
  "eventType": "checkout.session.completed",
  "processed": true,
  "processedAt": "2025-08-18T13:30:00"
}
```

## Scénarios de test complets

### Scénario 1: Flux de paiement réussi

1. **Calculer les frais** → `GET /api/payments/fees`
2. **Initier le paiement** → `POST /api/payments/process/{orderId}`
3. **Vérifier le statut initial** → `GET /api/payments/transaction/{transactionId}/status`
4. **Simuler le webhook de succès** → `POST /api/webhooks/stripe`
5. **Vérifier le statut final** → `GET /api/payments/transaction/{transactionId}`

### Scénario 2: Test des erreurs

#### Montant invalide
```json
{
  "amount": -50.00,
  "currency": "CAD"
}
```

#### Devise non supportée
```json
{
  "amount": 100.00,
  "currency": "XYZ"
}
```

#### Commande inexistante
```
POST /api/payments/process/999999
```

## Collection Postman prête à l'emploi

### Format d'exportation

Voici la structure JSON pour importer la collection complète dans Postman :

```json
{
  "info": {
    "name": "LMP Stripe Checkout API",
    "description": "Collection complète pour tester l'API de paiement Stripe Checkout",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:8080"
    },
    {
      "key": "api_path",
      "value": "/api"
    }
  ],
  "item": [
    {
      "name": "Payments",
      "item": [
        {
          "name": "Calculate Fees",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{base_url}}{{api_path}}/payments/fees?amount=100.00&currency=CAD",
              "host": ["{{base_url}}"],
              "path": ["{{api_path}}", "payments", "fees"],
              "query": [
                {"key": "amount", "value": "100.00"},
                {"key": "currency", "value": "CAD"}
              ]
            }
          }
        },
        {
          "name": "Process Payment",
          "request": {
            "method": "POST",
            "header": [
              {"key": "Content-Type", "value": "application/json"}
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"amount\": 150.00,\n  \"currency\": \"CAD\",\n  \"paymentProvider\": \"stripe\",\n  \"paymentMethod\": \"checkout_session\"\n}"
            },
            "url": {
              "raw": "{{base_url}}{{api_path}}/payments/process/{{test_order_id}}",
              "host": ["{{base_url}}"],
              "path": ["{{api_path}}", "payments", "process", "{{test_order_id}}"]
            }
          }
        }
      ]
    },
    {
      "name": "Webhooks",
      "item": [
        {
          "name": "Stripe Webhook",
          "request": {
            "method": "POST",
            "header": [
              {"key": "Content-Type", "value": "application/json"},
              {"key": "Stripe-Signature", "value": "t=1629794400,v1=test_signature"}
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"id\": \"evt_test_webhook\",\n  \"object\": \"event\",\n  \"type\": \"checkout.session.completed\"\n}"
            },
            "url": {
              "raw": "{{base_url}}{{api_path}}/webhooks/stripe",
              "host": ["{{base_url}}"],
              "path": ["{{api_path}}", "webhooks", "stripe"]
            }
          }
        }
      ]
    }
  ]
}
```

## Instructions d'utilisation

1. **Importer la collection** dans Postman
2. **Configurer l'environnement** avec vos variables
3. **Démarrer l'application** Spring Boot (`mvn spring-boot:run`)
4. **Exécuter les tests** dans l'ordre des scénarios
5. **Vérifier les logs** de l'application pour le debugging

## Notes importantes

- Les clés Stripe utilisées sont des clés de test
- Les webhooks nécessitent une signature valide en production
- Les montants sont en cents dans l'API Stripe (multipliés par 100)
- Les sessions Stripe Checkout expirent après 1 heure

## Débogage courant

### Erreurs fréquentes

1. **Port 8080 déjà utilisé** → Changer le port ou arrêter l'autre processus
2. **Clé Stripe invalide** → Vérifier `application.properties`
3. **Base de données** → S'assurer que MySQL est démarré
4. **Authentification** → Vérifier le token JWT

### Logs utiles

```bash
# Démarrer avec logs détaillés
mvn spring-boot:run -Dlogging.level.com.lmp=DEBUG

# Voir les requêtes SQL
mvn spring-boot:run -Dlogging.level.org.hibernate.SQL=DEBUG
```

Cette documentation vous permet de tester complètement l'API Stripe Checkout de manière systématique et reproductible.