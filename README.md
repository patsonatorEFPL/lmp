# LMP - Local Map Profil

🚀 Application Spring Boot pour l'expertise en visibilité locale et référencement digital.

## 📋 Description

LMP (Local Map Profil) est une plateforme web moderne développée avec Spring Boot qui aide les entreprises à améliorer leur visibilité locale et leur présence digitale. L'application offre une interface utilisateur élégante et responsive pour présenter les services, une carte interactive, et un système de contact intégré.

## ✨ Fonctionnalités

- 🏠 **Page d'accueil** avec présentation des services et animation flottante
- 🛠️ **Page services** détaillée avec tous les services offerts
- 🗺️ **Carte interactive** pour localiser les services
- 📧 **Formulaire de contact** avec validation
- ℹ️ **Page à propos** pour présenter l'entreprise
- 📱 **Design responsive** optimisé pour tous les appareils
- 🎨 **Navigation unifiée** avec modal de réservation
- 🌐 **Sélecteur de langue** (FR/EN préparé)

## 🛠️ Technologies utilisées

- **Backend:** Spring Boot 3.x
- **Template Engine:** Thymeleaf
- **Frontend:** HTML5, CSS3, JavaScript ES6
- **Styling:** Tailwind CSS
- **Java:** 17+
- **Build Tool:** Maven
- **Icons:** Font Awesome 6

## 📦 Prérequis

- Java 17 ou supérieur
- Maven 3.6 ou supérieur
- Un navigateur web moderne

## 🚀 Installation et lancement

1. **Clonez le repository** (une fois uploadé sur GitHub) :
```bash
git clone https://github.com/votre-username/lmp-spring-boot.git
cd lmp-spring-boot
```

2. **Compilez et lancez l'application** :
```bash
./mvnw spring-boot:run
```

3. **Ouvrez votre navigateur** et allez sur :
```
http://localhost:8080
```

## 🌐 Pages disponibles

| Route | Description |
|-------|-------------|
| `/` | Page d'accueil avec animation |
| `/services` | Services détaillés |
| `/map` | Carte interactive |
| `/about` | À propos de l'entreprise |
| `/contact` | Formulaire de contact |
| `/contact-success` | Confirmation d'envoi |

## 📱 Design responsive

L'application est entièrement responsive et optimisée pour :
- 📱 **Mobile** (320px - 768px)
- 📟 **Tablette** (768px - 1024px)
- 💻 **Desktop** (1024px+)

## 🎨 Architecture des fragments

Le projet utilise des fragments Thymeleaf pour une maintenance optimale :

```
src/main/resources/templates/fragments/
├── navigation.html      # Barre de navigation unifiée
├── footer.html         # Pied de page
├── styles.html         # Styles globaux
├── scripts.html        # Scripts JavaScript
└── booking-modal.html  # Modal de réservation
```

## 📂 Structure du projet

```
src/
├── main/
│   ├── java/com/lmp/
│   │   ├── LmpApplication.java
│   │   └── controller/
│   │       ├── HomeController.java
│   │       ├── AboutController.java
│   │       ├── ServicesController.java
│   │       ├── MapController.java
│   │       └── ContactController.java
│   └── resources/
│       ├── static/
│       │   ├── css/style.css
│       │   └── js/map.js
│       └── templates/
│           ├── index.html
│           ├── about.html
│           ├── services.html
│           ├── map.html
│           ├── contact.html
│           ├── contact-success.html
│           └── fragments/
└── test/
```

## 🚀 Déploiement

L'application peut être déployée gratuitement sur :

- **Railway** (Recommandé)
- **Render**
- **Heroku**
- **Fly.io**

### Configuration pour le déploiement

Le projet est prêt pour le déploiement avec :
- Configuration Spring Boot optimisée
- Gestion des variables d'environnement
- Profils de développement et production

## 🎯 Fonctionnalités avancées

- ✅ **Animation CSS** sur les éléments visuels
- ✅ **Modal de réservation** intégrée
- ✅ **Navigation sticky** avec ombre
- ✅ **Effets hover** sur les boutons
- ✅ **Transitions fluides**
- ✅ **Optimisation mobile**

## 👨‍💻 Développement

### Lancer en mode développement
```bash
./mvnw spring-boot:run
```

### Construire pour la production
```bash
./mvnw clean package
```

### Structure des contrôleurs
Chaque page a son propre contrôleur pour une meilleure organisation :
- Gestion des titres dynamiques
- Variables de template personnalisées
- Logique métier séparée

## 📄 License

Ce projet est sous licence privée. Tous droits réservés.

## 🔗 Contact

Pour toute question concernant ce projet, contactez l'équipe de développement.

---

*Développé avec ❤️ en utilisant Spring Boot et Thymeleaf*