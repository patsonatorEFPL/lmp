# Guide d'Installation Nginx pour LMP Services
## Configuration Reverse Proxy avec SSL/TLS pour https://lmp-services.ca

Ce guide vous accompagne dans l'installation et la configuration complète de Nginx comme reverse proxy pour l'application Spring Boot LMP, avec gestion automatique des certificats SSL via Let's Encrypt.

---

## 📋 Prérequis

### Système
- **Serveur** : Ubuntu 20.04+ / Debian 11+ / CentOS 8+ / RHEL 8+
- **RAM** : Minimum 1 GB (2 GB recommandé)
- **Domaine** : `lmp-services.ca` pointant vers votre serveur
- **Ports** : 80, 443 ouverts dans le firewall
- **Application** : Spring Boot LMP fonctionnelle sur localhost:8080

### Accès
- Accès root ou sudo sur le serveur
- DNS configuré pour `lmp-services.ca` → IP du serveur

---

## 🚀 Installation Step-by-Step

### 1. Installation de Nginx

#### Ubuntu/Debian
```bash
# Mise à jour du système
sudo apt update && sudo apt upgrade -y

# Installation de Nginx
sudo apt install nginx -y

# Vérification de l'installation
nginx -v
sudo systemctl status nginx
```

#### CentOS/RHEL
```bash
# Mise à jour du système
sudo yum update -y

# Installation de Nginx
sudo yum install nginx -y

# Démarrage et activation
sudo systemctl start nginx
sudo systemctl enable nginx

# Vérification
sudo systemctl status nginx
```

### 2. Installation de Certbot (Let's Encrypt)

#### Ubuntu/Debian
```bash
# Installation de Certbot et plugin Nginx
sudo apt install certbot python3-certbot-nginx -y
```

#### CentOS/RHEL
```bash
# Installation d'EPEL et Certbot
sudo yum install epel-release -y
sudo yum install certbot python3-certbot-nginx -y
```

### 3. Configuration du Firewall

#### UFW (Ubuntu/Debian)
```bash
# Autoriser HTTP et HTTPS
sudo ufw allow 'Nginx HTTP'
sudo ufw allow 'Nginx HTTPS'
sudo ufw allow 'Nginx Full'

# Vérifier le statut
sudo ufw status
```

#### Firewalld (CentOS/RHEL)
```bash
# Autoriser HTTP et HTTPS
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload

# Vérifier
sudo firewall-cmd --list-all
```

### 4. Préparation des Répertoires

```bash
# Créer les répertoires nécessaires
sudo mkdir -p /var/www/certbot
sudo mkdir -p /var/cache/nginx/lmp
sudo mkdir -p /var/log/nginx

# Définir les permissions
sudo chown -R www-data:www-data /var/www/certbot
sudo chown -R nginx:nginx /var/cache/nginx/lmp
sudo chmod -R 755 /var/www/certbot
```

### 5. Sauvegarde de la Configuration par Défaut

```bash
# Sauvegarder la configuration Nginx par défaut
sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup
sudo cp /etc/nginx/sites-available/default /etc/nginx/sites-available/default.backup
```

### 6. Installation de la Configuration LMP

```bash
# Copier le fichier de configuration
sudo cp /path/to/lmp/nginx/lmp-services.ca.conf /etc/nginx/sites-available/

# Désactiver le site par défaut
sudo unlink /etc/nginx/sites-enabled/default

# Activer la configuration LMP
sudo ln -s /etc/nginx/sites-available/lmp-services.ca.conf /etc/nginx/sites-enabled/

# Tester la configuration
sudo nginx -t
```

### 7. Configuration SSL Temporaire

Créer une configuration temporaire pour obtenir les certificats :

```bash
# Créer un fichier temporaire pour Let's Encrypt
sudo tee /etc/nginx/sites-available/lmp-temp.conf > /dev/null <<EOF
server {
    listen 80;
    server_name lmp-services.ca www.lmp-services.ca;
    
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    
    location / {
        return 301 https://\$server_name\$request_uri;
    }
}
EOF

# Activer la configuration temporaire
sudo unlink /etc/nginx/sites-enabled/lmp-services.ca.conf
sudo ln -s /etc/nginx/sites-available/lmp-temp.conf /etc/nginx/sites-enabled/

# Recharger Nginx
sudo systemctl reload nginx
```

### 8. Obtention des Certificats SSL

```bash
# Obtenir les certificats Let's Encrypt
sudo certbot certonly --webroot \
    -w /var/www/certbot \
    -d lmp-services.ca \
    -d www.lmp-services.ca \
    --email votre-email@domain.com \
    --agree-tos \
    --no-eff-email

# Vérifier les certificats
sudo ls -la /etc/letsencrypt/live/lmp-services.ca/
```

### 9. Activation de la Configuration Finale

```bash
# Supprimer la configuration temporaire
sudo unlink /etc/nginx/sites-enabled/lmp-temp.conf

# Activer la configuration finale
sudo ln -s /etc/nginx/sites-available/lmp-services.ca.conf /etc/nginx/sites-enabled/

# Tester la configuration
sudo nginx -t

# Recharger Nginx
sudo systemctl reload nginx
```

---

## 🔧 Configuration du Renouvellement Automatique SSL

### 1. Test du Renouvellement

```bash
# Tester le renouvellement
sudo certbot renew --dry-run
```

### 2. Configuration du Cron Job

```bash
# Éditer le crontab
sudo crontab -e

# Ajouter cette ligne pour un renouvellement quotidien
0 12 * * * /usr/bin/certbot renew --quiet && /usr/bin/systemctl reload nginx
```

### 3. Configuration Systemd Timer (Alternative)

```bash
# Créer le service
sudo tee /etc/systemd/system/certbot-renewal.service > /dev/null <<EOF
[Unit]
Description=Certbot Renewal

[Service]
ExecStart=/usr/bin/certbot renew --quiet --post-hook "systemctl reload nginx"
EOF

# Créer le timer
sudo tee /etc/systemd/system/certbot-renewal.timer > /dev/null <<EOF
[Unit]
Description=Run certbot twice daily

[Timer]
OnCalendar=*-*-* 00,12:00:00
RandomizedDelaySec=3600
Persistent=true

[Install]
WantedBy=timers.target
EOF

# Activer le timer
sudo systemctl enable certbot-renewal.timer
sudo systemctl start certbot-renewal.timer
```

---

## ✅ Tests et Validation

### 1. Vérification de la Configuration Nginx

```bash
# Test de la syntaxe
sudo nginx -t

# Vérification du statut
sudo systemctl status nginx

# Test des ports
sudo netstat -tlnp | grep nginx
```

### 2. Tests de Connectivité

```bash
# Test HTTP (doit rediriger vers HTTPS)
curl -I http://lmp-services.ca

# Test HTTPS
curl -I https://lmp-services.ca

# Test des headers de sécurité
curl -I https://lmp-services.ca | grep -E "(Strict-Transport|X-Frame|X-Content)"
```

### 3. Tests SSL

```bash
# Vérification du certificat
openssl s_client -connect lmp-services.ca:443 -servername lmp-services.ca

# Test SSL Labs (en ligne)
# https://www.ssllabs.com/ssltest/analyze.html?d=lmp-services.ca
```

### 4. Tests de Performance

```bash
# Test de compression
curl -H "Accept-Encoding: gzip" -I https://lmp-services.ca

# Test du cache
curl -I https://lmp-services.ca/css/style.css
```

### 5. Validation Spring Boot

```bash
# Vérifier que Spring Boot reçoit les bons headers
# Regarder les logs de l'application pour confirmer :
# - X-Forwarded-Proto: https
# - X-Forwarded-Host: lmp-services.ca
# - X-Forwarded-Port: 443

sudo tail -f /var/log/spring-boot/lmp.log
```

---

## 🔍 Monitoring et Logs

### 1. Surveillance des Logs

```bash
# Logs d'accès
sudo tail -f /var/log/nginx/lmp-services.ca.access.log

# Logs d'erreur
sudo tail -f /var/log/nginx/lmp-services.ca.error.log

# Logs Nginx globaux
sudo tail -f /var/log/nginx/error.log
```

### 2. Métriques Nginx

```bash
# Statut Nginx (si activé dans la config)
curl http://localhost:8081/nginx_status

# Santé Nginx
curl https://lmp-services.ca/nginx-health
```

### 3. Configuration de Logrotate

```bash
# Créer la configuration logrotate
sudo tee /etc/logrotate.d/lmp-nginx > /dev/null <<EOF
/var/log/nginx/lmp-services.ca.*.log {
    daily
    missingok
    rotate 52
    compress
    delaycompress
    notifempty
    create 644 www-data adm
    postrotate
        if [ -f /var/run/nginx.pid ]; then
            kill -USR1 \$(cat /var/run/nginx.pid)
        fi
    endscript
}
EOF
```

---

## 🚨 Dépannage

### Problèmes Courants

#### 1. Erreur 502 Bad Gateway
```bash
# Vérifier que Spring Boot fonctionne
curl http://localhost:8080

# Vérifier les logs
sudo tail -f /var/log/nginx/lmp-services.ca.error.log

# Vérifier la connectivité interne
sudo netstat -tlnp | grep 8080
```

#### 2. Certificat SSL Expiré
```bash
# Vérifier l'expiration
sudo certbot certificates

# Renouveler manuellement
sudo certbot renew --force-renewal

# Recharger Nginx
sudo systemctl reload nginx
```

#### 3. Rate Limiting Trop Restrictif
```bash
# Ajuster les limites dans la configuration
# Modifier les valeurs dans lmp-services.ca.conf :
# limit_req_zone $binary_remote_addr zone=main:10m rate=20r/s;

# Recharger
sudo nginx -t && sudo systemctl reload nginx
```

#### 4. Headers X-Forwarded Manquants
```bash
# Vérifier la configuration Spring Boot
# Assurer que server.forward-headers-strategy=framework

# Vérifier les logs Spring Boot
sudo tail -f /var/log/spring-boot/lmp.log | grep X-Forwarded
```

### Commandes de Diagnostic

```bash
# Vérification complète du système
sudo systemctl status nginx
sudo systemctl status certbot.timer
sudo nginx -t
sudo certbot certificates
curl -I https://lmp-services.ca
sudo netstat -tlnp | grep -E "(80|443|8080)"
```

---

## 🔐 Sécurité Avancée

### 1. Fail2Ban pour Nginx

```bash
# Installation
sudo apt install fail2ban -y

# Configuration pour Nginx
sudo tee /etc/fail2ban/jail.d/nginx-lmp.conf > /dev/null <<EOF
[nginx-http-auth]
enabled = true
port = http,https
logpath = /var/log/nginx/lmp-services.ca.error.log

[nginx-limit-req]
enabled = true
port = http,https
logpath = /var/log/nginx/lmp-services.ca.error.log
maxretry = 10

[nginx-botsearch]
enabled = true
port = http,https
logpath = /var/log/nginx/lmp-services.ca.access.log
maxretry = 2
EOF

# Redémarrer Fail2Ban
sudo systemctl restart fail2ban
```

### 2. Surveillance avec Netdata (Optionnel)

```bash
# Installation de Netdata
bash <(curl -Ss https://my-netdata.io/kickstart.sh)

# Configuration pour Nginx
sudo systemctl enable netdata
sudo systemctl start netdata
```

---

## 📚 Maintenance

### Tâches Régulières

#### Hebdomadaire
- Vérifier les logs d'erreur
- Contrôler l'utilisation du cache
- Vérifier les métriques de performance

#### Mensuel
- Mettre à jour Nginx et les dépendances
- Vérifier l'expiration des certificats
- Analyser les rapports de sécurité

#### Trimestriel
- Réviser la configuration de sécurité
- Optimiser les paramètres de performance
- Tester la récupération après incident

### Scripts de Maintenance

```bash
# Script de vérification quotidienne
sudo tee /usr/local/bin/lmp-nginx-check.sh > /dev/null <<'EOF'
#!/bin/bash
echo "=== Vérification Nginx LMP - $(date) ==="

# Test configuration
nginx -t

# Vérification SSL
echo "Certificats SSL :"
certbot certificates

# Test connectivité
echo "Test HTTPS :"
curl -Is https://lmp-services.ca | head -1

# Espace disque logs
echo "Espace disque logs :"
du -sh /var/log/nginx/

echo "=== Fin vérification ==="
EOF

sudo chmod +x /usr/local/bin/lmp-nginx-check.sh
```

---

## 🎯 Optimisations Spécifiques à Coolify

Si vous utilisez Coolify pour le déploiement :

### 1. Configuration Docker

```bash
# Assurer que Nginx peut communiquer avec le container Spring Boot
# Modifier l'upstream dans la configuration :
# upstream lmp_backend {
#     server lmp-app:8080 max_fails=3 fail_timeout=30s;
# }
```

### 2. Variables d'Environnement

```bash
# Dans Coolify, définir :
SPRING_PROFILES_ACTIVE=prod
SERVER_FORWARD_HEADERS_STRATEGY=framework
SERVER_USE_FORWARD_HEADERS=true
```

### 3. Health Checks

```bash
# Configurer les health checks Coolify
# URL: https://lmp-services.ca/actuator/health
# Intervalle: 30s
# Timeout: 10s
```

---

## 📞 Support

### Logs Importants
- Nginx : `/var/log/nginx/lmp-services.ca.*.log`
- Let's Encrypt : `/var/log/letsencrypt/letsencrypt.log`
- Spring Boot : `/var/log/spring-boot/lmp.log`

### Commandes de Debug Rapide
```bash
# Status complet
sudo systemctl status nginx
curl -I https://lmp-services.ca
sudo nginx -t

# Redémarrage sécurisé
sudo nginx -t && sudo systemctl reload nginx
```

### Contact
Pour toute question ou problème, vérifiez d'abord les logs et utilisez les commandes de diagnostic fournies dans ce guide.

---

**📝 Note :** Ce guide a été testé sur Ubuntu 20.04+ et CentOS 8+. Adaptez les commandes selon votre distribution Linux.