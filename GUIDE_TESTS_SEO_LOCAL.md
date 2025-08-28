# Guide de Tests SEO Locaux - LMP

## 🚀 Démarrage de l'Environnement de Test

### Option 1 : Script Automatique
```bash
# Exécuter le script de test
./test-seo-local.bat
```

### Option 2 : Démarrage Manuel
```bash
# Démarrer en mode développement
mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Ou avec variable d'environnement
set SPRING_PROFILES_ACTIVE=dev
mvnw spring-boot:run
```

## 🔍 Tests à Effectuer

### 1. Vérification des URLs Canoniques

**Test dans le navigateur :**
- Ouvrir `http://localhost:8080`
- Afficher le code source (`Ctrl+U`)
- Chercher la ligne : `<link rel="canonical"`
- **Résultat attendu :** `href="http://localhost:8080/"`

**Test des autres pages :**
- `/services` → canonical devrait être `http://localhost:8080/services`
- `/contact` → canonical devrait être `http://localhost:8080/contact`
- `/about` → canonical devrait être `http://localhost:8080/about`

### 2. Vérification des Redirections SEO

**Test de redirection canonique :**
```bash
# Test avec curl (si disponible)
curl -I http://127.0.0.1:8080/

# Résultat attendu : HTTP 302 vers http://localhost:8080/
```

**Test manuel :**
- Ouvrir `http://127.0.0.1:8080` dans le navigateur
- Vérifier si l'URL change automatiquement vers `http://localhost:8080`

### 3. Vérification du Sitemap et Robots

**Sitemap.xml :**
- Ouvrir `http://localhost:8080/sitemap.xml`
- Vérifier que toutes les URLs commencent par `http://localhost:8080`

**Robots.txt :**
- Ouvrir `http://localhost:8080/robots.txt`
- Vérifier la ligne `Sitemap: http://localhost:8080/sitemap.xml`

### 4. Vérification des Headers SEO

**Test avec outils développeur :**
1. Ouvrir F12 → Network
2. Actualiser la page
3. Cliquer sur la requête principale
4. Vérifier la présence du header : `X-Robots-Tag: index, follow`

### 5. Vérification des Logs

**Dans la console Spring Boot, rechercher :**
```
SEO Interceptor - Request: http://localhost:8080/, URI: /, Canonical base: http://localhost:8080
```

**Types de logs attendus :**
- `DEBUG com.lmp.web.config.SeoInterceptor` - Logs de diagnostic
- `INFO com.lmp.web.config.SeoInterceptor` - Redirections effectuées

## 🐛 Résolution des Problèmes

### Problème : Application ne démarre pas
**Solution :**
```bash
# Vérifier le port
netstat -an | findstr :8080

# Changer le port si nécessaire
mvnw spring-boot:run -Dserver.port=8081
```

### Problème : URLs canoniques incorrectes
**Vérification :**
1. Confirmer que `application-dev.properties` est utilisé
2. Vérifier `app.base.url=http://localhost:8080`
3. Redémarrer l'application

### Problème : Pas de redirections
**Vérification :**
1. Confirmer que l'intercepteur est enregistré
2. Vérifier les logs pour `SeoInterceptor`
3. Tester avec différentes URLs d'accès

## ✅ Checklist de Validation

- [ ] Application démarre sur `http://localhost:8080`
- [ ] URLs canoniques pointent vers `localhost:8080`
- [ ] Sitemap.xml accessible et correct
- [ ] Robots.txt accessible avec bon sitemap
- [ ] Headers `X-Robots-Tag` présents
- [ ] Redirections fonctionnent pour `127.0.0.1`
- [ ] Logs SEO visibles dans la console
- [ ] Métadonnées Open Graph correctes

## 🚀 Déploiement en Production

Une fois les tests validés localement :

1. **Arrêter l'application locale**
2. **Déployer sur Railway** avec le profil par défaut
3. **Vérifier** que `app.base.url=https://lmp.run.place` en production
4. **Attendre 48-72h** pour voir l'amélioration dans Google Search Console

## 📝 Notes

- Le profil `dev` utilise des clés Stripe de test
- Les logs sont plus détaillés en mode développement
- Les URLs canoniques s'adaptent automatiquement selon l'environnement