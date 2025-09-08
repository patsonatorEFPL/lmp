# Implémentation de l'Internationalisation (i18n) - LMP Digital Services

## ✅ Ce qui a été implémenté

### 1. Configuration Spring i18n (`I18nConfig.java`)
- **MessageSource** : Configuration des fichiers de traduction avec encodage UTF-8
- **LocaleResolver** : Résolveur basé sur cookies (90 jours de durée de vie)
- **LocaleChangeInterceptor** : Interception du paramètre `lang` pour changer la langue
- **Locale par défaut** : Français (adapté à l'entreprise québécoise)

### 2. Fichiers de traduction
- **`messages_fr.properties`** : Messages en français (langue par défaut)
- **`messages_en.properties`** : Messages en anglais
- **Structure organisée** : Navigation, accueil, services, contact, auth, etc.
- **Plus de 200 clés de traduction** couvrant les fonctionnalités principales

### 3. Contrôleur de changement de langue (`LanguageController.java`)
- **Endpoint `/lang`** : Gestion du changement de langue avec validation
- **Redirection intelligente** : Retour à la page précédente après changement
- **Validation des langues** : Seuls FR et EN sont acceptés
- **Gestion d'erreurs** : Fallback vers le français en cas d'erreur

### 4. Sélecteurs de langue UI
- **Header principal** : Drapeaux 🇫🇷 🇺🇸 avec état actif (opacité)
- **Page des paramètres** : Sélecteur avancé avec JavaScript
- **État visuel** : Indication claire de la langue active

### 5. Internationalisation des templates
- **Layout principal** : Navigation, footer, meta tags avec attribut `lang`
- **Page d'accueil** : Titre, sous-titre et méta descriptions traduites
- **Pages des paramètres** : Interface traduite pour les options linguistiques

### 6. Configuration de sécurité
- **Accès public** : Endpoint `/lang` accessible sans authentification
- **CSRF** : Configuration appropriée pour les changements de langue

## 🔧 Comment tester l'implémentation

### 1. Démarrer l'application
```bash
./mvnw spring-boot:run
```

### 2. Tester les URLs
- **Français** : http://localhost:8080/?lang=fr
- **Anglais** : http://localhost:8080/?lang=en
- **Page d'accueil** : Vérifier le changement du titre et sous-titre
- **Navigation** : Vérifier les liens de navigation

### 3. Tester l'interface
- **Drapeaux dans le header** : Cliquer sur 🇫🇷 ou 🇺🇸
- **Page des paramètres** : `/settings` → Préférences → Langue
- **Persistance** : La langue doit être persistée via cookie

### 4. Vérifier la persistance
- Changer de langue et fermer le navigateur
- Rouvrir : la langue choisie doit être conservée
- Cookie `lmp_locale` visible dans les outils de développement

## 🚀 Prochaines étapes (TODO restants)

### 1. Finaliser l'internationalisation des templates
```bash
# Pages prioritaires à traduire :
- services.html (page des services)
- contact.html (formulaire de contact)  
- about.html (page à propos)
- auth/login.html et auth/register.html
- user/dashboard.html
- admin/* (pages d'administration)
```

### 2. Messages de validation et d'erreur
```properties
# Créer ValidationMessages_fr.properties et ValidationMessages_en.properties
# Traduire les messages Spring Security
# Tester les pages d'erreur 404/500
```

### 3. Persistance utilisateur avancée (optionnel)
```sql
-- Ajouter une colonne à la table users
ALTER TABLE users ADD COLUMN preferred_locale VARCHAR(5) DEFAULT 'fr';
```
```java
// Modifier User.java pour inclure preferredLocale
// Mettre à jour au changement de langue si utilisateur connecté
// Charger la préférence au login
```

### 4. Fonctionnalités avancées
- **Détection automatique** : Langue du navigateur au premier accès
- **Formats de date/heure** : Adaptation selon la locale
- **Formatage des devises** : $ vs CAD selon la langue
- **Direction de texte** : Support RTL si nécessaire

## 📁 Structure des fichiers créés/modifiés

### Nouveaux fichiers
```
src/main/java/com/lmp/config/I18nConfig.java
src/main/java/com/lmp/web/controller/LanguageController.java
src/main/resources/i18n/messages_fr.properties
src/main/resources/i18n/messages_en.properties
```

### Fichiers modifiés
```
src/main/resources/templates/layout.html (header, navigation, footer)
src/main/resources/templates/index.html (page d'accueil)
src/main/resources/templates/user/settings.html (sélecteur de langue)
src/main/java/com/lmp/config/SecurityConfig.java (autorisation /lang)
```

## 🎯 Utilisation dans les templates Thymeleaf

### Syntaxe de base
```html
<!-- Texte simple -->
<h1 th:text="#{home.title}">Titre par défaut</h1>

<!-- Attribut HTML -->
<meta name="description" th:content="#{home.subtitle}">

<!-- Avec paramètres -->
<p th:text="#{dashboard.welcome(${user.name})}">Bienvenue, Utilisateur!</p>

<!-- Classe conditionnelle selon la langue -->
<div th:classappend="${#locale.language == 'fr' ? 'french-style' : 'english-style'}">
```

### Variables utiles
```html
<!-- Langue actuelle -->
${#locale.language}     <!-- 'fr' ou 'en' -->
${#locale.displayName}  <!-- 'français' ou 'English' -->

<!-- URL actuelle -->
${#httpServletRequest.requestURI}

<!-- Messages avec paramètres -->
#{validation.password.min(8)}  <!-- "Le mot de passe doit contenir au moins 8 caractères" -->
```

## 🔍 Debug et dépannage

### Vérifications communes
1. **Fichiers de traduction** : Vérifier l'encodage UTF-8
2. **Cache** : Redémarrer l'application si les traductions ne se chargent pas
3. **Cookies** : Vérifier le cookie `lmp_locale` dans le navigateur
4. **Logs** : Messages dans la console lors du changement de langue
5. **Clés manquantes** : Les clés non trouvées s'affichent entre `???`

### Messages de debug
```java
// LanguageController affiche :
"✓ Langue changée vers: fr"  // ou "en"
"✗ Erreur lors du changement de langue: [message]"
```

## 🌟 Fonctionnalités implémentées

### ✅ Core i18n
- [x] Configuration Spring complète
- [x] Fichiers de traduction FR/EN
- [x] Changement de langue via URL
- [x] Persistance par cookies
- [x] Interface utilisateur (drapeaux)

### ✅ Templates
- [x] Layout principal (navigation/footer)
- [x] Page d'accueil
- [x] Page des paramètres utilisateur
- [x] Attribut lang dynamique

### ✅ Sécurité
- [x] Endpoint public pour changement de langue
- [x] Validation des langues supportées
- [x] Gestion d'erreurs robuste

### 🔄 En cours/À venir
- [ ] Templates restants (services, contact, auth)
- [ ] Messages de validation
- [ ] Persistance en base de données
- [ ] Tests d'intégration

---

**Implémenté par** : Assistant IA  
**Date** : 7 septembre 2025  
**Version** : LMP 1.0.1  
**Status** : ✅ Fonctionnel - Prêt pour les tests
