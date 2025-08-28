@echo off
echo ===================================
echo    TESTS SEO LOCAUX - LMP
echo ===================================
echo.

echo Demarrage de l'application en mode DEVELOPPEMENT...
echo Port: http://localhost:8080
echo URLs canoniques: http://localhost:8080
echo.

echo 1. Demarrage du serveur Spring Boot en mode DEV...
start cmd /k "mvnw spring-boot:run -Dspring-boot.run.profiles=dev"

echo.
echo 2. Attente du demarrage (30 secondes)...
timeout /t 30 /nobreak > nul

echo.
echo 3. Tests SEO automatiques:
echo.

echo a) Test de la page d'accueil...
curl -I http://localhost:8080/ 2>nul | findstr "HTTP\|Location\|X-Robots"

echo.
echo b) Test de la page services...
curl -I http://localhost:8080/services 2>nul | findstr "HTTP\|Location\|X-Robots"

echo.
echo c) Test de redirection canonique...
curl -I http://127.0.0.1:8080/ 2>nul | findstr "HTTP\|Location"

echo.
echo 4. Ouverture du navigateur pour tests manuels...
start http://localhost:8080

echo.
echo 5. Tests a effectuer manuellement:
echo    - Verifier les URLs canoniques dans le code source
echo    - Tester /sitemap.xml
echo    - Tester /robots.txt
echo    - Verifier les meta tags SEO
echo.

echo 6. Consultation des logs SEO:
echo    Les logs de l'intercepteur SEO s'affichent dans la console Spring Boot
echo    Recherchez "SEO Interceptor" dans les logs
echo.

pause