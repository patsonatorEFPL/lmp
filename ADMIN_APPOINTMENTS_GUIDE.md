# Guide d'Administration des Rendez-vous - LMP

## Vue d'ensemble

Le module de gestion des rendez-vous permet aux administrateurs de gérer complètement le cycle de vie des rendez-vous clients avec toutes les opérations CRUD et des fonctionnalités avancées.

## Fonctionnalités

### 🎯 Fonctionnalités principales

- **Gestion complète CRUD** : Créer, lire, modifier, supprimer des rendez-vous
- **Workflow de statuts** : Gestion des transitions de statuts avec validation
- **Filtrage avancé** : Par statut, date, client, mot-clé
- **Export de données** : Export CSV avec filtres appliqués
- **Notifications automatiques** : Emails aux clients selon les actions
- **Interface responsive** : Optimisée pour desktop et mobile

### 📊 Statuts des rendez-vous

| Statut | Description | Actions possibles |
|--------|-------------|-------------------|
| `PENDING` | En attente de confirmation | Confirmer, Annuler |
| `CONFIRMED` | Confirmé par l'équipe | Démarrer, Marquer absence, Annuler |
| `IN_PROGRESS` | En cours de réalisation | Terminer |
| `COMPLETED` | Terminé avec succès | *(Aucune action)* |
| `CANCELLED` | Annulé | *(Aucune action)* |
| `NO_SHOW` | Client absent | *(Aucune action)* |

## Guide d'utilisation

### 🚪 Accès à l'interface

1. Connectez-vous avec un compte **ADMIN**
2. Accédez via :
   - Menu navigation : **Admin Panel** → **Rendez-vous**
   - URL directe : `/admin/appointments`
   - Dashboard admin : Bouton **Gérer les rendez-vous**

### 📋 Liste des rendez-vous

#### Statistiques en temps réel
- **Cartes colorées** affichant le nombre de rendez-vous par statut
- **Mise à jour automatique** toutes les minutes

#### Filtres disponibles
- **Statut** : Filtrer par statut spécifique
- **Période** : Date de début et fin
- **Recherche** : Par nom client, email ou sujet
- **Reset** : Effacer tous les filtres

#### Actions sur la liste
- **👁️ Voir** : Afficher les détails complets
- **✏️ Éditer** : Modifier les informations
- **✅ Confirmer** : Passer de PENDING à CONFIRMED
- **▶️ Démarrer** : Passer de CONFIRMED à IN_PROGRESS
- **🏁 Terminer** : Passer de IN_PROGRESS à COMPLETED
- **❌ Annuler** : Passer à CANCELLED (avec raison)
- **👤❌ No-show** : Marquer l'absence du client

### ➕ Création d'un rendez-vous

1. Cliquer sur **Nouveau Rendez-vous**
2. Sélectionner le **client** dans la liste déroulante
3. Renseigner :
   - **Sujet** (5-200 caractères, requis)
   - **Date et heure** (jours ouvrables 9h-17h, requis)
   - **Durée** (30 min à 8h, requis)
   - **Priorité** (Urgent/Normal/Basse)
   - **Description** (optionnel, 1000 caractères max)

#### Validation automatique
- ✅ **Date future** obligatoire
- ✅ **Créneaux horaires** : 9h-17h, lundi-vendredi
- ✅ **Pas de conflit** avec rendez-vous existants
- ✅ **Sujet professionnel** requis

### ✏️ Modification d'un rendez-vous

1. Cliquer sur **Éditer** dans la liste ou détails
2. Modifier les champs souhaités
3. **Notes administratives** : Section spéciale visible uniquement par les admins
4. Les mêmes validations s'appliquent

### 👁️ Vue détaillée

#### Informations affichées
- **Informations générales** : Date, durée, priorité, statut
- **Informations client** : Nom, email, téléphone, type (compte/anonyme)
- **Détails du rendez-vous** : Sujet, description, notes admin
- **Historique complet** : Timeline des changements de statut et notifications

#### Actions contextuelles
- Boutons d'action selon le statut actuel
- Édition rapide
- Retour à la liste

### 📤 Export des données

1. Appliquer les filtres souhaités
2. Cliquer sur **Export CSV**
3. Le fichier inclut :
   - ID, Client, Email, Téléphone
   - Sujet, Date, Statut, Durée
   - Priorité, Date de création, Notes admin

## Interface technique

### 🔗 Endpoints principaux

```
GET  /admin/appointments              # Liste avec filtres
GET  /admin/appointments/{id}         # Détails
GET  /admin/appointments/new          # Formulaire création  
POST /admin/appointments/save         # Sauvegarde nouveau
GET  /admin/appointments/{id}/edit    # Formulaire édition
POST /admin/appointments/{id}/update  # Mise à jour
POST /admin/appointments/{id}/delete  # Suppression (soft)

# Actions AJAX de statut
POST /admin/appointments/{id}/confirm    # Confirmer
POST /admin/appointments/{id}/cancel     # Annuler
POST /admin/appointments/{id}/start      # Démarrer  
POST /admin/appointments/{id}/complete   # Terminer
POST /admin/appointments/{id}/no-show    # Marquer absence

GET  /admin/appointments/export       # Export CSV
```

### 🎨 Classes CSS principales

```css
.stat-card              # Cartes de statistiques
.status-badge           # Badges de statut colorés
.action-btn             # Boutons d'action circulaires
.appointments-table     # Table responsive
.filter-card            # Panneau de filtres
```

### 📱 JavaScript

- **Classe principale** : `AppointmentManager`
- **Actions AJAX** : Changements de statut sans rechargement
- **Validation temps réel** : Formulaires et champs
- **SweetAlert2** : Modales de confirmation
- **Auto-refresh** : Statistiques et données

## Sécurité

### 🔒 Contrôle d'accès
- **Rôle requis** : `ROLE_ADMIN` uniquement
- **Protection CSRF** : Tokens sur tous les formulaires
- **Audit logging** : Toutes les actions administrateur

### 🛡️ Validations
- **Côté serveur** : Annotations Bean Validation
- **Côté client** : JavaScript temps réel
- **Règles métier** : Transitions de statut validées

## Notifications

### 📧 Emails automatiques
- **Création** : Confirmation client + notification équipe
- **Changement statut** : Information client
- **Annulation** : Notification avec raison
- **Rappels** : 24h avant rendez-vous (automatique)

## Dépannage

### ❓ Problèmes courants

**Rendez-vous non visible**
- ✅ Vérifier les filtres appliqués
- ✅ Statut peut être masqué par défaut

**Erreur lors de la création**
- ✅ Date dans le futur et heures ouvrables
- ✅ Pas de conflit d'horaires
- ✅ Client sélectionné valide

**Actions de statut échouent**
- ✅ Transition autorisée selon règles métier
- ✅ Connexion admin active
- ✅ Tokens CSRF valides

**Export ne fonctionne pas**
- ✅ Droits d'écriture serveur
- ✅ Filtres ne retournent pas trop de données

## Configuration

### ⚙️ Paramètres modifiables

Dans `application.properties` :
```properties
# Créneaux horaires (par défaut 9h-17h)
appointments.business.start-hour=9
appointments.business.end-hour=17

# Durée par défaut (minutes)
appointments.default.duration=60

# Rappels automatiques (heures avant)
appointments.reminder.hours-before=24
```

## Performance

### 📈 Optimisations incluses

- **Pagination** : 10 résultats par défaut, configurable
- **Cache** : Créneaux disponibles mis en cache
- **Index DB** : Sur dates, statuts, utilisateurs
- **Requêtes optimisées** : JPA avec fetch joins
- **Compression** : Assets CSS/JS minifiés

### 🔄 Maintenance

- **Cleanup automatique** : Anciens rendez-vous archivés
- **Logs rotatifs** : Audit et erreurs
- **Monitoring** : Métriques de performance disponibles

---

## 🆘 Support

Pour toute question ou problème :
1. Consulter les logs applicatifs
2. Vérifier la documentation technique  
3. Contacter l'équipe de développement

**Version** : 1.0.0  
**Dernière mise à jour** : Janvier 2024
