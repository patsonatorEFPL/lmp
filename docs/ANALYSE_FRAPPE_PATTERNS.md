# Analyse : Patterns Frappe / ERPNext applicables à LMP

> Étude comparative entre l'architecture Frappe (Python/Vue) et l'architecture LMP (Spring Boot/Angular)

---

## 1. Vue d'ensemble de Frappe

Frappe est un **framework full-stack monolithique mais modulaire** qui alimente ERPNext et Frappe CRM.

### Stack technique
| Couche | Technologie |
|--------|-------------|
| Backend | Python + Werkzeug/WSGI + MariaDB/PostgreSQL |
| ORM | Frappe ORM (custom, proche de Django) |
| Frontend | Vue 3 + Vite + Pinia + Tailwind |
| UI Library | `frappe-ui` (composants Vue spécifiques Frappe) |
| Real-time | Socket.io |
| Auth | Session-based (cookie `user_id`) |
| API | REST auto-générée + RPC whiteliste |

---

## 2. Le concept central : DocType

Un **DocType** = Modèle de données + UI + API + Permissions dans un seul artefact.

### Structure d'un DocType (ex: CRM Lead)
```
crm/fcrm/doctype/crm_lead/
├── crm_lead.json      ← Schéma complet (champs, types, validation, onglets, permissions)
├── crm_lead.py        ← Controller Python (lifecycle hooks, business logic)
├── crm_lead.js        ← Client-side behavior (desk UI)
└── test_crm_lead.py   ← Tests
```

### Le JSON Schema (`crm_lead.json`)
Déclare exhaustivement :
- **Champs** : `fieldname`, `fieldtype` (Data, Link, Currency, Table...), `label`, `reqd`, `options`
- **Layout** : `field_order`, onglets, sections, colonnes
- **Métadonnées** : `autoname`, `allow_import`, `email_append_to`
- **Permissions** (peut être défini ici ou dans le code)

Le framework génère **automatiquement** :
- La table SQL avec migrations
- Les formulaires d'administration
- Les listes filtrables
- L'API REST complète (`/api/resource/CRM Lead/{name}`)
- La documentation

### Le Controller Python (`crm_lead.py`)
```python
class CRMLead(Document):
    def before_validate(self):
        self.set_sla()
    
    def validate(self):
        self.validate_status()
        self.set_full_name()
        self.set_lead_name()
        if self.has_value_changed("status"):
            add_status_change_log(self)
    
    def after_insert(self):
        if self.lead_owner:
            self.assign_agent(self.lead_owner)
```

**Pattern : Template Method** avec hooks de cycle de vie standardisés :
- `before_validate` → `validate` → `before_save` → `on_update` → `after_insert`

---

## 3. hooks.py : Le registre d'événements centralisé

```python
# hooks.py = Event Bus + Plugin Registry
doc_events = {
    "Contact": {
        "validate": ["crm.api.contact.validate"],
    },
    "ToDo": {
        "after_insert": ["crm.api.todo.after_insert"],
        "on_update": ["crm.api.todo.on_update"],
    },
    "CRM Deal": {
        "on_update": [
            "crm.fcrm.doctype.erpnext_crm_settings.create_customer_in_erpnext"
        ],
    },
}

scheduler_events = {
    "all": ["crm.api.event.trigger_offset_event_notifications"],
    "hourly": ["crm.api.event.trigger_hourly_event_notifications"],
    "daily": ["crm.api.event.trigger_daily_event_notifications"],
    "cron": {
        "*/5 * * * *": ["crm.lead_syncing.background_sync.sync_leads_from_sources_5_minutes"],
    },
}

override_doctype_class = {
    "Contact": "crm.overrides.contact.CustomContact",
    "Email Template": "crm.overrides.email_template.CustomEmailTemplate",
}
```

**Intérêt** : Sans modifier le code source d'un DocType externe, on peut :
- Hooker sur ses événements
- Remplacer sa classe controller
- Ajouter des champs custom (`create_custom_fields`)
- Modifier des propriétés (`Property Setter`)

---

## 4. API : Pas de controllers séparés

### Pattern Whitelist
Les méthodes API ne sont pas dans des controllers `@RestController` séparés. Elles sont :
- **Sur les documents** eux-mêmes (méthodes d'instance)
- **Ou en fonctions utilitaires** décorées avec `@frappe.whitelist()`

```python
@frappe.whitelist()
def get_activities(name: str):
    if frappe.db.exists("CRM Deal", name):
        return get_deal_activities(name)
    elif frappe.db.exists("CRM Lead", name):
        return get_lead_activities(name)

@frappe.whitelist()
def get_users():
    # Récupère les users avec leurs rôles
    ...
```

L'URL devient simplement : `/api/method/crm.api.activities.get_activities`

### API auto-générée pour le CRUD
Pour tout DocType, Frappe expose automatiquement :
- `GET /api/resource/DocTypeName` → Liste
- `GET /api/resource/DocTypeName/{name}` → Lecture
- `POST /api/resource/DocTypeName` → Création
- `PUT /api/resource/DocTypeName/{name}` → Mise à jour
- `DELETE /api/resource/DocTypeName/{name}` → Suppression

Avec filtres, pagination, tri, champs à inclure/exclure — le tout via query params.

---

## 5. Frontend : Resource-centric + Meta-driven

### Architecture
- **Vue 3** avec Composition API (`<script setup>`)
- **Pinia** pour l'état global (session, users, views, notifications)
- **frappe-ui** : librairie de composants + primitives de data fetching
- **Tailwind CSS** + design tokens custom

### Primitives de data fetching (frappe-ui)
Au lieu de `fetch`/`axios`, ils utilisent des **ressources réactives** :

```javascript
// Ressource simple
const users = createResource({
  url: 'crm.api.session.get_users',
  cache: 'users',
  auto: true,  // fetch automatiquement au mount
})
// → users.data, users.loading, users.error, users.reload()

// Document CRUD complet
const lead = createDocumentResource({
  doctype: 'CRM Lead',
  name: leadId,
})
// → lead.doc, lead.setValue(), lead.save(), lead.submit()

// Liste avec filtres
const leads = createListResource({
  doctype: 'CRM Lead',
  fields: ['name', 'lead_name', 'status', 'organization'],
  filters: { converted: 0 },
})
```

### Session & Auth
```javascript
// Session store (Pinia)
const sessionStore = defineStore('crm-session', () => {
  function sessionUser() {
    let cookies = new URLSearchParams(document.cookie.split('; ').join('&'))
    let user = cookies.get('user_id')
    return user === 'Guest' ? null : user
  }
  
  const user = ref(sessionUser())
  const isLoggedIn = computed(() => !!user.value)
  
  const login = createResource({
    url: 'login',
    onSuccess() { user.value = sessionUser(); router.replace('/') }
  })
  
  const logout = createResource({
    url: 'logout',
    onSuccess() { user.value = null; window.location.href = '/login' }
  })
})
```

**Point clé** : Cookie-based session (`user_id`), pas de JWT. CORS géré au niveau du serveur.

### Cache + Real-time invalidation
- Les ressources ont une clé de `cache`
- Socket.io écoute `refetch_resource` pour invalider automatiquement
- Le serveur peut pousser un événement : `refetch_resource: ['users']` → tous les clients rechargent `users`

### Meta-driven UI
Les formulaires ne sont pas codés en dur. Ils sont générés à partir des métadonnées du DocType :
```javascript
const meta = getMeta('CRM Lead')  // Récupère le schéma JSON
// → Génère les champs, sections, validations, options dynamiquement
```

Les layouts (Quick Entry, Side Panel, Data Fields) sont stockés en JSON dans la DB et chargés dynamiquement.

---

## 6. ERPNext : Héritage avancé + Status déclaratif

### Chaîne d'héritage des controllers
```
Document (Frappe)
  └── StatusUpdater          # Calcul de statut cross-doc
        └── TransactionBase  # Validation temps, UOM, refs
              └── AccountsController  # Écritures comptables, taxes
                    └── StockController  # Stock ledger, serial/batch
                          └── SellingController
                                └── SalesInvoice
```

Chaque couche ajoute son `validate()` qui appelle `super().validate()` puis ajoute sa logique.

### Status déclaratif (`status_map`)
```python
status_map = {
    "Sales Order": [
        ["Draft", None],
        ["To Deliver and Bill", "eval:self.per_delivered < 100 and self.per_billed < 100"],
        ["Completed", "eval:self.per_delivered >= 100 and self.per_billed >= 100"],
        ["Closed", "eval:self.status == 'Closed'"],
    ]
}
```

Le statut n'est pas un simple enum — il est **calculé** à partir de l'état du document et de documents liés.

### Cross-document updates déclaratifs
```python
self.status_updater = [{
    "source_dt": "Sales Invoice Item",
    "target_dt": "Sales Order Item",
    "target_field": "billed_amt",
    "target_ref_field": "amount",
    "target_parent_field": "per_billed",
    "join_field": "so_detail",
}]
```

Quand une facture est validée, elle met à jour automatiquement le montant facturé et le % facturé de la commande — tout est configuré, pas codé en dur.

---

## 7. Installation & Extensibilité

### Bootstrap de données (`install.py`)
```python
def after_install(force=False):
    add_default_lead_statuses()
    add_default_deal_statuses()
    add_default_fields_layout(force)
    add_email_template_custom_fields()
    add_email_account_custom_field()
    create_default_manager_dashboard(force)
    frappe.db.commit()
```

### Extension sans modification (Monkey-patching propre)
```python
# Ajouter des champs à un DocType existant
create_custom_fields({
    "Email Template": [
        {
            "fieldname": "enabled",
            "fieldtype": "Check",
            "label": "Enabled",
        },
        {
            "fieldname": "reference_doctype",
            "fieldtype": "Link",
            "label": "Doctype",
            "options": "DocType",
        },
    ]
})

# Modifier une propriété d'un champ existant
Property Setter:
    doc_type = "Assignment Rule"
    field_name = "assign_condition"
    property = "depends_on"
    value = "eval: !doc.assign_condition_json"
```

---

## 8. Patterns applicables à LMP (Spring Boot + Angular)

### A. Backend (Spring Boot)

| Pattern Frappe | Inspiration pour LMP | Priorité |
|----------------|----------------------|----------|
| **DocType JSON Schema** | Définir un schéma JSON pour chaque entité métier (champs, types, validation, layout). Générer OpenAPI plus riche ou formulaires Angular dynamiques. | ⭐⭐⭐ |
| **Controller lifecycle hooks** | Standardiser des `@EntityListeners` ou `AbstractEntity` avec `onCreate()`, `onUpdate()`, `onValidate()`. Services template method. | ⭐⭐⭐ |
| **hooks.py event registry** | Centraliser les `@EventListener` dans une config. Ex: `EntityEventConfig` qui mappe `Order.validate` → `[StockValidator, EmailNotifier]`. | ⭐⭐ |
| **@frappe.whitelist()** | Exposer des méthodes de service directement comme endpoints REST. Ou utiliser `@Service` + `@RestController` léger qui délègue. | ⭐⭐ |
| **Status déclaratif** | `StatusComputer` bean avec SpEL ou Java predicates configurables en DB. Ex: `Order.status = fn( paymentStatus, deliveryStatus )`. | ⭐⭐⭐ |
| **Cross-doc updates déclaratifs** | `DocumentLinkUpdater` qui lit une table/config YAML pour savoir comment mettre à jour les entités liées. | ⭐⭐ |
| **Company row-level scope** | `@FilterDef` Hibernate ou `TenantContext` avec `ThreadLocal` pour injecter `company_id` dans chaque requête. | ⭐⭐ |
| **install.py bootstrap** | `DataInitializer` beans avec `@EventListener(ApplicationReadyEvent)` pour créer les données par défaut. | ⭐⭐⭐ |
| **Custom Fields / Property Setter** | Entity `CustomField` + `@MappedSuperclass` avec `Map<String, Object> customFields` persisté en JSONB. | ⭐⭐ |
| **Scheduler centralisé** | `@Scheduled` + une table `ScheduledTask` pour activer/désactiver/configurer sans redémarrer. | ⭐⭐ |

### B. Frontend (Angular)

| Pattern Frappe | Inspiration pour LMP | Priorité |
|----------------|----------------------|----------|
| **createResource** | Service générique `ResourceService<T>` avec Signals/RxJS retournant `{ data, loading, error, reload }`. | ⭐⭐⭐ |
| **createDocumentResource** | `DocumentService<T>` avec cache par `entityType+id`, méthodes `.save()`, `.patch()`, `.reload()`. | ⭐⭐⭐ |
| **createListResource** | `ListResourceService<T>` avec filtres, pagination, tri réactifs. | ⭐⭐⭐ |
| **Session cookie-based** | On est déjà en session-based (`JSESSIONID`) ✅. S'assurer que le frontend lit bien le cookie pour savoir si authentifié. | ✅ |
| **Socket.io cache invalidation** | WebSocket/STOMP pour invalider le cache Angular. Ex: `stomp.subscribe('/topic/order-updates', ...)` → recharger la ressource concernée. | ⭐⭐ |
| **Meta-driven forms** | Générer des formulaires Angular à partir de métadonnées d'entité (OpenAPI ou custom). Composant `<dynamic-form [schema]="entitySchema">`. | ⭐⭐ |
| **Layout JSON store** | Stocker les layouts (Quick Entry, Side Panel) en JSON dans la DB. Charger dynamiquement selon le profil utilisateur. | ⭐⭐ |
| **frappe-ui components** | Créer une librairie de composants LMP réutilisables (Button, Input, FormControl, Dialog, Badge, Avatar...). | ⭐⭐⭐ |
| **View state in URL** | Encore plus d'état dans l'URL : `/orders?view=kanban&filter=status:pending`. Angular Resolver pour résoudre la vue par défaut. | ⭐⭐ |
| **Responsive route switching** | Layout component qui swappe les routes enfants selon `BreakpointObserver`. | ⭐⭐ |

---

## 9. Idées concrètes à implémenter

### 9.1 Service `DocumentResource` Angular (inspiré de `createDocumentResource`)
```typescript
// document.service.ts
export class DocumentService<T> {
  private cache = new Map<string, BehaviorSubject<T | null>>();
  
  getDocument(type: string, id: string): Observable<T> {
    const key = `${type}:${id}`;
    if (!this.cache.has(key)) {
      this.cache.set(key, new BehaviorSubject<T | null>(null));
      this.load(type, id, key);
    }
    return this.cache.get(key)!.asObservable();
  }
  
  async save(type: string, id: string, data: Partial<T>): Promise<T> {
    const result = await this.http.put<T>(`/api/${type}/${id}`, data).toPromise();
    this.invalidate(key);
    return result;
  }
}
```

### 9.2 `EntityLifecycle` + Spring Events (inspiré de `hooks.py`)
```java
@EntityListeners(EntityLifecycleListener.class)
public class Order extends AbstractDocument { ... }

@Component
public class EntityLifecycleListener {
    @PrePersist @PreUpdate
    public void onValidate(Object entity) { 
        eventPublisher.publishEvent(new EntityValidateEvent(entity)); 
    }
}

@Configuration
public class EntityEventConfig {
    @EventListener(condition = "#event.entityType == 'Order'")
    public void onOrderValidate(EntityValidateEvent event) {
        // Validation stock, email, etc.
    }
}
```

### 9.3 Status déclaratif (inspiré de `status_map`)
```java
@Component
public class StatusComputer {
    @Autowired private StatusRuleRepository rules;
    
    public String computeStatus(String entityType, Object entity) {
        List<StatusRule> rules = rules.findByEntityType(entityType);
        for (StatusRule rule : rules) {
            if (spelEvaluator.evaluate(rule.getCondition(), entity)) {
                return rule.getStatus();
            }
        }
        return "Draft";
    }
}
```

### 9.4 Layout dynamique pour formulaires (inspiré de Frappe CRM)
```typescript
// Layout chargé depuis le backend
interface FieldLayout {
  sections: Section[];
}

interface Section {
  label: string;
  columns: Column[];
  opened?: boolean;
}

interface Column {
  fields: string[];  // Références aux champs du schéma
}

// Backend stocke ça en JSON
// Frontend génère : <app-dynamic-form [layout]="layout" [schema]="schema">
```

---

## 10. Ce qu'on ne devrait PAS copier

| Anti-pattern Frappe | Pourquoi éviter |
|---------------------|-----------------|
| **Monolithisme total** | Frappe est un monolithe Python gigantesque. Notre modulith Spring Boot est plus maintenable. |
| **Python ORM custom** | Spring Data JPA est mature et standard. Pas besoin de réinventer. |
| **Monkey-patching** | Les overrides de classe en runtime (`override_doctype_class`) sont fragiles. Utiliser Spring DI + `@Primary` ou Strategy pattern. |
| **eval() de code Python** | Les expressions `eval:self.per_delivered >= 100` sont dangereuses. Remplacer par SpEL sandboxé ou règles Java configurables. |
| **JSON Schema comme source de vérité** | Bien pour la flexibilité, mais perte de typage fort. Hybride : JPA entities + JSON metadata pour UI. |

---

## 11. Synthèse — Top 5 actions prioritaires

1. **Service `DocumentService` Angular** avec cache intelligent et invalidation WebSocket
2. **Standardiser les lifecycle hooks** backend (`@EntityListeners` + Spring Events centralisés)
3. **Status déclaratif** : table `StatusRule` + SpEL pour calculer les statuts métier
4. **Librairie de composants LMP** : uniformiser les boutons, inputs, dialogs, badges
5. **Layouts de formulaires dynamiques** : stocker en JSON, générer les formulaires Angular

---

## Références
- Frappe Framework : https://github.com/frappe/frappe
- Frappe CRM : https://github.com/frappe/crm
- ERPNext : https://github.com/frappe/erpnext
- Frappe UI : https://github.com/frappe/frappe-ui
