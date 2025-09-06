# Restriction de Prise de Rendez-vous : 30 Jours Ouvrables

## Vue d'ensemble
Pour empêcher les abus et maintenir une gestion efficace des rendez-vous, le système limite désormais la prise de rendez-vous à **30 jours ouvrables maximum** à partir de la date actuelle.

## Fonctionnalités Implémentées

### 1. Backend (Java)

#### Nouvelle classe utilitaire : `DateUtils.java`
- **Emplacement** : `src/main/java/com/lmp/util/DateUtils.java`
- **Fonctions principales** :
  - `calculateBusinessDaysBetween()` : Calcule le nombre de jours ouvrables entre deux dates
  - `isBusinessDay()` : Vérifie si une date est un jour ouvrable (lundi-vendredi)
  - `addBusinessDays()` : Ajoute un nombre de jours ouvrables à une date
  - `getMaxAppointmentDate()` : Retourne la date limite (30 jours ouvrables)
  - `isWithinAppointmentRange()` : Vérifie si une date est dans la plage autorisée

#### Validation dans `AppointmentService.java`
- La méthode `validateAppointmentForm()` vérifie maintenant :
  1. Que le rendez-vous est pris au moins 24h à l'avance (règle existante)
  2. **NOUVEAU** : Que la date n'est pas au-delà de 30 jours ouvrables
  3. **NOUVEAU** : Que la date est un jour ouvrable (lundi-vendredi)

- **Message d'erreur français** : 
  > "La date sélectionnée est trop éloignée. Veuillez choisir une date dans les 30 prochains jours ouvrables."

### 2. Frontend (JavaScript)

#### Mise à jour de `appointment-modal-v3.js`
- **Nouvelles fonctions** :
  - `calculateBusinessDaysBetween()` : Calcule les jours ouvrables côté client
  - `isDateWithinAllowedRange()` : Vérifie si une date est dans les 30 jours ouvrables

- **Validation en temps réel** :
  - Lors de la sélection d'une date dans le calendrier
  - Affichage immédiat du message d'erreur si la date est trop éloignée
  - Désactivation du bouton de confirmation
  - Indication visuelle (couleur orange) pour les dates hors limite

- **Indicateurs visuels dans le calendrier** :
  - **Gris** : Jours passés et weekends (non sélectionnables)
  - **Orange** : Dates au-delà de 30 jours ouvrables (sélectionnables mais invalides)
  - **Bleu** : Aujourd'hui
  - **Normal** : Dates valides et disponibles

## Règles Métier

### Calcul des jours ouvrables
- **Jours ouvrables** : Lundi à Vendredi uniquement
- **Exclus** : Samedi, Dimanche
- **Période** : 30 jours ouvrables à partir d'aujourd'hui

### Exemples pratiques
- Si aujourd'hui est **Lundi 1er septembre** :
  - Date limite ≈ **Vendredi 10 octobre** (environ 6 semaines)
  
- Si aujourd'hui est **Vendredi 5 septembre** :
  - Date limite ≈ **Jeudi 16 octobre** (environ 6 semaines)

### Comportement utilisateur
1. L'utilisateur peut **naviguer** dans tout le calendrier
2. L'utilisateur peut **sélectionner** n'importe quelle date
3. Si la date est au-delà de 30 jours ouvrables :
   - Un message d'erreur clair apparaît
   - La date est marquée visuellement en orange/rouge
   - Le bouton de confirmation est désactivé
   - Les créneaux horaires ne sont pas chargés

## Tests recommandés

### Cas de test à vérifier
1. **Date valide** : Sélectionner une date dans 15 jours ouvrables → ✅ Succès
2. **Date limite** : Sélectionner exactement 30 jours ouvrables → ✅ Succès
3. **Date trop éloignée** : Sélectionner 31 jours ouvrables → ❌ Erreur avec message
4. **Weekend** : Sélectionner un samedi/dimanche → ❌ Non sélectionnable
5. **Date passée** : Sélectionner hier → ❌ Non sélectionnable

## Configuration

### Modifier la limite de jours
Si vous souhaitez changer la limite de 30 jours :

1. **Backend** : Dans `DateUtils.java`, modifier :
   ```java
   public static LocalDate getMaxAppointmentDate() {
       return addBusinessDays(LocalDate.now(), 30); // Changer 30 ici
   }
   ```

2. **Frontend** : Dans `appointment-modal-v3.js`, modifier :
   ```javascript
   isDateWithinAllowedRange(selectedDate) {
       // ...
       return businessDays <= 30; // Changer 30 ici
   }
   ```

3. **Validation** : Dans `AppointmentService.java`, modifier :
   ```java
   if (businessDays > 30) { // Changer 30 ici
       // ...
   }
   ```

## Logs et Débogage

### Logs Backend
- Un log DEBUG est généré quand une date est rejetée :
  ```
  DEBUG: Date de rendez-vous trop éloignée : XX jours ouvrables
  ```

### Logs Frontend (Console)
- Lors de la sélection d'une date invalide :
  ```
  [AppointmentModal] ⚠️ Date trop éloignée: XX jours ouvrables
  ```

## Impact sur l'expérience utilisateur

### Avantages
- ✅ Prévient les réservations abusives très éloignées
- ✅ Facilite la gestion des plannings
- ✅ Réduit les no-shows potentiels
- ✅ Message clair et en français

### Points d'attention
- L'utilisateur voit immédiatement si une date est trop éloignée
- Les dates invalides restent visibles mais sont clairement marquées
- Le système reste flexible (changement facile de la limite)

## Migration et compatibilité

### Base de données
- Aucune modification de schéma requise
- Les rendez-vous existants ne sont pas affectés

### API REST
- Les endpoints existants fonctionnent toujours
- Nouvelle validation ajoutée sans breaking changes
- Message d'erreur standardisé en français

## Support et maintenance

### En cas de problème
1. Vérifier les logs backend pour les détails de validation
2. Consulter la console JavaScript pour les erreurs côté client
3. S'assurer que les dates sont au format ISO (YYYY-MM-DD)
4. Vérifier le calcul des jours ouvrables dans les cas limites (fin de mois, années bissextiles)
