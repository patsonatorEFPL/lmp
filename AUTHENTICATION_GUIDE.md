# Guide d'Authentification et d'Autorisation - LMP

## Vue d'ensemble

Ce guide détaille l'implémentation complète du système d'authentification et d'autorisation pour l'application LMP avec les rôles admin, user et visiteur.

## Architecture Implémentée

### Rôles et Permissions

1. **VISITEUR** (non connecté)
   - Accès aux pages publiques : `/`, `/about`, `/services`, `/contact`, `/map`
   - Accès aux pages d'authentification : `/login`, `/register`

2. **ROLE_USER** (utilisateur connecté)
   - Toutes les permissions visiteur
   - Accès au dashboard utilisateur : `/dashboard/**`
   - Gestion du profil : `/profile`
   - Gestion des commandes : `/orders/**`
   - Gestion des avis : `/reviews/**`

3. **ROLE_ADMIN** (administrateur)
   - Toutes les permissions utilisateur
   - Accès au panneau d'administration : `/admin/**`
   - Gestion des utilisateurs
   - Accès aux statistiques
   - Contrôle total de l'application

## Composants Implémentés

### Configuration de Sécurité

- **SecurityConfig** : Configuration Spring Security avec authentification par formulaire
- **CustomUserDetailsService** : Service personnalisé pour charger les utilisateurs
- **PasswordEncoder** : Encodage BCrypt des mots de passe

### Services d'Authentification

- **AuthService/AuthServiceImpl** : Gestion inscription, validation, vérification email
- **UserService/UserServiceImpl** : CRUD utilisateurs, gestion des rôles

### Contrôleurs

- **AuthController** : Connexion, inscription, profil
- **DashboardController** : Tableau de bord utilisateur
- **AdminDashboardController** : Tableau de bord administrateur

### Gestion des Erreurs

- **GlobalExceptionHandler** : Gestionnaire centralisé des exceptions
- Pages d'erreur personnalisées (403, 500)

### Interface Utilisateur

- Pages d'authentification modernes (Bootstrap 5)
- Navigation adaptée selon le rôle
- Dashboards différenciés pour admin/user

## Comptes de Test Disponibles

### Administrateur
- **Email** : `admin@lmp.ca`
- **Mot de passe** : `admin123`
- **Accès** : Toutes les fonctionnalités + administration

### Utilisateur Standard
- **Email** : `user@lmp.ca`
- **Mot de passe** : `user123`
- **Accès** : Fonctionnalités utilisateur standard

### Utilisateurs de Test Supplémentaires
- `marie.martin@example.com` / `password123`
- `pierre.dubois@example.com` / `password123`
- `sarah.wilson@example.com` / `password123`
- `alex.tremblay@example.com` / `password123`
- `locked@example.com` / `password123` (compte verrouillé)
- `inactive@example.com` / `password123` (compte inactif)

## Configuration Base de Données

### Migrations Flyway

1. **V1__init.sql** : Structure des tables principales
2. **V2__insert_default_roles.sql** : Données initiales (rôles + comptes admin/user)

### Configuration Application

```properties
# Base de données
spring.datasource.url=jdbc:mysql://localhost:3306/lmp
spring.datasource.username=root
spring.datasource.password=

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

## Fonctionnalités Clés

### Sécurité

- ✅ Authentification par email/mot de passe
- ✅ Encodage sécurisé des mots de passe (BCrypt)
- ✅ Sessions sécurisées avec "Se souvenir de moi"
- ✅ Protection CSRF
- ✅ Gestion des comptes verrouillés/désactivés
- ✅ Redirection automatique selon le rôle

### Gestion des Utilisateurs

- ✅ Inscription avec validation complète
- ✅ Profils utilisateur complets
- ✅ Gestion des rôles (USER/ADMIN)
- ✅ Soft delete des utilisateurs
- ✅ Historique des connexions

### Interface Utilisateur

- ✅ Design responsive (Bootstrap 5)
- ✅ Navigation contextuelle selon les rôles
- ✅ Dashboards personnalisés
- ✅ Messages d'erreur informatifs
- ✅ Expérience utilisateur optimisée

### Administration

- ✅ Panel d'administration complet
- ✅ Gestion des utilisateurs (activation, verrouillage, suppression)
- ✅ Statistiques en temps réel
- ✅ Monitoring des comptes

## URLs Principales

### Pages Publiques
- `/` - Accueil
- `/services` - Services
- `/about` - À propos
- `/contact` - Contact
- `/login` - Connexion
- `/register` - Inscription

### Espace Utilisateur
- `/dashboard` - Tableau de bord utilisateur
- `/dashboard/orders` - Mes commandes
- `/dashboard/reviews` - Mes avis
- `/profile` - Mon profil

### Espace Administration
- `/admin/dashboard` - Tableau de bord admin
- `/admin/users` - Gestion utilisateurs
- `/admin/statistics` - Statistiques

## Instructions de Démarrage

1. **Prérequis**
   ```bash
   - Java 21+
   - MySQL 8.0+
   - Maven 3.6+
   ```

2. **Configuration Base de Données**
   ```sql
   CREATE DATABASE lmp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **Démarrage de l'Application**
   ```bash
   mvn spring-boot:run
   ```

4. **Accès à l'Application**
   - Application : http://localhost:8080
   - Login Admin : http://localhost:8080/login
   - Dashboard Admin : http://localhost:8080/admin/dashboard

## Architecture des Fichiers Créés

```
src/main/java/com/lmp/
├── config/
│   ├── SecurityConfig.java           # Configuration Spring Security
│   └── DataInitializer.java          # Données initiales
├── service/auth/
│   ├── AuthService.java              # Interface service auth
│   ├── AuthServiceImpl.java          # Implémentation service auth
│   └── CustomUserDetailsService.java # Service utilisateur Spring Security
├── service/user/
│   ├── UserService.java              # Interface service utilisateur
│   └── UserServiceImpl.java          # Implémentation service utilisateur
├── web/
│   ├── controller/
│   │   ├── auth/AuthController.java  # Contrôleur authentification
│   │   ├── user/DashboardController.java # Dashboard utilisateur
│   │   └── admin/AdminDashboardController.java # Dashboard admin
│   ├── dto/
│   │   ├── LoginDto.java             # DTO connexion
│   │   └── RegisterDto.java          # DTO inscription
│   └── advice/
│       └── GlobalExceptionHandler.java # Gestionnaire erreurs global

src/main/resources/
├── db/migration/
│   └── V2__insert_default_roles.sql # Migration rôles par défaut
├── templates/
│   ├── auth/
│   │   ├── login.html                # Page de connexion
│   │   └── register.html             # Page d'inscription
│   ├── user/
│   │   └── dashboard.html            # Dashboard utilisateur
│   ├── admin/
│   │   └── dashboard.html            # Dashboard administrateur
│   ├── error/
│   │   ├── 403.html                  # Page erreur 403
│   │   └── 500.html                  # Page erreur 500
│   └── fragments/
│       └── navigation.html           # Navigation mise à jour
└── application.properties            # Configuration mise à jour
```

## Sécurité et Bonnes Pratiques

- **Mots de passe** : Minimum 6 caractères, hashés avec BCrypt
- **Sessions** : Expiration automatique, protection contre le fixation
- **CSRF** : Protection activée pour tous les formulaires
- **XSS** : Échappement automatique des données avec Thymeleaf
- **Validation** : Validation côté serveur et client
- **Logs** : Journalisation des événements de sécurité

## Tests et Validation

Pour tester le système :

1. **Connexion Visiteur** : Accès limité aux pages publiques
2. **Connexion Utilisateur** : Accès au dashboard et fonctionnalités user
3. **Connexion Admin** : Accès total incluant le panneau d'administration
4. **Test des Erreurs** : Tentative d'accès non autorisé (403)
5. **Test de Sécurité** : Vérification des redirections et protections

Le système d'authentification et d'autorisation est maintenant entièrement fonctionnel et sécurisé pour l'application LMP !