# Diagramme de Flux - Architecture Unifiée d'Achat

## 🔄 Flux d'Achat Unifié

```mermaid
flowchart TD
    A[Utilisateur clique sur 'Acheter'] --> B{Vérification AUTH_INFO}
    
    B -->|Utilisateur connecté| C[handleAuthenticatedUserOrder]
    B -->|Utilisateur non connecté| D[savePurchaseIntentAndShowModal]
    
    C --> C1[Créer commande temporaire via /api/orders/create-temp]
    C1 --> C2[Créer session Stripe via /stripe/checkout/create-session/orderId]
    C2 --> C3[Redirection vers Stripe Checkout]
    
    D --> D1[Sauvegarder intention via /api/orders/save-purchase-intent]
    D1 --> D2[Afficher Modal d'inscription]
    D2 --> D3[Utilisateur remplit formulaire]
    D3 --> D4[Soumission via /register]
    D4 --> D5[Création compte + Auto-login]
    D5 --> D6[AuthenticationSuccessHandler traite l'intention]
    D6 --> D7[Redirection vers paiement Stripe]
    
    C3 --> E[Paiement Stripe]
    D7 --> E
    E --> F[Succès/Échec du paiement]
    F --> G[Retour sur le site]
```

## 🏗️ Architecture des Composants

```mermaid
graph TB
    subgraph "Pages Web"
        HP[index.html - Page d'accueil]
        SP[services.html - Page services]
    end
    
    subgraph "Composants Partagés"
        BM[fragments/booking-modal.html]
        RC[register-checkout.js]
        CSRF[Métadonnées CSRF]
        AUTH[Window.AUTH_INFO]
    end
    
    subgraph "APIs Backend"
        REG[/register - Inscription]
        TEMP[/api/orders/create-temp]
        INTENT[/api/orders/save-purchase-intent]
        STRIPE[/stripe/checkout/create-session]
    end
    
    HP --> BM
    SP --> BM
    HP --> RC
    SP --> RC
    HP --> CSRF
    SP --> CSRF
    HP --> AUTH
    SP --> AUTH
    
    RC --> REG
    RC --> TEMP
    RC --> INTENT
    RC --> STRIPE
```

## 🔐 Flux de Sécurité CSRF

```mermaid
sequenceDiagram
    participant U as Utilisateur
    participant P as Page (index.html)
    participant J as JavaScript
    participant S as Serveur
    
    Note over P: Page chargée avec métadonnées CSRF
    P->>J: <meta name="_csrf" content="token">
    P->>J: <meta name="_csrf_header" content="X-CSRF-TOKEN">
    
    U->>J: Clique sur "Acheter"
    J->>J: Récupère token CSRF depuis métadonnées
    J->>S: Requête avec header X-CSRF-TOKEN
    S->>S: Validation token CSRF
    S->>J: Réponse sécurisée
```

## 🎯 États et Transitions

```mermaid
stateDiagram-v2
    [*] --> PageChargee
    PageChargee --> VerificationAuth : Clic Acheter
    
    VerificationAuth --> UtilisateurConnecte : AUTH_INFO.isAuthenticated = true
    VerificationAuth --> UtilisateurNonConnecte : AUTH_INFO.isAuthenticated = false
    
    UtilisateurConnecte --> CreationCommande : Appel API direct
    CreationCommande --> SessionStripe : Commande créée
    SessionStripe --> PaiementStripe : Redirection
    
    UtilisateurNonConnecte --> SauvegardeIntention : Intention stockée
    SauvegardeIntention --> ModalInscription : Modal affiché
    ModalInscription --> Inscription : Formulaire soumis
    Inscription --> AutoLogin : Compte créé
    AutoLogin --> TraitementIntention : AuthSuccessHandler
    TraitementIntention --> PaiementStripe : Redirection
    
    PaiementStripe --> [*] : Retour site
```

## 📱 Composants Unifiés

### Avant l'Unification
```mermaid
graph LR
    subgraph "Page d'accueil"
        A1[selectService] --> A2[Modal personnalisé]
        A2 --> A3[JavaScript inline]
        A3 --> A4[API non sécurisée]
    end
    
    subgraph "Page services"
        B1[openBookingModal] --> B2[Fragment modal]
        B2 --> B3[register-checkout.js]
        B3 --> B4[API sécurisée]
    end
```

### Après l'Unification
```mermaid
graph LR
    subgraph "Toutes les pages"
        C1[openBookingModal] --> C2[Fragment modal unifié]
        C2 --> C3[register-checkout.js]
        C3 --> C4[API sécurisée avec CSRF]
    end
```

## 🧪 Scénarios de Test

### Test 1: Utilisateur Connecté
```mermaid
sequenceDiagram
    participant U as Utilisateur Connecté
    participant P as Page
    participant A as API
    participant S as Stripe
    
    U->>P: Clique "Acheter"
    P->>P: Vérifie AUTH_INFO.isAuthenticated = true
    P->>A: POST /api/orders/create-temp (avec CSRF)
    A->>P: Retour orderId
    P->>A: POST /stripe/checkout/create-session/orderId
    A->>P: Retour redirectUrl
    P->>S: Redirection vers Stripe Checkout
```

### Test 2: Utilisateur Non Connecté
```mermaid
sequenceDiagram
    participant U as Utilisateur Non Connecté
    participant P as Page
    participant M as Modal
    participant A as API
    participant S as Stripe
    
    U->>P: Clique "Acheter"
    P->>P: Vérifie AUTH_INFO.isAuthenticated = false
    P->>A: POST /api/orders/save-purchase-intent
    P->>M: Affiche modal d'inscription
    U->>M: Remplit formulaire
    M->>A: POST /register (avec CSRF)
    A->>A: Crée compte + Auto-login
    A->>A: AuthSuccessHandler traite intention
    A->>S: Redirection vers paiement
```

## 🎨 Avantages Visuels de l'Unification

### Cohérence Interface
```mermaid
graph TD
    A[Même Modal] --> B[Même Formulaire]
    B --> C[Même Validation]
    C --> D[Même UX]
    D --> E[Expérience Uniforme]
```

### Architecture Technique
```mermaid
graph TD
    A[Code Réutilisable] --> B[Maintenance Simplifiée]
    B --> C[Sécurité Renforcée]
    C --> D[Performance Optimisée]
    D --> E[Qualité Améliorée]
```

## 📋 Points de Validation

### Sécurité ✅
- [ ] Tokens CSRF présents sur toutes les pages
- [ ] Validation AUTH_INFO fonctionnelle
- [ ] Protection contre les attaques CSRF
- [ ] Gestion sécurisée des sessions

### Fonctionnalité ✅
- [ ] Flux identique page d'accueil / services
- [ ] Modal unifié fonctionne partout
- [ ] Gestion d'erreurs cohérente
- [ ] Redirections correctes

### Performance ✅
- [ ] Script externalisé chargé une fois
- [ ] Fragments réutilisés efficacement
- [ ] Pas de duplication de code
- [ ] Optimisation des requêtes

### UX/UI ✅
- [ ] Interface cohérente entre pages
- [ ] Comportement prévisible
- [ ] Messages d'erreur uniformes
- [ ] Transitions fluides

Cette architecture unifiée garantit une expérience d'achat cohérente, sécurisée et maintenir sur l'ensemble du site LMP.