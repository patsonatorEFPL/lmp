# Guide des Animations de Défilement LMP

## 🎬 Vue d'ensemble

Ce système d'animations de défilement fournit des animations fluides et performantes pour améliorer l'expérience utilisateur sur toutes les pages du site LMP. Les animations sont basées sur l'intersection Observer API pour des performances optimales.

## 📁 Fichiers créés

### CSS
- `src/main/resources/static/css/scroll-animations.css` - Styles et classes d'animation

### JavaScript  
- `src/main/resources/static/js/scroll-animations.js` - Logique d'animation et gestionnaire

### Template Fragment
- `src/main/resources/templates/fragments/scroll-animations.html` - Fragment Thymeleaf réutilisable

## 🚀 Implémentation actuelle

### Pages déjà configurées
- ✅ **Page d'accueil** (`index.html`) - Complètement animée
- ✅ **Page services** (`services.html`) - Partiellement animée

### Pages à configurer
- ⏳ **Page carte** (`map.html`)
- ⏳ **Page à propos** (`about.html`) 
- ⏳ **Page contact** (`contact.html`)

## 📚 Classes d'animation disponibles

### Animations de base
- `.fade-in-up` - Apparition depuis le bas
- `.fade-in-left` - Apparition depuis la gauche
- `.fade-in-right` - Apparition depuis la droite  
- `.fade-in` - Apparition simple
- `.scale-in` - Apparition avec effet de zoom
- `.slide-in-bottom` - Glissement depuis le bas

### Animations spécialisées
- `.hero-animation` - Pour les sections héro
- `.title-animation` - Pour les titres principaux
- `.card-animation` - Pour les cartes de contenu
- `.counter-animation` - Pour les compteurs numériques
- `.form-animation` - Pour les formulaires
- `.button-animation` - Pour les boutons
- `.icon-animation` - Pour les icônes avec rotation
- `.section-animation` - Pour les sections complètes

### Délais en cascade
- `.delay-100` - Délai de 0.1s
- `.delay-200` - Délai de 0.2s
- `.delay-300` - Délai de 0.3s
- `.delay-400` - Délai de 0.4s
- `.delay-500` - Délai de 0.5s

## 🛠 Comment ajouter les animations à une nouvelle page

### Étape 1: Inclure les fichiers CSS et JS

Dans le `<head>` de votre page :
```html
<!-- Animations de défilement -->
<div th:replace="fragments/scroll-animations :: scroll-animations-css"></div>
```

Avant la fermeture du `<body>` :
```html
<!-- Script des animations de défilement -->
<div th:replace="fragments/scroll-animations :: scroll-animations-js"></div>
```

### Étape 2: Ajouter les classes aux éléments HTML

#### Exemple pour un titre de section
```html
<h2 class="text-3xl font-bold title-animation">Mon Titre</h2>
```

#### Exemple pour des cartes en cascade
```html
<div class="grid grid-cols-1 md:grid-cols-3 gap-8">
    <div class="card card-animation delay-100">Carte 1</div>
    <div class="card card-animation delay-200">Carte 2</div>
    <div class="card card-animation delay-300">Carte 3</div>
</div>
```

#### Exemple pour un compteur animé
```html
<div class="counter-animation">
    <div class="text-4xl font-bold" data-target="500">500+</div>
    <div class="text-secondary">Clients satisfaits</div>
</div>
```

## 🎯 Exemples d'utilisation par type de contenu

### Section Héro
```html
<section class="hero">
    <h1 class="hero-animation">Titre Principal</h1>
    <p class="hero-animation delay-200">Description</p>
    <form class="form-animation delay-300">...</form>
    <div class="fade-in-up delay-400">Indicateurs de confiance</div>
</section>
```

### Section Services/Produits
```html
<section class="services">
    <h2 class="title-animation">Nos Services</h2>
    <p class="fade-in-up delay-200">Description</p>
    
    <div class="grid">
        <div class="service-card card-animation delay-100">Service 1</div>
        <div class="service-card card-animation delay-200">Service 2</div>
        <div class="service-card card-animation delay-300">Service 3</div>
    </div>
</section>
```

### Section Statistiques
```html
<section class="stats">
    <div class="counter-animation delay-100">
        <div data-target="500">500+</div>
        <div>Clients</div>
    </div>
    <div class="counter-animation delay-200">
        <div data-target="95">95%</div>
        <div>Satisfaction</div>
    </div>
</section>
```

## 🎨 Personnalisation

### Modifier les délais
```css
.mon-element {
    transition-delay: 0.6s; /* Délai personnalisé */
}
```

### Créer une nouvelle animation
```css
.mon-animation {
    opacity: 0;
    transform: translateX(-50px) rotate(45deg);
    transition: all 1s ease-out;
}

.mon-animation.animate {
    opacity: 1;
    transform: translateX(0) rotate(0deg);
}
```

## ♿ Accessibilité

Le système respecte automatiquement les préférences d'accessibilité :
- Détection de `prefers-reduced-motion`
- Désactivation automatique des animations si nécessaire
- Fallback gracieux pour les navigateurs non supportés

## ⚡ Performances

- Utilise l'**Intersection Observer API** pour des performances optimales
- **Lazy loading** des animations (déclenchement au scroll)
- **will-change** optimisé pour les performances GPU
- **Debouncing** des événements de scroll
- Nettoyage automatique des ressources

## 🔧 API JavaScript disponible

### Méthodes globales
```javascript
// Déclencher une animation manuellement
window.animateElement('.mon-element');

// Réinitialiser une animation
window.resetAnimation('.mon-element');

// Animation en cascade d'un conteneur
window.animateCascade('.mon-conteneur', 100);
```

### Événements
```javascript
// Écouter quand un élément a été animé
document.addEventListener('elementAnimated', function(e) {
    console.log('Élément animé:', e.detail.element);
});
```

## 🐛 Débogage

Pour activer les logs de débogage :
```javascript
// Dans la console du navigateur
localStorage.setItem('debug-animations', 'true');
```

## 📋 TODO - Implémentation complète

### Pages restantes à animer

#### Page Carte (`map.html`)
- [ ] Ajouter CSS et JS d'animation
- [ ] Animer le titre de la page
- [ ] Animer la carte interactive
- [ ] Animer les informations de contact

#### Page À Propos (`about.html`)  
- [ ] Ajouter CSS et JS d'animation
- [ ] Animer la section héro
- [ ] Animer les profils d'équipe
- [ ] Animer les valeurs/mission

#### Page Contact (`contact.html`)
- [ ] Ajouter CSS et JS d'animation  
- [ ] Animer le formulaire de contact
- [ ] Animer les coordonnées
- [ ] Animer les call-to-action

### Améliorations futures
- [ ] Animation de progression pour les barres de compétence
- [ ] Animation de typewriter pour les titres
- [ ] Animation de particles en arrière-plan
- [ ] Animation de morphing des icônes
- [ ] Integration avec la bibliothèque GSAP pour des animations avancées

## 📞 Support

Pour toute question ou problème avec les animations, vérifiez :
1. Que les fichiers CSS/JS sont bien inclus
2. Que les classes sont correctement appliquées
3. Que le JavaScript n'a pas d'erreurs dans la console
4. Que l'Intersection Observer est supporté par le navigateur

---

*Développé pour LMP - Marketing Digital Local Québec*
