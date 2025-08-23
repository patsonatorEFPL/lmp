# Guide de Configuration Email pour Railway.com

## Configuration des Variables d'Environnement Email

### 1. Variables Requises pour Railway

Configurez les variables d'environnement suivantes dans Railway.com :

```bash
# Configuration Email Gmail
MAIL_USERNAME=lmp.assistance@gmail.com
MAIL_PASSWORD=Menkepslmp
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587

# Configuration entreprise (optionnel)
COMPANY_EMAIL=contact@lmp-digital.ca
COMPANY_ADDRESS=123 Rue Principale, Ville, Province, Code Postal
COMPANY_PHONE=+1 (555) 123-4567
```

### 2. Configuration du Mot de Passe d'Application Gmail

**IMPORTANT** : Pour utiliser Gmail SMTP, vous devez créer un "Mot de passe d'application" :

1. **Activer l'authentification à 2 facteurs** sur votre compte Gmail
2. Aller dans **Paramètres du compte Google** > **Sécurité**
3. Sous "Se connecter à Google", cliquer sur **Mots de passe des applications**
4. Sélectionner **Application** : "Autre (nom personnalisé)"
5. Entrer **"LMP Spring Boot App"** comme nom
6. **Copier le mot de passe généré** (16 caractères)
7. Utiliser ce mot de passe dans la variable `MAIL_PASSWORD`

### 3. Configuration dans Railway.com

#### Via l'interface Web Railway :
1. Aller dans votre projet Railway
2. Cliquer sur **Variables**
3. Ajouter chaque variable :
   - Nom : `MAIL_USERNAME`
   - Valeur : `lmp.assistance@gmail.com`
   - Répéter pour toutes les variables

#### Via Railway CLI :
```bash
railway variables set MAIL_USERNAME=lmp.assistance@gmail.com
railway variables set MAIL_PASSWORD=votre_mot_de_passe_application
railway variables set MAIL_HOST=smtp.gmail.com
railway variables set MAIL_PORT=587
```

### 4. Vérification de la Configuration

Après déploiement, testez via :
- **Interface de diagnostic** : `/admin/email-test`
- **Logs Railway** : Vérifier les logs de connexion SMTP
- **Test d'envoi** : Utiliser le formulaire de test

### 5. Dépannage Commun

#### Erreur "Authentication failed" :
- Vérifier que l'authentification 2FA est activée
- Régénérer le mot de passe d'application
- Vérifier que la variable `MAIL_PASSWORD` est correcte

#### Erreur "Connection timeout" :
- Vérifier les variables `MAIL_HOST` et `MAIL_PORT`
- S'assurer que Railway autorise les connexions SMTP sortantes

#### Erreur "Templates not found" :
- Vérifier que tous les templates sont déployés dans `/templates/emails/`
- Contrôler les logs Spring Boot pour les erreurs Thymeleaf

### 6. Templates Email Inclus

Les templates suivants sont configurés :
- ✅ `order-confirmation.html` - Confirmation de commande
- ✅ `order-status-change.html` - Changement de statut
- ✅ `order-cancellation.html` - Annulation de commande
- ✅ `order-refund.html` - Remboursement traité
- ✅ `order-shipping.html` - Notification d'expédition
- ✅ `admin-notification.html` - Notifications administratives

### 7. Configuration de Sécurité

En production, les emails utilisent :
- **STARTTLS** activé pour chiffrement
- **Authentication SMTP** requise
- **SSL Trust** configuré pour Gmail
- **Debugging** désactivé (sauf en cas de problème)

### 8. Monitoring

Surveillez les métriques suivantes :
- **Taux de livraison** des emails
- **Erreurs SMTP** dans les logs
- **Performance** de l'envoi d'emails
- **Utilisation** des quotas Gmail

---

## Commandes Utiles

### Test local avec profil prod :
```bash
mvn spring-boot:run -Dspring.profiles.active=prod
```

### Vérification des variables Railway :
```bash
railway variables
```

### Consultation des logs en temps réel :
```bash
railway logs
```

---

**Note** : Remplacez `votre_mot_de_passe_application` par le vrai mot de passe d'application Gmail avant le déploiement.