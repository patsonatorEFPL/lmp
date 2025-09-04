# Documentation Coolify - LMP Services

## Index de la documentation pour le déploiement sur `https://lmp-services.ca`

Cette documentation complète couvre tous les aspects du déploiement et de la maintenance de l'application LMP Services sur la plateforme Coolify.

---

## 📚 Documentation Disponible

### 🚀 [Guide de Déploiement Principal](./README-DEPLOYMENT.md)
Configuration complète pour déployer l'application Spring Boot LMP sur Coolify avec le domaine `https://lmp-services.ca`.

**Contenu :**
- Variables d'environnement obligatoires
- Configuration du service Coolify
- Post-déploiement et validation
- Monitoring et logs
- Tests de connectivité

### 🏗️ [Architecture Coolify](./ARCHITECTURE-COOLIFY.md)
Guide détaillé de l'architecture de déploiement supportée par Coolify.

**Contenu :**
- Architectures supportées (direct et avec reverse proxy)
- Configuration Spring Boot pour headers X-Forwarded
- Flux de déploiement
- Optimisations et performance
- Maintenance et monitoring

### ⚙️ [Variables d'Environnement](./ENVIRONMENT-VARIABLES.md)
Documentation complète de toutes les variables d'environnement requises et optionnelles.

**Contenu :**
- Variables obligatoires vs optionnelles
- Configuration par environnement
- Sécurité des variables sensibles
- Templates de configuration
- Migration depuis l'ancien domaine

### 🛠️ [Guide de Dépannage](./TROUBLESHOOTING.md)
Solutions aux problèmes courants lors du déploiement et de l'exploitation.

**Contenu :**
- Problèmes de déploiement
- Erreurs réseau et SSL
- Problèmes de base de données
- Issues Stripe et email
- Outils de diagnostic

### ✅ [Checklist de Validation](./VALIDATION-CHECKLIST.md)
Liste complète de vérifications pour valider un déploiement réussi.

**Contenu :**
- Tests pré-déploiement
- Validation post-déploiement
- Tests fonctionnels complets
- Critères de validation
- Actions post-validation

---

## 🎯 Démarrage Rapide

### 1. Premier Déploiement

```bash
# 1. Configurer les variables dans Coolify
# Voir : ENVIRONMENT-VARIABLES.md

# 2. Déployer avec docker-compose.coolify.yml
# Voir : README-DEPLOYMENT.md

# 3. Valider le déploiement
# Voir : VALIDATION-CHECKLIST.md
```

### 2. Variables Essentielles

```bash
# Configuration minimale
SPRING_PROFILES_ACTIVE=prod
APP_BASE_URL=https://lmp-services.ca
SERVER_FORWARD_HEADERS_STRATEGY=framework
SERVER_USE_FORWARD_HEADERS=true

# Secrets requis
DATABASE_PASSWORD=***
STRIPE_SECRET_KEY=sk_live_***
STRIPE_WEBHOOK_SECRET=whsec_***
MAIL_PASSWORD=***
```

### 3. Tests Rapides

```bash
# Vérification de base
curl https://lmp-services.ca/actuator/health

# Test SSL
curl -I https://lmp-services.ca

# Validation complète
# Suivre VALIDATION-CHECKLIST.md
```

---

## 🔄 Workflows Courants

### Déploiement de Mise à Jour

1. **Push du code** vers le repository Git
2. **Coolify détecte** automatiquement les changements
3. **Build et déploiement** automatique
4. **Validation** avec VALIDATION-CHECKLIST.md

### Résolution de Problème

1. **Consulter** TROUBLESHOOTING.md
2. **Vérifier** les logs dans Coolify
3. **Tester** les endpoints critiques
4. **Escalade** si nécessaire

### Changement de Configuration

1. **Modifier** les variables dans Coolify
2. **Redémarrer** le service
3. **Valider** avec les tests appropriés
4. **Documenter** les changements

---

## 🏛️ Architecture Overview

```
Internet
   ↓
DNS (lmp-services.ca)
   ↓
Coolify Platform
   ↓ (SSL/TLS automatique)
Load Balancer (Coolify)
   ↓
Spring Boot Application (Port 8080)
   ↓
Base de données (MySQL/PostgreSQL)
```

**Composants clés :**
- **Coolify** : Plateforme de déploiement et orchestration
- **Spring Boot** : Application Java avec configuration spécifique
- **Base de données** : MySQL ou PostgreSQL
- **SSL/TLS** : Géré automatiquement par Coolify

---

## 📊 Monitoring et Maintenance

### URLs de Monitoring

```bash
# Health check principal
https://lmp-services.ca/actuator/health

# Informations application
https://lmp-services.ca/actuator/info

# Pages principales
https://lmp-services.ca/
https://lmp-services.ca/services
https://lmp-services.ca/contact
```

### Logs Importants

```bash
# Logs application (Coolify Interface)
Services → LMP → Logs

# Logs de build
Services → LMP → Build Logs

# Métriques ressources
Services → LMP → Metrics
```

### Tâches de Maintenance

- **Quotidienne** : Vérifier health checks et logs d'erreur
- **Hebdomadaire** : Analyser performance et utilisation ressources
- **Mensuelle** : Mise à jour dépendances et révision sécurité

---

## 🔒 Sécurité

### Variables Sensibles

Ces variables doivent être configurées comme **secrets** dans Coolify :
- `DATABASE_PASSWORD`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `MAIL_PASSWORD`

### Headers de Sécurité

L'application configure automatiquement :
- X-Frame-Options: DENY
- X-Content-Type-Options: nosniff
- X-XSS-Protection: 1; mode=block
- Strict-Transport-Security (HTTPS)

### Certificats SSL

- **Gestion** : Automatique par Coolify
- **Renouvellement** : Automatique
- **Validation** : Via tests dans VALIDATION-CHECKLIST.md

---

## 📞 Support et Escalation

### Niveaux de Support

1. **Auto-diagnostic** : TROUBLESHOOTING.md
2. **Logs Coolify** : Interface web → Services → Logs
3. **Tests manuels** : VALIDATION-CHECKLIST.md
4. **Escalation** : Contact équipe infrastructure

### Informations à Collecter

Avant escalation :
```bash
# Status service
curl -I https://lmp-services.ca

# Health check
curl https://lmp-services.ca/actuator/health

# Logs récents (Coolify Interface)
# Variables d'environnement (masquer secrets)
# Configuration service Coolify
```

---

## 🔄 Historique des Changements

### Migration vers lmp-services.ca

- **Date** : 2025-01-03
- **Changements** :
  - Migration domaine : `lmp.run.place` → `lmp-services.ca`
  - Mise à jour documentation Coolify complète
  - Configuration headers X-Forwarded pour reverse proxy
  - Guides de troubleshooting et validation étendus

### Fichiers Modifiés

- `README-DEPLOYMENT.md` : URLs et variables mises à jour
- `docker-compose.coolify.yml` : Nouvelles variables d'environnement
- **Nouveaux fichiers** :
  - `ARCHITECTURE-COOLIFY.md`
  - `ENVIRONMENT-VARIABLES.md`
  - `TROUBLESHOOTING.md`
  - `VALIDATION-CHECKLIST.md`

---

## 📋 Checklist Rapide

### Avant Déploiement
- [ ] Variables d'environnement configurées
- [ ] Secrets Coolify définis
- [ ] Base de données accessible
- [ ] Webhooks Stripe mis à jour

### Après Déploiement
- [ ] Health check vert
- [ ] HTTPS fonctionnel
- [ ] Emails opérationnels
- [ ] Tests fonctionnels passés

### Maintenance Continue
- [ ] Logs surveillés
- [ ] Performance monitored
- [ ] Sauvegardes vérifiées
- [ ] Sécurité maintenue

---

**🎯 Objectif** : Cette documentation garantit un déploiement fiable et une maintenance efficace de l'application LMP Services sur Coolify avec le domaine `https://lmp-services.ca`.

**📝 Note** : Consultez toujours la documentation spécifique selon votre besoin. En cas de doute, référez-vous à TROUBLESHOOTING.md ou utilisez la checklist de validation.