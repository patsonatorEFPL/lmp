# Diagramme d'Architecture - Vues d'Administration

## 🏗️ Vue d'Ensemble de l'Architecture

```mermaid
graph TB
    subgraph "Frontend Admin"
        A[Navigation Admin] --> B[/admin/dashboard]
        A --> C[/admin/users]
        A --> D[/admin/settings]
        
        B --> B1[AdminDashboardController]
        C --> C1[AdminUserViewController]
        D --> D1[SystemSettingsController]
    end
    
    subgraph "Controllers Layer"
        B1 --> B2[dashboard.html]
        C1 --> C2[users.html]
        D1 --> D2[settings.html]
        
        C1 --> C3[UserService]
        D1 --> D3[SystemConfigService]
    end
    
    subgraph "Service Layer"
        C3 --> C4[UserRepository]
        D3 --> D4[application.properties]
        
        C3 --> C5[User Management Logic]
        D3 --> D5[Config Management Logic]
    end
    
    subgraph "Data Layer"
        C4 --> C6[MySQL Database]
        D4 --> D7[Configuration Files]
        
        C6 --> C8[users table]
        C6 --> C9[user_roles table]
    end
    
    subgraph "Security Layer"
        S1[Spring Security] --> S2[@PreAuthorize ADMIN]
        S2 --> B1
        S2 --> C1
        S2 --> D1
    end
```

## 🔄 Flux de Données - Gestion Utilisateurs

```mermaid
sequenceDiagram
    participant U as Admin User
    participant N as Navigation
    participant C as AdminUserViewController
    participant S as UserService
    participant R as UserRepository
    participant D as Database
    
    U->>N: Click "Gestion utilisateurs"
    N->>C: GET /admin/users
    C->>S: findAllWithFilters(status, search, page)
    S->>R: findByStatusAndEmailContaining()
    R->>D: SELECT users query
    D->>R: User list with pagination
    R->>S: Page<User> results
    S->>C: UserInfo DTOs
    C->>N: users.html with data
    N->>U: Display user management interface
    
    Note over U,D: User Action: Lock Account
    U->>C: POST /admin/users/{id}/lock
    C->>S: lockUserAccount(userId)
    S->>R: user.setAccountLocked(true)
    R->>D: UPDATE users SET account_locked=true
    D->>R: Success confirmation
    R->>S: Updated user
    S->>C: Success response
    C->>U: JSON response + UI update
```

## 🛠️ Flux de Données - Paramètres Système

```mermaid
sequenceDiagram
    participant A as Admin User
    participant C as SystemSettingsController
    participant S as SystemConfigService
    participant P as Properties Manager
    participant F as application.properties
    
    A->>C: GET /admin/settings
    C->>S: loadAllSettings()
    S->>P: getProperty(key) for each setting
    P->>F: Read configuration values
    F->>P: Property values
    P->>S: Configuration map
    S->>C: SystemSettingsDto
    C->>A: settings.html with current values
    
    Note over A,F: Admin Updates Settings
    A->>C: POST /admin/settings
    C->>S: updateSettings(settingsDto)
    S->>P: validateAndUpdateProperties()
    P->>F: Write new values
    F->>P: Confirmation
    P->>S: Success
    S->>C: Updated settings
    C->>A: Success response + reload
```

## 📁 Structure des Fichiers

```
src/main/java/com/lmp/
├── web/controller/admin/
│   ├── AdminDashboardController.java     ✅ Existant
│   ├── UserManagementController.java     ✅ Existant (API REST)
│   ├── AdminUserViewController.java      🆕 Vue Gestion Users
│   └── SystemSettingsController.java     🆕 Vue Paramètres
├── service/
│   ├── user/UserService.java            ✅ Existant
│   └── system/SystemConfigService.java   🆕 Gestion Config
├── web/dto/admin/
│   ├── UserActionDto.java                🆕 Actions utilisateur
│   └── SystemSettingsDto.java           🆕 Paramètres système
└── domain/entity/
    └── User.java                         ✅ Existant

src/main/resources/templates/admin/
├── dashboard.html                        ✅ Existant
├── users.html                           🆕 Interface Users
├── settings.html                        🆕 Interface Settings
└── fragments/
    ├── admin-layout.html                🆕 Layout commun
    ├── user-table.html                  🆕 Tableau users
    ├── user-actions.html                🆕 Actions users
    └── settings-form.html               🆕 Formulaire config
```

## 🎨 Architecture des Composants UI

```mermaid
graph TD
    subgraph "Page /admin/users"
        U1[Header Section] --> U2[Search & Filters Bar]
        U2 --> U3[Users Data Table]
        U3 --> U4[Pagination Controls]
        U4 --> U5[Action Modals]
        
        U3 --> U6[User Row Component]
        U6 --> U7[Avatar + Name]
        U6 --> U8[Status Badge]  
        U6 --> U9[Action Buttons]
        
        U5 --> U10[Confirm Lock Modal]
        U5 --> U11[User Details Modal]
        U5 --> U12[Bulk Actions Modal]
    end
    
    subgraph "Page /admin/settings"
        S1[Settings Tabs Navigation] --> S2[Application Tab]
        S1 --> S3[Company Tab] 
        S1 --> S4[Payment Tab]
        S1 --> S5[Email Tab]
        S1 --> S6[Security Tab]
        
        S2 --> S7[App Config Form]
        S3 --> S8[Company Info Form]
        S4 --> S9[Stripe Config Form]
        S5 --> S10[SMTP Config Form]
        S6 --> S11[Security Policies Form]
        
        S7 --> S12[Save/Reset Actions]
        S8 --> S12
        S9 --> S12
        S10 --> S12
        S11 --> S12
    end
```

## 🔒 Modèle de Sécurité

```mermaid
graph LR
    subgraph "Security Flow"
        A[Request] --> B[Spring Security Filter]
        B --> C{User Authenticated?}
        C -->|No| D[Redirect to Login]
        C -->|Yes| E{Has ADMIN Role?}
        E -->|No| F[403 Forbidden]
        E -->|Yes| G[Controller Access Granted]
        
        G --> H[@PreAuthorize Check]
        H --> I[Method Execution]
        I --> J[Response]
    end
    
    subgraph "Admin Endpoints"
        G --> K[AdminDashboardController]
        G --> L[AdminUserViewController] 
        G --> M[SystemSettingsController]
        
        L --> N[User Management Actions]
        M --> O[System Configuration]
        
        N --> P[Audit Logging]
        O --> P
    end
```

## 📊 Modèle de Données

```mermaid
erDiagram
    User {
        bigint id PK
        string email UK
        string password
        string first_name
        string last_name
        string phone
        datetime registration_date
        datetime last_login_date
        enum status "ACTIVE,INACTIVE,DELETED"
        boolean account_locked
        boolean email_verified
    }
    
    Role {
        bigint id PK
        string name UK
        string description
    }
    
    UserRole {
        bigint user_id FK
        bigint role_id FK
    }
    
    SystemConfig {
        string config_key PK
        string config_value
        string config_type
        datetime last_modified
        string modified_by
    }
    
    User ||--o{ UserRole : has
    Role ||--o{ UserRole : assigned_to
    User ||--o{ SystemConfig : modified_by
```

## 🎯 Points d'Intégration

### 1. Navigation Existante
```html
<!-- Dans admin/dashboard.html -->
<li class="nav-item">
    <a class="nav-link" th:href="@{/admin/users}">
        <i class="fas fa-users me-1"></i>
        Utilisateurs
    </a>
</li>
<li class="nav-item">  
    <a class="nav-link" th:href="@{/admin/settings}">
        <i class="fas fa-cogs me-1"></i>
        Paramètres
    </a>
</li>
```

### 2. API REST Existante
```java
// Déjà disponible dans UserManagementController
@GetMapping("/api/admin/users")
public ResponseEntity<List<UserInfo>> getAllUsers()

// À étendre pour actions CRUD
@PostMapping("/api/admin/users/{id}/lock")
@PostMapping("/api/admin/users/{id}/unlock")
@PostMapping("/api/admin/users/{id}/activate")
```

### 3. Services Existants
```java
// UserService déjà disponible
@Autowired
private UserService userService;

// Nouvelles méthodes à ajouter
public void lockUser(Long userId);
public void unlockUser(Long userId);
public Page<User> findUsersWithFilters(UserFilter filter, Pageable pageable);
```

## 📋 Check-list d'Implémentation

### Phase 1: Infrastructure
- [ ] Créer `AdminUserViewController.java`
- [ ] Créer `SystemSettingsController.java`  
- [ ] Créer `SystemConfigService.java`
- [ ] Créer DTOs pour actions et paramètres

### Phase 2: Vues HTML
- [ ] Développer `users.html` avec tableau et filtres
- [ ] Développer `settings.html` avec onglets et formulaires
- [ ] Créer fragments réutilisables
- [ ] Intégrer avec layout existant

### Phase 3: Fonctionnalités
- [ ] Implémenter actions utilisateur (lock/unlock/activate)
- [ ] Implémenter gestion des paramètres système
- [ ] Ajouter validation et gestion d'erreurs
- [ ] Intégrer avec sécurité Spring

### Phase 4: UX/UI
- [ ] Actions AJAX pour réactivité
- [ ] Feedback utilisateur (toasts/alerts)
- [ ] Pagination et recherche
- [ ] Responsive design

### Phase 5: Tests & Validation
- [ ] Tests unitaires des controllers
- [ ] Tests d'intégration des vues
- [ ] Tests de sécurité
- [ ] Validation manuelle interface

Cette architecture s'appuie sur l'infrastructure existante tout en ajoutant les fonctionnalités demandées avec une approche modulaire et maintenable.