# 🔍 Guide de Diagnostic - Problème Stripe "Site Dangereux"

## 📋 Problème Identifié

**Symptôme :** Chrome affiche "Site dangereux" lors du retour depuis Stripe Checkout  
**URL d'erreur :** `https://checkout.stripe.com/c/pay/cs_live_...`  
**Contexte :** Migration de `lmp.run.place` vers `lmp-services.ca`

## 🎯 Causes Probables Identifiées

### 1. **Configuration Stripe Dashboard** (Probabilité: 80%)
- Le nouveau domaine `lmp-services.ca` n'est pas configuré dans les URLs de redirection autorisées
- Stripe utilise une whitelist stricte des domaines pour la sécurité

### 2. **Problème SSL/DNS du nouveau domaine** (Probabilité: 60%)
- Certificat SSL non valide ou expiré pour `lmp-services.ca`
- Résolution DNS incorrecte ou propagation incomplète

## 🛠 Modifications de Diagnostic Ajoutées

### ✅ Logs Détaillés dans StripeCheckoutPaymentProcessor
- **Lignes 340-350 :** URLs exactes générées pour success/cancel
- **Lignes 435-445 :** Validation des composants URL et vérification HTTPS
- **Fichier :** `src/main/java/com/lmp/service/payment/processor/StripeCheckoutPaymentProcessor.java`

### ✅ Endpoint de Diagnostic Temporaire
- **URL :** `GET /api/diagnostic/proxy-headers`
- **Fonction :** Affiche la configuration complète (baseUrl, headers X-Forwarded-*, URLs générées)
- **URL Santé :** `GET /api/diagnostic/health`
- **Fichier :** `src/main/java/com/lmp/web/controller/DiagnosticController.java`

### ✅ Filtre de Logging des Requêtes
- **Fonction :** Capture automatiquement les headers X-Forwarded-* pour les requêtes importantes
- **Cible :** `/stripe/`, `/payment/`, `/api/`, `/checkout/`
- **Fichier :** `src/main/java/com/lmp/config/RequestLoggingFilter.java`

### ✅ Configuration de Logging
- **Dev :** Logs DEBUG pour diagnostic complet
- **Prod :** Logs INFO/WARN pour éviter le spam
- **Fichiers :** `application.properties`, `application-prod.properties`

## 📊 Plan de Diagnostic

### Étape 1: Vérification de Base
```bash
# Tester la connectivité du nouveau domaine
curl -I https://lmp-services.ca

# Vérifier le certificat SSL
openssl s_client -connect lmp-services.ca:443 -servername lmp-services.ca
```

### Étape 2: Test de l'Endpoint de Diagnostic
```bash
# Après déploiement, tester l'endpoint
curl https://lmp-services.ca/api/diagnostic/health
curl https://lmp-services.ca/api/diagnostic/proxy-headers
```

### Étape 3: Analyser les Logs
Rechercher dans les logs de l'application :
```
STRIPE_CONFIG_DEBUG
STRIPE_CALLBACK_DEBUG
STRIPE_URL_DEBUG
FORWARDED_HEADERS_DEBUG
```

### Étape 4: Vérifier Stripe Dashboard
1. Aller dans [Stripe Dashboard](https://dashboard.stripe.com/)
2. **Settings > Business settings > Branding**
3. **Developers > Webhooks > Endpoints**
4. Vérifier que `https://lmp-services.ca/*` est autorisé

## 🔧 Solutions Prévisibles

### Si Problème = Configuration Stripe Dashboard
```bash
# URLs à ajouter dans Stripe Dashboard:
https://lmp-services.ca/stripe/checkout/success
https://lmp-services.ca/stripe/checkout/cancel
https://lmp-services.ca/webhook/stripe
```

### Si Problème = SSL/DNS
```bash
# Vérifier DNS
nslookup lmp-services.ca
dig lmp-services.ca

# Vérifier certificat
certbot certificates
```

## 📝 Informations de Debug Attendues

### URLs Générées Attendues
```
successUrl=https://lmp-services.ca/stripe/checkout/success?order_id=123&session_id={CHECKOUT_SESSION_ID}&type=success
cancelUrl=https://lmp-services.ca/stripe/checkout/cancel?order_id=123&session_id={CHECKOUT_SESSION_ID}&type=cancel
```

### Headers X-Forwarded Attendus (avec Nginx)
```
X-Forwarded-Proto: https
X-Forwarded-Host: lmp-services.ca
X-Forwarded-Port: 443
X-Forwarded-For: [client-ip]
```

## ⚠️ Notes Importantes

1. **DiagnosticController est TEMPORAIRE** - À supprimer après résolution
2. **RequestLoggingFilter** - Peut générer beaucoup de logs, ajuster le niveau selon besoin
3. **Logs de sécurité** - Contiennent des informations sensibles, ne pas partager publiquement
4. **Test en Dev** - Valider d'abord localement avant production

## 🎯 Prochaines Étapes

1. **Déployer les modifications** sur branche `develop`
2. **Tester l'endpoint de diagnostic** 
3. **Analyser les logs** générés lors d'un test de paiement
4. **Identifier la cause exacte** basée sur les logs
5. **Appliquer la solution appropriée**
6. **Nettoyer le code de diagnostic** après résolution

---

**Créé le :** 2025-01-03  
**Migration :** lmp.run.place → lmp-services.ca  
**Status :** Diagnostic en cours