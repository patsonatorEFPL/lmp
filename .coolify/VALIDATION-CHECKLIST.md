# Checklist de Validation - LMP Services Coolify

## Guide de validation pour le déploiement sur `https://lmp-services.ca`

Cette checklist garantit que tous les aspects de l'application fonctionnent correctement après la migration vers le nouveau domaine et la configuration Coolify.

---

## ✅ Pré-déploiement

### Configuration Coolify

- [ ] **Variables d'environnement configurées**
  ```bash
  SPRING_PROFILES_ACTIVE=prod
  APP_BASE_URL=https://lmp-services.ca
  SERVER_FORWARD_HEADERS_STRATEGY=framework
  SERVER_USE_FORWARD_HEADERS=true
  ```

- [ ] **Secrets configurés**
  ```bash
  DATABASE_PASSWORD=***
  STRIPE_SECRET_KEY=sk_live_***
  STRIPE_WEBHOOK_SECRET=whsec_***
  MAIL_PASSWORD=***
  ```

- [ ] **Service Coolify configuré**
  - Type: Docker Compose
  - Fichier: `docker-compose.coolify.yml`
  - Domaine: `lmp-services.ca`
  - Port: 8080
  - Health check: `/actuator/health`

### Base de Données

- [ ] **Connectivité testée**
  ```bash
  # Test de connexion depuis un client externe
  mysql -h database_host -u username -p database_name
  ```

- [ ] **Permissions vérifiées**
  ```sql
  SHOW GRANTS FOR 'username'@'%';
  -- Doit inclure : SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP
  ```

- [ ] **Migrations prêtes**
  - Scripts Flyway dans `src/main/resources/db/migration/`
  - Numérotation correcte (V1__, V2__, etc.)

---

## 🚀 Déploiement

### Build et Démarrage

- [ ] **Build Docker réussi**
  ```bash
  # Dans Coolify → Services → Build logs
  # Vérifier : "Successfully built" et pas d'erreurs
  ```

- [ ] **Container démarré**
  ```bash
  # Dans Coolify → Services → Status
  # État : "Running" (vert)
  ```

- [ ] **Health check passé**
  ```bash
  # Test automatique
  curl https://lmp-services.ca/actuator/health
  
  # Réponse attendue :
  {
    "status": "UP",
    "components": {
      "db": {"status": "UP"},
      "ping": {"status": "UP"}
    }
  }
  ```

### Logs de Démarrage

- [ ] **Pas d'erreurs critiques**
  ```bash
  # Dans Coolify → Services → Logs
  # Rechercher : "Started LmpApplication"
  # Pas de : "ERROR", "FATAL", "Exception"
  ```

- [ ] **Base de données connectée**
  ```bash
  # Dans les logs, rechercher :
  # "HikariPool-1 - Start completed"
  # "Flyway Community Edition ... successfully applied"
  ```

---

## 🌐 Tests de Connectivité

### Domaine et SSL

- [ ] **Résolution DNS**
  ```bash
  nslookup lmp-services.ca
  # Doit retourner l'IP du serveur Coolify
  ```

- [ ] **Certificat SSL valide**
  ```bash
  openssl s_client -connect lmp-services.ca:443 -servername lmp-services.ca
  # Vérifier : "Verify return code: 0 (ok)"
  ```

- [ ] **Redirection HTTP → HTTPS**
  ```bash
  curl -I http://lmp-services.ca
  # Réponse attendue : "301 Moved Permanently" vers https://
  ```

### Endpoints Principaux

- [ ] **Page d'accueil**
  ```bash
  curl -I https://lmp-services.ca
  # Réponse : 200 OK
  ```

- [ ] **Health check**
  ```bash
  curl https://lmp-services.ca/actuator/health
  # Réponse : {"status":"UP"}
  ```

- [ ] **Sitemap**
  ```bash
  curl https://lmp-services.ca/sitemap.xml
  # Réponse : 200 OK avec contenu XML
  ```

- [ ] **Robots.txt**
  ```bash
  curl https://lmp-services.ca/robots.txt
  # Réponse : 200 OK
  ```

### Pages Principales

- [ ] **Services**
  ```bash
  curl -I https://lmp-services.ca/services
  # Réponse : 200 OK
  ```

- [ ] **Contact**
  ```bash
  curl -I https://lmp-services.ca/contact
  # Réponse : 200 OK
  ```

- [ ] **Admin (si accessible)**
  ```bash
  curl -I https://lmp-services.ca/admin/login
  # Réponse : 200 OK
  ```

---

## 🔧 Tests Fonctionnels

### Authentification

- [ ] **Inscription utilisateur**
  - Créer un compte de test
  - Vérifier la réception de l'email de confirmation
  - Confirmer l'email
  - Vérifier la connexion

- [ ] **Connexion/Déconnexion**
  - Connexion avec utilisateur test
  - Vérification de la session
  - Déconnexion propre

- [ ] **Gestion des mots de passe**
  - Test de "mot de passe oublié"
  - Réception de l'email de réinitialisation
  - Changement de mot de passe

### Paiements Stripe

- [ ] **Configuration webhooks Stripe**
  ```bash
  # Dans Stripe Dashboard → Webhooks
  # URL : https://lmp-services.ca/api/webhooks/stripe
  # Status : Enabled
  ```

- [ ] **Test de paiement (mode test)**
  - Créer une session de checkout
  - Utiliser carte de test : 4242424242424242
  - Vérifier la confirmation de paiement
  - Contrôler la réception du webhook

- [ ] **Gestion des erreurs de paiement**
  - Tester avec carte déclinée : 4000000000000002
  - Vérifier la gestion d'erreur

### Emails

- [ ] **Configuration SMTP**
  ```bash
  # Vérifier dans les logs au démarrage :
  # "Mail server configured successfully"
  ```

- [ ] **Email de contact**
  - Envoyer un message via le formulaire de contact
  - Vérifier la réception sur `lmp.assistance@gmail.com`
  - Contrôler l'email de confirmation à l'utilisateur

- [ ] **Emails transactionnels**
  - Email de confirmation d'inscription
  - Email de confirmation de commande
  - Email de changement de statut

---

## 🔍 Tests de Performance

### Temps de Réponse

- [ ] **Page d'accueil < 2s**
  ```bash
  curl -w "@curl-format.txt" -o /dev/null -s https://lmp-services.ca
  ```

- [ ] **Health check < 500ms**
  ```bash
  curl -w "%{time_total}\n" -o /dev/null -s https://lmp-services.ca/actuator/health
  ```

- [ ] **API endpoints < 1s**
  ```bash
  # Test des endpoints critiques
  curl -w "%{time_total}\n" -o /dev/null -s https://lmp-services.ca/services
  ```

### Ressources

- [ ] **Utilisation mémoire**
  ```bash
  # Dans Coolify → Services → Metrics
  # RAM utilisée < 80% de la limite configurée
  ```

- [ ] **Utilisation CPU**
  ```bash
  # Dans Coolify → Services → Metrics  
  # CPU utilisé < 70% en moyenne
  ```

---

## 🔒 Tests de Sécurité

### Headers HTTP

- [ ] **Headers de sécurité présents**
  ```bash
  curl -I https://lmp-services.ca | grep -E "(X-Frame-Options|X-Content-Type-Options|Strict-Transport-Security)"
  ```

- [ ] **Headers X-Forwarded gérés**
  ```bash
  # Vérifier dans les logs d'application
  # Headers X-Forwarded-Proto, X-Forwarded-Host détectés
  ```

### SSL/TLS

- [ ] **Protocoles sécurisés uniquement**
  ```bash
  # Test SSL Labs : https://www.ssllabs.com/ssltest/
  # Grade attendu : A ou A+
  ```

- [ ] **Certificat valide**
  ```bash
  # Vérifier :
  # - Émis pour lmp-services.ca
  # - Date d'expiration > 30 jours
  # - Chaîne de certificats complète
  ```

---

## 📊 Tests de Monitoring

### Logging

- [ ] **Logs d'application**
  ```bash
  # Dans Coolify → Services → Logs
  # Logs structurés et lisibles
  # Pas d'erreurs répétitives
  ```

- [ ] **Niveaux de logs appropriés**
  ```bash
  # Production : INFO, WARN, ERROR
  # Pas de logs DEBUG en production
  ```

### Métriques

- [ ] **Actuator configuré**
  ```bash
  curl https://lmp-services.ca/actuator/info
  # Informations de build et version
  ```

- [ ] **Health checks détaillés**
  ```bash
  curl https://lmp-services.ca/actuator/health
  # Status des composants (db, mail, etc.)
  ```

---

## 🔄 Tests de Récupération

### Redémarrage

- [ ] **Redémarrage propre**
  ```bash
  # Dans Coolify → Services → Restart
  # Application redémarre sans erreur
  # Health check redevient UP rapidement
  ```

- [ ] **Persistence des données**
  ```bash
  # Vérifier après redémarrage :
  # - Données en base de données intactes
  # - Logs conservés dans le volume
  # - Sessions utilisateur gérées correctement
  ```

### Base de Données

- [ ] **Connexion automatique**
  ```bash
  # Après redémarrage BD, vérifier :
  # - Reconnexion automatique de l'application
  # - Pool de connexions reconstitué
  ```

---

## 📋 Validation Finale

### Checklist Globale

- [ ] **Toutes les URLs avec nouveau domaine**
  ```bash
  # Vérifier dans les emails générés
  # Vérifier dans les redirections
  # Vérifier dans les liens absolus
  ```

- [ ] **Migration des données**
  ```bash
  # Si migration depuis ancien domaine :
  # - Données utilisateur migrées
  # - Commandes historiques accessibles
  # - Paramètres système mis à jour
  ```

- [ ] **Webhooks externes mis à jour**
  ```bash
  # Stripe : https://lmp-services.ca/api/webhooks/stripe
  # Autres services : URLs mises à jour
  ```

### Tests d'Intégration

- [ ] **Workflow complet utilisateur**
  1. Visite du site → ✅
  2. Navigation services → ✅  
  3. Création compte → ✅
  4. Confirmation email → ✅
  5. Commande service → ✅
  6. Paiement Stripe → ✅
  7. Confirmation commande → ✅

- [ ] **Workflow administrateur**
  1. Connexion admin → ✅
  2. Gestion commandes → ✅
  3. Gestion utilisateurs → ✅
  4. Rapports → ✅

---

## 🎯 Critères de Validation

### Critères Obligatoires (Bloquants)

- ✅ Health check répond "UP"
- ✅ HTTPS fonctionne avec certificat valide
- ✅ Base de données accessible
- ✅ Webhooks Stripe configurés
- ✅ Emails fonctionnels

### Critères Recommandés

- ✅ Temps de réponse < 2s
- ✅ Headers de sécurité présents
- ✅ Logs propres sans erreurs
- ✅ Monitoring fonctionnel
- ✅ Workflow complet testé

---

## 📞 Actions Post-Validation

### Succès
1. ✅ Documentation mise à jour
2. ✅ Équipe notifiée du nouveau domaine
3. ✅ DNS ancien domaine (si applicable) redirigé
4. ✅ Monitoring mis en place

### Échec
1. ❌ Rollback vers configuration précédente
2. ❌ Investigation des logs d'erreur
3. ❌ Correction des problèmes identifiés
4. ❌ Nouvelle validation

---

**📝 Note**: Cette checklist doit être complétée intégralement avant la mise en production sur `https://lmp-services.ca`. Tout échec d'un critère obligatoire doit être résolu avant de procéder.