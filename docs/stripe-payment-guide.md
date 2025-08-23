# Guide des Paiements Stripe - LMP

## Vue d'ensemble

Le système LMP propose deux approches complémentaires pour l'intégration Stripe, optimisées pour différents cas d'usage :

### 🚀 **StripeCheckoutPaymentProcessor** (Recommandé par défaut)
- Interface utilisateur hébergée par Stripe
- Configuration simplifiée et sécurisée
- Conformité PCI DSS automatique
- Idéal pour : e-commerce standard, paiements B2C, intégrations rapides

### ⚙️ **StripePaymentProcessor** (Cas avancés)
- Contrôle granulaire via API PaymentIntent
- Authentification 3D Secure forcée
- Métadonnées étendues et audit détaillé
- Idéal pour : paiements B2B, transactions complexes, intégrations personnalisées

---

## Utilisation du Processeur Checkout (Recommandé)

### Configuration

```properties
# Configuration Stripe Checkout
stripe.checkout.success.url=${app.base.url}/stripe/checkout/success
stripe.checkout.cancel.url=${app.base.url}/stripe/checkout/cancel
stripe.checkout.mode=payment
```

### Implémentation Frontend

```javascript
// Créer une session de paiement
async function initiateCheckoutPayment(orderId) {
    try {
        const response = await fetch(`/stripe/checkout/create-session/${orderId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                successUrl: window.location.origin + '/payment-success',
                cancelUrl: window.location.origin + '/payment-cancel'
            })
        });
        
        const session = await response.json();
        
        if (session.redirectUrl) {
            // Rediriger vers Stripe Checkout
            window.location.href = session.redirectUrl;
        }
    } catch (error) {
        console.error('Erreur lors de la création de la session:', error);
    }
}
```

### Endpoints Disponibles

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/stripe/checkout/create-session/{orderId}` | POST | Créer une session Checkout |
| `/stripe/checkout/success` | GET | Page de succès post-paiement |
| `/stripe/checkout/cancel` | GET | Page d'annulation |

---

## Utilisation du Processeur Avancé

### Cas d'usage recommandés

```java
@Service
public class PaymentService {
    
    @Autowired
    private StripeProcessorSelector processorSelector;
    
    public PaymentResponseDto processPayment(PaymentRequestDto request) {
        // Sélection automatique du processeur approprié
        PaymentProcessor processor = processorSelector.selectProcessor(request);
        
        // Traitement du paiement
        return processor.processPayment(order, request);
    }
}
```

### Configuration pour cas avancés

```java
PaymentRequestDto advancedRequest = new PaymentRequestDto();
advancedRequest.setAmount(new BigDecimal("2500.00")); // Montant élevé
advancedRequest.setCurrency("CAD");
advancedRequest.setPaymentMethod("card");

// Métadonnées étendues pour audit B2B
Map<String, Object> metadata = new HashMap<>();
metadata.put("processor_type", "advanced");
metadata.put("business_unit", "enterprise");
metadata.put("contract_id", "CTR-2024-001");
metadata.put("approval_code", "APP-789");
advancedRequest.setMetadata(metadata);

// URL de retour personnalisée
advancedRequest.setReturnUrl("https://myapp.com/payment/callback");
```

---

## Sélection Automatique du Processeur

Le `StripeProcessorSelector` choisit automatiquement le processeur approprié selon ces critères :

### ➡️ **Processeur Avancé** utilisé si :
- Métadonnées > 3 entrées
- URL de retour personnalisée (hors `/checkout/`)
- Montant > 1000 CAD/USD
- `metadata.processor_type = "advanced"`

### ➡️ **Processeur Checkout** utilisé dans tous les autres cas

```java
// Exemple d'utilisation
@RestController
public class PaymentController {
    
    @Autowired
    private StripeProcessorSelector selector;
    
    @PostMapping("/payment/process")
    public ResponseEntity<PaymentResponseDto> processPayment(@RequestBody PaymentRequestDto request) {
        // Récupération des recommandations
        String recommendation = selector.getProcessorRecommendation(request);
        logger.info("Processeur recommandé: {}", recommendation);
        
        // Sélection et traitement
        PaymentProcessor processor = selector.selectProcessor(request);
        PaymentResponseDto response = processor.processPayment(order, request);
        
        return ResponseEntity.ok(response);
    }
}
```

---

## Gestion des Webhooks

Les deux processeurs partagent le même système de webhooks :

```java
@RestController
public class StripeWebhookController {
    
    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        // Gestion unifiée des événements Stripe
        return webhookHandler.processWebhook(payload);
    }
}
```

### Événements supportés

#### Stripe Checkout
- `checkout.session.completed` - Paiement réussi
- `checkout.session.expired` - Session expirée
- `checkout.session.async_payment_succeeded` - Paiement asynchrone réussi
- `checkout.session.async_payment_failed` - Paiement asynchrone échoué

#### PaymentIntent (Processeur avancé)
- `payment_intent.succeeded` - Paiement confirmé
- `payment_intent.payment_failed` - Paiement échoué
- `payment_intent.requires_action` - Action requise (3D Secure)

---

## Comparaison des Approches

| Critère | Checkout | Avancé |
|---------|----------|---------|
| **Complexité d'implémentation** | ⭐⭐ | ⭐⭐⭐⭐ |
| **Contrôle du processus** | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Conformité PCI DSS** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Personnalisation UI** | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Métadonnées et audit** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **3D Secure** | Automatique | Forcé |
| **Temps d'intégration** | 1-2 jours | 1-2 semaines |

---

## Bonnes Pratiques

### Pour le Processeur Checkout
✅ **À faire :**
- Utiliser les URLs de succès/échec configurées
- Gérer les sessions expirées
- Tester les redirections en production

❌ **À éviter :**
- Personnaliser excessivement l'interface
- Ignorer les webhooks de session

### Pour le Processeur Avancé
✅ **À faire :**
- Implémenter la gestion 3D Secure
- Logger toutes les métadonnées pour audit
- Tester les différents états de PaymentIntent

❌ **À éviter :**
- Utiliser pour des cas d'usage simples
- Négliger la sécurité des tokens

---

## Migration et Coexistence

Les deux processeurs peuvent coexister dans la même application :

```java
// Configuration Spring
@Configuration
public class PaymentConfig {
    
    @Bean
    @Primary
    public PaymentProcessor defaultStripeProcessor() {
        return new StripeCheckoutPaymentProcessor();
    }
    
    @Bean("stripeAdvancedPaymentProcessor")
    public PaymentProcessor advancedStripeProcessor() {
        return new StripePaymentProcessor();
    }
}
```

Cette architecture permet une migration progressive et l'utilisation contextuelle du processeur approprié.