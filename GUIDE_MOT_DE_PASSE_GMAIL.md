# Guide Complet : Créer un Mot de Passe d'Application Gmail

## 🔐 Pourquoi un Mot de Passe d'Application ?

Gmail ne permet plus l'utilisation du mot de passe principal pour les applications tierces. Vous devez créer un **mot de passe d'application** spécifique de 16 caractères.

## 📋 Étapes pour Créer le Mot de Passe d'Application

### **Étape 1 : Activer l'Authentification à 2 Facteurs**

1. Allez sur [myaccount.google.com](https://myaccount.google.com)
2. Connectez-vous avec `lmp.assistance@gmail.com`
3. Cliquez sur **"Sécurité"** dans le menu de gauche
4. Sous **"Se connecter à Google"**, cliquez sur **"Validation en deux étapes"**
5. **Activez la validation en deux étapes** si elle n'est pas déjà active
6. Suivez les instructions (SMS, appel, ou application authenticator)

### **Étape 2 : Créer le Mot de Passe d'Application**

1. Retournez dans **"Sécurité"** > **"Validation en deux étapes"**
2. Faites défiler vers le bas jusqu'à **"Mots de passe des applications"**
3. Cliquez sur **"Mots de passe des applications"**
4. Sélectionnez **"Autre (nom personnalisé)"**
5. Entrez : **"LMP Spring Boot Application"**
6. Cliquez sur **"GÉNÉRER"**

### **Étape 3 : Copier le Mot de Passe Généré**

Gmail va afficher un mot de passe de **16 caractères** comme :
```
abcd efgh ijkl mnop
```

**⚠️ IMPORTANT** : Copiez ce mot de passe **sans les espaces** : `abcdefghijklmnop`

### **Étape 4 : Remplacer dans application.properties**

Dans le fichier `src/main/resources/application.properties`, ligne 89 :

**AVANT :**
```properties
spring.mail.password=GMAIL_APP_PASSWORD_REQUIRED
```

**APRÈS :**
```properties
spring.mail.password=abcdefghijklmnop
```

*(Remplacez `abcdefghijklmnop` par votre vrai mot de passe de 16 caractères)*

## 🧪 Test de Vérification

Après avoir mis le bon mot de passe :

1. **Redémarrez** l'application Spring Boot
2. Testez via `/admin/email-test` ou le formulaire `/contact`
3. L'email devrait arriver dans `lmp.assistance@gmail.com`

## 🔧 Configuration Finale

Votre configuration complète devrait ressembler à :

```properties
# Configuration des emails Gmail pour développement
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=lmp.assistance@gmail.com
spring.mail.password=abcdefghijklmnop
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com
spring.mail.properties.mail.smtp.ssl.protocols=TLSv1.2
spring.mail.properties.mail.debug=true
```

## ❗ Problèmes Courants

### **"Mot de passe des applications" n'apparaît pas**
- Vérifiez que l'authentification à 2 facteurs est **active**
- Attendez quelques minutes après activation de 2FA

### **Erreur "Authentication failed"**
- Vérifiez que vous avez copié le mot de passe **sans espaces**
- Assurez-vous d'utiliser le compte `lmp.assistance@gmail.com`

### **Erreur "Connection timeout"**
- Vérifiez votre connexion internet
- Certains réseaux bloquent les ports SMTP

## 🎯 Étapes Suivantes

Une fois le mot de passe configuré :

1. ✅ **Test local** : Formulaire de contact fonctionnel
2. ✅ **Production Railway** : Configurer `MAIL_PASSWORD` avec le même mot de passe
3. ✅ **Monitoring** : Vérifier les logs d'envoi d'emails

---

**Note de Sécurité** : Ce mot de passe d'application donne accès à votre Gmail. Ne le partagez jamais et stockez-le de manière sécurisée.