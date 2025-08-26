# Guide Postman - Création Session Stripe Checkout

## 🎯 Endpoints Disponibles Après Correction

### 1. Tester l'endpoint /fees (CORRIGÉ)
**GET** `http://localhost:8080/api/payments/fees`

**Paramètres Query :**
- `amount`: 100
- `currency`: CAD
- `provider`: stripe (optionnel, défaut = "stripe")

**Réponse Attendue :**
```json
{
    "amount": 100,
    "currency": "CAD", 
    "provider": "stripe",
    "paymentMethod": "checkout_session",
    "fees": 3.20,
    "totalAmount": 103.20,
    "note": "Frais calculés pour Stripe Checkout uniquement"
}
```

### 2. Lister les providers disponibles
**GET** `http://localhost:8080/api/payments/providers`

**Réponse Attendue :**
```json
{
    "providers": ["stripe"],
    "count": 1
}
```

### 3. Créer une session Stripe Checkout
**POST** `http://localhost:8080/stripe/checkout/create-session/{orderId}`

**Remplacer {orderId} par 1 par exemple :**
`http://localhost:8080/stripe/checkout/create-session/1`

**Headers :**
- `Content-Type`: application/json

**Body (JSON) - OPTIONNEL :**
```json
{}
```

**Réponse Attendue (Succès) :**
```json
{
    "success": true,
    "redirectUrl": "https://checkout.stripe.com/c/pay/cs_test_...",
    "sessionId": "cs_test_..."
}
```

**Réponse Attendue (Erreur si commande inexistante) :**
```json
{
    "error": "INVALID_ORDER_STATUS",
    "message": "Cette commande ne peut pas être payée dans son état actuel"
}
```

## 🚨 Prérequis Importants

### 1. L'application doit être démarrée
```bash
mvn spring-boot:run
```

### 2. Une commande doit exister avec ID=1
Si pas de commande, l'endpoint retournera 404.

### 3. Configuration Stripe
Vérifier que les clés Stripe sont configurées dans `application.properties`.

## 🛠️ Correction Appliquée

**Problème Résolu :** L'erreur `CALCULATION_ERROR` était causée par un mauvais nom de bean.
- **Avant :** `@Component("stripeCheckoutPaymentProcessor")`  
- **Après :** `@Component("stripePaymentProcessor")`

Le [`PaymentServiceImpl`](src/main/java/com/lmp/service/payment/PaymentServiceImpl.java) cherche un bean nommé `provider + "PaymentProcessor"`, soit `"stripePaymentProcessor"`.

## 📋 Tests à Effectuer avec Postman

### Test 1: Endpoint /fees
1. **GET** `http://localhost:8080/api/payments/fees?amount=100&currency=CAD`
2. **Vérifier :** Status 200 et calcul des frais correct

### Test 2: Endpoint /providers  
1. **GET** `http://localhost:8080/api/payments/providers`
2. **Vérifier :** Status 200 et provider "stripe" présent

### Test 3: Création de session
1. **POST** `http://localhost:8080/stripe/checkout/create-session/1`
2. **Vérifier :** Status 200 ou 404 (selon existence commande)

## 🔧 Dépannage

### Si erreur 404 sur create-session
- Vérifier qu'une commande existe avec l'ID utilisé
- Ou utiliser l'endpoint de création de commande d'abord

### Si erreur CALCULATION_ERROR persiste
- Redémarrer l'application après la correction du bean
- Vérifier les logs Spring Boot au démarrage

### Si erreur d'authentification ou 403 Forbidden
- Les endpoints `/api/payments/**` sont publics (pas d'auth requise)
- L'endpoint `/stripe/checkout/**` est public aussi
- **CORRIGÉ :** Protection CSRF désactivée pour `/stripe/**`

## 🔧 Corrections Appliquées

### 1. Correction Bean PaymentProcessor
- **Fichier :** `StripeCheckoutPaymentProcessor.java`
- **Change :** `@Component("stripePaymentProcessor")` au lieu de `"stripeCheckoutPaymentProcessor"`

### 2. Correction CSRF Protection
- **Fichier :** `SecurityConfig.java`
- **Change :** `.ignoringRequestMatchers("/webhook/**", "/api/**", "/stripe/**")`
- **Résultat :** Endpoints POST Stripe Checkout fonctionnels

## ⚠️ Important
**Redémarrer l'application** après ces corrections pour que les changements prennent effet :
```bash
mvn spring-boot:run
```