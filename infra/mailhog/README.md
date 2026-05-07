# LMP — MailHog (catch-all SMTP staging)

Capture tous les emails envoyés par lmp-test-back sans rien envoyer en réel.
Critique pour load tests : pas de quota Mailtrap/Gmail à exploser.

## Déploiement Dokploy

### 1. Cloudflare DNS

Add A record `mail.lmp-services.ca` → `140.238.216.231` (proxied OFF, DNS-only pour Let's Encrypt HTTP-01).

### 2. Generate basic auth

```bash
# Sur n'importe quelle machine avec htpasswd (apache2-utils)
htpasswd -nb admin "MotDePasseFort2026"
# Output: admin:$apr1$ABC.DEF$XYZ...

# Puis doubler les $ pour Docker Compose:
# admin:$$apr1$$ABC.DEF$$XYZ...
```

### 3. Créer projet Dokploy "Compose"

- New > Compose
- Provider : Git (ce repo, branche develop)
- Compose Path : `infra/mailhog/docker-compose.yml`
- Environment Variables :
  - `MAILHOG_BASIC_AUTH=admin:$$apr1$$...$$...` (doubler les `$`)
- Deploy

### 4. Update env vars staging backend (lmp-test-back)

Dans Dokploy lmp-test-back → Environment, modifier :

```
SMTP_HOST=lmp-mailhog
SMTP_PORT=1025
SMTP_USERNAME=
SMTP_PASSWORD=
```

Vide pour USERNAME/PASSWORD = MailHog n'a pas d'auth SMTP. `application-staging.properties` désactive `smtp.auth` et `starttls`.

Redéployer lmp-test-back.

### 5. Vérifier

```bash
# Test SMTP depuis lmp-test-back
ssh ubuntu@140.238.216.231
sudo docker exec lmp-test-lmptestback-6xvske \
  curl -v --url 'smtp://lmp-mailhog:1025' --mail-from 'test@lmp.test' \
       --mail-rcpt 'recv@lmp.test' --upload-file <(echo -e "Subject: Test\r\n\r\nHello")
```

Puis ouvrir https://mail.lmp-services.ca → l'email doit apparaître.

## Use cases

| Cas | Avant | Avec MailHog |
|-----|-------|--------------|
| Load test 5k VUs register | 535 daily limit Mailtrap | 0 erreur, 10k emails capturés |
| Dev — voir template welcome | Login Mailtrap inbox lent | UI MailHog instant |
| E2E browser — flow reset password | Mailtrap quota | Captures tout, link click direct |

## Limites & cleanup

- **Mémoire only** par défaut — restart container = emails perdus (acceptable pour staging)
- Si besoin persistence : ajouter volume + flag `--storage=maildir`
- Purge UI : bouton "Delete all messages" en haut

## Sécurité

- UI publique avec Basic Auth Traefik
- SMTP 1025 jamais exposé via Traefik (réseau Docker interne uniquement)
- Container peut être stoppé entre tests pour économiser RAM (~10 MB anyway)

## API REST

MailHog expose une API JSON :
- `GET /api/v2/messages` — liste tous les emails
- `GET /api/v2/messages?limit=10` — derniers 10
- `DELETE /api/v1/messages` — purge tout

Utile pour assertions E2E :
```javascript
const inbox = await fetch('https://mail.lmp-services.ca/api/v2/messages');
const messages = await inbox.json();
expect(messages.items[0].Content.Body).toContain('verification link');
```
