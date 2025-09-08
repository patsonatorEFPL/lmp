# 🚀 GUIDE DE DÉPLOIEMENT PRODUCTION - LMP SERVICES

## ✅ Services Mails Configurés

Votre application est maintenant prête pour la production avec les services mails Mailtrap intégrés.

### 📧 **Configuration Email Production**

- **Serveur SMTP** : `live.smtp.mailtrap.io:587`
- **Domaine configuré** : `lmp-services.ca`
- **Adresses opérationnelles** :
  - `noreply@lmp-services.ca` (emails transactionnels)
  - `support@lmp-services.ca` (support client)

---

## 🔧 **ÉTAPES DE DÉPLOIEMENT**

### 1. **Vérification pré-déploiement**

```bash
# Compilation et tests
mvn clean compile
mvn test -Dtest="!*MailtrapService*" # Exclure les tests SDK désactivés

# Construction du JAR
mvn clean package -DskipTests
```

### 2. **Configuration Mailtrap Production**

✅ **Déjà configuré** dans `application.properties` :
- Serveur SMTP live Mailtrap
- Identifiants API configurés
- Domaine `lmp-services.ca` authentifié

### 3. **Services Email Disponibles**

#### **Emails Transactionnels (noreply@lmp-services.ca)**
- `emailService.sendWelcomeEmail(email, firstName)` - Email de bienvenue HTML
- `emailService.sendTestEmail(email)` - Email de test
- `emailService.sendSimpleEmail(to, subject, body)` - Email texte simple
- `emailService.sendHtmlEmail(to, subject, template, variables)` - Email HTML avec template

#### **Emails de Support (support@lmp-services.ca)**  
- `emailService.sendSupportEmail(to, subject, body)` - Emails bidirectionnels avec le client

### 4. **Interface d'Administration**

URL : `https://lmp-services.ca/admin/email-test`

**Fonctionnalités** :
- Diagnostic de la configuration SMTP
- Test d'envoi d'emails
- Surveillance des services
- **Authentification requise** : Rôle ADMIN

---

## 📋 **CHECKLIST DE DÉPLOIEMENT**

### ✅ **Services Mails**
- [x] Configuration SMTP Mailtrap Live 
- [x] Domaine `lmp-services.ca` vérifié dans Mailtrap
- [x] Adresses `noreply@` et `support@` configurées
- [x] Templates HTML Thymeleaf opérationnels
- [x] Tests d'envoi validés
- [x] Contrôleur temporaire supprimé

### ✅ **Configuration Production**
- [x] Logs optimisés (niveau INFO)
- [x] DevTools désactivé
- [x] Thymeleaf cache activé
- [x] Debug SMTP désactivé
- [x] Configuration sécurisée

### ✅ **Base de Données**
- [x] Pool de connexions HikariCP optimisé
- [x] Configuration Coolify opérationnelle
- [x] Flyway désactivé (gestion manuelle)

### ✅ **Sécurité & Performance**
- [x] HTTPS configuré
- [x] Headers de sécurité activés
- [x] Compression activée
- [x] Sessions sécurisées

---

## 🌐 **DÉPLOIEMENT SUR COOLIFY**

### **Commande de déploiement** :
```bash
# Construction de l'image Docker (si applicable)
docker build -t lmp-services .

# Ou déploiement JAR direct
java -jar target/lmp-1.0.1.jar --spring.profiles.active=production
```

### **Variables d'environnement Coolify** :
```bash
# Port (défini par Coolify)
PORT=8080

# Base de données (déjà configurée)
SPRING_DATASOURCE_URL=jdbc:mysql://...
```

---

## 📧 **TESTS POST-DÉPLOIEMENT**

### 1. **Test de connectivité**
```bash
curl -I https://lmp-services.ca/admin/email-test
# Attendu: HTTP 200 (après connexion admin)
```

### 2. **Test d'envoi d'email**
- Connectez-vous en tant qu'admin
- Accédez à `/admin/email-test`
- Envoyez un email de test à votre adresse
- Vérifiez la réception dans votre boîte email

### 3. **Vérification Mailtrap**
- Connectez-vous à votre dashboard Mailtrap
- Vérifiez les statistiques d'envoi
- Consultez les logs de livraison

---

## 🔍 **SURVEILLANCE & MONITORING**

### **Logs à surveiller** :
```bash
# Logs des services email
tail -f logs/spring.log | grep "com.lmp.service.email"

# Logs d'erreurs SMTP
tail -f logs/spring.log | grep "ERROR.*mail"
```

### **Métriques importantes** :
- Taux de livraison des emails
- Temps de réponse SMTP
- Erreurs d'authentification
- Usage des templates Thymeleaf

---

## 🛠 **DÉPANNAGE**

### **Problèmes fréquents** :

#### **Email non reçu**
1. Vérifier les logs : `logging.level.org.springframework.mail=INFO`
2. Contrôler le dashboard Mailtrap
3. Vérifier la configuration DNS du domaine

#### **Erreur SMTP**
1. Vérifier la connectivité : `telnet live.smtp.mailtrap.io 587`
2. Contrôler les identifiants Mailtrap
3. Vérifier les logs d'authentification

#### **Template non rendu**
1. Vérifier l'emplacement : `src/main/resources/templates/emails/`
2. Contrôler les variables Thymeleaf
3. Tester avec email simple en fallback

---

## 📞 **CONTACT SUPPORT**

- **Interface Admin** : `https://lmp-services.ca/admin/email-test`
- **Logs Application** : Niveau INFO configuré
- **Support Mailtrap** : Dashboard en ligne

---

## 🎯 **PROCHAINES ÉTAPES RECOMMANDÉES**

1. **Monitoring avancé** : Intégrer des alertes sur les erreurs email
2. **Templates supplémentaires** : Créer d'autres templates selon les besoins
3. **Statistiques** : Utiliser l'API Mailtrap pour des rapports détaillés
4. **Tests automatisés** : Créer des tests d'intégration pour les services email

---

**🚀 Votre application LMP Services est prête pour la production avec des services mails professionnels !**
