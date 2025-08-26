# Diagrammes d'Architecture - Vue Administration des Commandes

## 🏗️ Architecture Système Globale

```mermaid
graph TB
    subgraph "Frontend - Interface Admin"
        UI[Interface /admin/orders]
        Filter[Filtres Avancés]
        Actions[Actions Administrateur]
        Reports[Dashboard Rapports]
        Export[Exports de Données]
    end
    
    subgraph "Controllers Layer"
        AC[AdminOrderController]
        API[AdminOrderApiController]
        RC[AdminOrderReportController]
    end
    
    subgraph "Services Layer"
        OAS[OrderAdminService]
        ONS[OrderNotificationService]
        ORS[OrderReportService]
        ORFS[OrderRefundService]
        OSM[OrderStatusManager]
    end
    
    subgraph "Infrastructure Layer"
        OR[OrderRepository]
        OSHR[OrderStatusHistoryRepository]
        PTR[PaymentTransactionRepository]
        Cache[Redis Cache]
    end
    
    subgraph "External Services"
        Stripe[Stripe API]
        Email[Service Email]
        Webhook[Stripe Webhooks]
    end
    
    subgraph "Database"
        DB[(PostgreSQL)]
    end
    
    UI --> AC
    Filter --> API
    Actions --> API
    Reports --> RC
    Export --> RC
    
    AC --> OAS
    API --> OAS
    API --> ONS
    RC --> ORS
    
    OAS --> OR
    OAS --> OSM
    ONS --> Email
    ORFS --> Stripe
    ORS --> Cache
    
    OR --> DB
    OSHR --> DB
    PTR --> DB
    
    Webhook --> OSM
    OSM --> OSHR
```

---

## 🔄 Flux de Gestion des Statuts avec Webhooks

```mermaid
sequenceDiagram
    participant Client
    participant AdminUI as Interface Admin
    participant Controller as AdminOrderController
    participant StatusMgr as OrderStatusManager
    participant Webhook as StripeWebhookHandler
    participant DB as Database
    participant Notif as NotificationService
    
    Note over StatusMgr: Flux Normal - Webhook Stripe
    Stripe->>Webhook: Événement statut
    Webhook->>StatusMgr: updateStatusFromWebhook()
    StatusMgr->>DB: Mise à jour Order
    StatusMgr->>DB: Enregistrement historique
    StatusMgr->>Notif: Notification automatique
    StatusMgr->>AdminUI: Mise à jour temps réel (WebSocket)
    
    Note over AdminUI: Flux Exceptionnel - Action Admin
    Client->>AdminUI: Action manuelle sur commande
    AdminUI->>Controller: updateOrderStatus()
    Controller->>StatusMgr: updateStatusFromAdmin(reason)
    StatusMgr->>DB: Validation + Override admin
    StatusMgr->>DB: Audit action admin
    StatusMgr->>Notif: Notification spéciale
    StatusMgr->>AdminUI: Confirmation action
```

---

## 🎯 Architecture du Système de Filtrage

```mermaid
graph LR
    subgraph "Interface Filtres"
        SF[Filtres Statut]
        DF[Filtres Date]
        UF[Filtres Utilisateur]
        MF[Filtres Montant]
        CF[Filtres Combinés]
    end
    
    subgraph "Processing"
        FP[FilterProcessor]
        QC[QueryComposer]
        Cache[Cache Requêtes]
    end
    
    subgraph "Repository"
        OR[OrderRepository]
        CQ[Custom Queries]
    end
    
    SF --> FP
    DF --> FP
    UF --> FP
    MF --> FP
    CF --> FP
    
    FP --> QC
    QC --> Cache
    Cache --> OR
    OR --> CQ
    
    CQ --> DB[(Database)]
    
    classDef filter fill:#e1f5fe
    classDef process fill:#f3e5f5
    classDef data fill:#e8f5e8
    
    class SF,DF,UF,MF,CF filter
    class FP,QC,Cache process
    class OR,CQ,DB data
```

---

## 📧 Architecture du Système de Notifications

```mermaid
graph TB
    subgraph "Triggers"
        WH[Webhooks Stripe]
        MA[Actions Manuelles Admin]
        SC[Événements Système]
    end
    
    subgraph "Notification Engine"
        NQ[Queue Notifications]
        TE[Template Engine]
        LS[Language Selector]
    end
    
    subgraph "Delivery Services"
        ES[Email Service]
        PS[Push Service]
        WS[WebSocket Service]
    end
    
    subgraph "Templates"
        ET[Email Templates]
        PT[Push Templates]
        MT[Multi-langue]
    end
    
    subgraph "Audit & Retry"
        NH[Notification History]
        RF[Retry Failed]
        AL[Audit Log]
    end
    
    WH --> NQ
    MA --> NQ
    SC --> NQ
    
    NQ --> TE
    TE --> LS
    LS --> ET
    LS --> PT
    LS --> MT
    
    TE --> ES
    TE --> PS
    TE --> WS
    
    ES --> NH
    PS --> NH
    WS --> NH
    
    NH --> RF
    NH --> AL
```

---

## 💰 Architecture du Système de Remboursements

```mermaid
flowchart TD
    Start([Demande Remboursement]) --> Validate{Validation<br/>Conditions}
    
    Validate -->|Invalide| Reject[Rejet avec Raison]
    Validate -->|Valide| CheckAmount{Montant vs<br/>Limites}
    
    CheckAmount -->|> Limite Auto| Approval[Workflow Approbation]
    CheckAmount -->|<= Limite Auto| DirectProcess[Traitement Direct]
    
    Approval --> Approved{Approuvé?}
    Approved -->|Non| Reject
    Approved -->|Oui| DirectProcess
    
    DirectProcess --> StripeCall[Appel API Stripe]
    StripeCall --> StripeResponse{Réponse Stripe}
    
    StripeResponse -->|Success| UpdateDB[Mise à jour BD]
    StripeResponse -->|Error| ErrorHandle[Gestion Erreur]
    
    UpdateDB --> CreateTransaction[Transaction Remboursement]
    CreateTransaction --> UpdateOrder[Statut Commande]
    UpdateOrder --> NotifyCustomer[Notification Client]
    NotifyCustomer --> AuditLog[Audit Complet]
    AuditLog --> End([Fin])
    
    ErrorHandle --> Retry{Retry?}
    Retry -->|Oui| StripeCall
    Retry -->|Non| ManualReview[Révision Manuelle]
    ManualReview --> End
    
    Reject --> NotifyAdmin[Notification Admin]
    NotifyAdmin --> End
```

---

## 📊 Architecture des Rapports et Analytics

```mermaid
graph TB
    subgraph "Data Sources"
        Orders[Table Orders]
        Payments[Table Payments]
        History[Status History]
        Users[User Data]
    end
    
    subgraph "Data Processing"
        ETL[ETL Process]
        Agg[Aggregation Engine]
        Calc[Calculation Service]
    end
    
    subgraph "Cache Layer"
        Redis[Redis Cache]
        Memory[Memory Cache]
    end
    
    subgraph "Report Generation"
        RG[Report Generator]
        Charts[Chart Generator]
        Export[Export Engine]
    end
    
    subgraph "Output Formats"
        Excel[Excel Files]
        PDF[PDF Reports]
        CSV[CSV Export]
        JSON[JSON API]
    end
    
    subgraph "Dashboard"
        KPI[KPI Widgets]
        Graphs[Interactive Charts]
        Tables[Data Tables]
    end
    
    Orders --> ETL
    Payments --> ETL
    History --> ETL
    Users --> ETL
    
    ETL --> Agg
    Agg --> Calc
    Calc --> Redis
    Calc --> Memory
    
    Redis --> RG
    Memory --> RG
    RG --> Charts
    RG --> Export
    
    Export --> Excel
    Export --> PDF
    Export --> CSV
    Charts --> JSON
    
    JSON --> KPI
    JSON --> Graphs
    JSON --> Tables
```

---

## 🔐 Architecture de Sécurité et Audit

```mermaid
graph TB
    subgraph "Authentication"
        Login[Admin Login]
        JWT[JWT Validation]
        Session[Session Management]
    end
    
    subgraph "Authorization"
        Roles[Role Check]
        Perms[Permission Validation]
        Actions[Action Authorization]
    end
    
    subgraph "Audit Trail"
        Capture[Action Capture]
        Context[Context Collection]
        Storage[Secure Storage]
    end
    
    subgraph "Security Monitoring"
        Detect[Anomaly Detection]
        Alert[Security Alerts]
        Response[Incident Response]
    end
    
    subgraph "Data Protection"
        Encrypt[Data Encryption]
        Mask[Data Masking]
        Purge[Data Purging]
    end
    
    Login --> JWT
    JWT --> Session
    Session --> Roles
    Roles --> Perms
    Perms --> Actions
    
    Actions --> Capture
    Capture --> Context
    Context --> Storage
    
    Storage --> Detect
    Detect --> Alert
    Alert --> Response
    
    Storage --> Encrypt
    Encrypt --> Mask
    Mask --> Purge
```

---

## 🔄 Flux de Données en Temps Réel

```mermaid
sequenceDiagram
    participant Admin as Admin Interface
    participant WS as WebSocket Service
    participant Controller as Order Controller
    participant Service as Order Service
    participant DB as Database
    participant Webhook as Stripe Webhook
    
    Note over Admin: Connexion temps réel
    Admin->>WS: Connexion WebSocket
    WS->>Admin: Confirmation connexion
    
    Note over Webhook: Événement externe
    Webhook->>Service: Mise à jour statut
    Service->>DB: Sauvegarde changement
    Service->>WS: Broadcast mise à jour
    WS->>Admin: Notification temps réel
    
    Note over Admin: Action administrateur
    Admin->>Controller: Action sur commande
    Controller->>Service: Traitement action
    Service->>DB: Mise à jour données
    Service->>WS: Broadcast changement
    WS->>Admin: Confirmation action
    
    Note over Service: Notification client
    Service->>Email: Envoi notification
    Email->>Service: Confirmation envoi
    Service->>WS: Status notification
    WS->>Admin: Mise à jour interface
```

---

## 📱 Architecture Interface Utilisateur

```mermaid
graph TB
    subgraph "Layout Principal"
        Header[Header Navigation]
        Sidebar[Sidebar Filters]
        Content[Content Area]
        Footer[Footer Actions]
    end
    
    subgraph "Content Components"
        Filters[Advanced Filters]
        Table[Orders Table]
        Pagination[Pagination Controls]
        Actions[Bulk Actions]
    end
    
    subgraph "Modal Components"
        DetailModal[Order Detail Modal]
        EditModal[Edit Order Modal]
        RefundModal[Refund Modal]
        HistoryModal[History Modal]
    end
    
    subgraph "Widgets Dashboard"
        KPIWidget[KPI Cards]
        ChartWidget[Chart Components]
        AlertWidget[Alert Notifications]
        QuickActions[Quick Actions]
    end
    
    subgraph "State Management"
        OrderState[Order State]
        FilterState[Filter State]
        UIState[UI State]
        Cache[Client Cache]
    end
    
    Header --> Content
    Sidebar --> Filters
    Content --> Table
    Content --> Pagination
    
    Table --> DetailModal
    Table --> EditModal
    Actions --> RefundModal
    
    KPIWidget --> ChartWidget
    ChartWidget --> AlertWidget
    
    Filters --> FilterState
    Table --> OrderState
    DetailModal --> UIState
    
    FilterState --> Cache
    OrderState --> Cache
    UIState --> Cache
```

---

## 🚀 Architecture de Déploiement

```mermaid
graph TB
    subgraph "Load Balancer"
        LB[Nginx Load Balancer]
    end
    
    subgraph "Application Servers"
        App1[Spring Boot App 1]
        App2[Spring Boot App 2]
        App3[Spring Boot App 3]
    end
    
    subgraph "Cache Layer"
        Redis1[Redis Master]
        Redis2[Redis Replica]
    end
    
    subgraph "Database Cluster"
        DB1[(PostgreSQL Master)]
        DB2[(PostgreSQL Replica)]
    end
    
    subgraph "External Services"
        Stripe[Stripe API]
        Email[Email Service]
        Storage[File Storage]
    end
    
    subgraph "Monitoring"
        Metrics[Prometheus]
        Logs[ELK Stack]
        APM[Application Performance]
    end
    
    LB --> App1
    LB --> App2
    LB --> App3
    
    App1 --> Redis1
    App2 --> Redis1
    App3 --> Redis1
    
    Redis1 --> Redis2
    
    App1 --> DB1
    App2 --> DB1
    App3 --> DB1
    
    DB1 --> DB2
    
    App1 --> Stripe
    App1 --> Email
    App1 --> Storage
    
    App1 --> Metrics
    App1 --> Logs
    App1 --> APM
```

---

## 📊 Métriques et Performance

```mermaid
graph LR
    subgraph "Performance Metrics"
        RT[Response Time]
        TP[Throughput]
        CPU[CPU Usage]
        MEM[Memory Usage]
    end
    
    subgraph "Business Metrics"
        Orders[Orders/Hour]
        Success[Success Rate]
        Errors[Error Rate]
        Users[Active Users]
    end
    
    subgraph "System Health"
        DB_Health[Database Health]
        Cache_Health[Cache Health]
        API_Health[API Health]
        Queue_Health[Queue Health]
    end
    
    subgraph "Alerting"
        Threshold[Threshold Monitoring]
        Notifications[Alert Notifications]
        Escalation[Escalation Rules]
    end
    
    RT --> Threshold
    TP --> Threshold
    CPU --> Threshold
    MEM --> Threshold
    
    Orders --> Threshold
    Success --> Threshold
    Errors --> Threshold
    
    DB_Health --> Notifications
    Cache_Health --> Notifications
    API_Health --> Notifications
    
    Notifications --> Escalation
    Threshold --> Notifications
```

---

Ces diagrammes illustrent l'architecture complète du système de gestion des commandes administrateur, couvrant tous les aspects techniques et fonctionnels identifiés dans le plan d'architecture.