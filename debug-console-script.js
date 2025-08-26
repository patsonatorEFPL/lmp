// Script de diagnostic pour la console du navigateur
// À exécuter dans la console de la page LMP après connexion

console.log('🔧 Script de diagnostic LMP - Changement de mot de passe');
console.log('================================================');

// ⚠️ MISE À JOUR DES IDENTIFIANTS CORRECTS
const debugConfig = {
    // Identifiants corrects trouvés dans les logs Spring Boot
    adminEmail: 'admin@lmp.ca',
    adminPassword: 'admin123',
    userEmail: 'user@lmp.ca', 
    userPassword: 'user123',
    newPassword: 'NewPass123!',
    baseUrl: window.location.origin
};

console.log('🔑 IDENTIFIANTS CORRECTS DÉTECTÉS:');
console.log('Admin:', debugConfig.adminEmail, '/', debugConfig.adminPassword);
console.log('User:', debugConfig.userEmail, '/', debugConfig.userPassword);

// Fonction principale de diagnostic
async function runPasswordDiagnosis(useAdmin = false) {
    console.log('🚀 Démarrage du diagnostic...');
    
    const currentEmail = useAdmin ? debugConfig.adminEmail : debugConfig.userEmail;
    const currentPassword = useAdmin ? debugConfig.adminPassword : debugConfig.userPassword;
    
    console.log(`🔐 Test avec le compte: ${currentEmail} (${useAdmin ? 'ADMIN' : 'USER'})`);
    
    // Étape 1: Vérifier l'environnement
    console.log('\n📋 ÉTAPE 1: Vérification de l\'environnement');
    console.log('==============================================');
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    
    console.log(`✅ URL actuelle: ${window.location.href}`);
    console.log(`✅ CSRF Token présent: ${!!csrfToken}`);
    console.log(`✅ CSRF Header: ${csrfHeader || 'Non trouvé'}`);
    
    if (csrfToken) {
        console.log(`✅ CSRF Token (preview): ${csrfToken.substring(0, 20)}...`);
    }
    
    // Étape 2: Vérifier les formulaires
    console.log('\n🔍 ÉTAPE 2: Analyse des formulaires');
    console.log('===================================');
    
    const forms = {
        passwordChangeForm: document.getElementById('passwordChangeForm'),
        passwordForm: document.getElementById('passwordForm'),
        passwordChangeModal: document.getElementById('passwordChangeModal')
    };
    
    Object.entries(forms).forEach(([name, element]) => {
        console.log(`${element ? '✅' : '❌'} ${name}: ${element ? 'Trouvé' : 'Non trouvé'}`);
        if (element) {
            console.log(`    - Action: ${element.action || 'Non définie'}`);
            console.log(`    - Method: ${element.method || 'Non définie'}`);
        }
    });
    
    // Étape 3: Vérifier les fonctions JavaScript
    console.log('\n⚙️ ÉTAPE 3: Fonctions JavaScript disponibles');
    console.log('============================================');
    
    const functions = [
        'submitPasswordChange',
        'showNotification',
        'advancedFormHandler'
    ];
    
    functions.forEach(funcName => {
        const func = window[funcName];
        console.log(`${func ? '✅' : '❌'} ${funcName}: ${typeof func}`);
        if (func && typeof func === 'function') {
            console.log(`    - Code: ${func.toString().substring(0, 100)}...`);
        }
    });
    
    // Étape 4: Test de l'endpoint avec les VRAIS identifiants
    if (csrfToken && csrfHeader) {
        console.log('\n🎯 ÉTAPE 4: Test de l\'endpoint /user/password/change');
        console.log('==================================================');
        
        const testData = {
            currentPassword: currentPassword,
            newPassword: debugConfig.newPassword,
            confirmPassword: debugConfig.newPassword
        };
        
        console.log('📤 Données de test (VRAIS identifiants):', testData);
        
        try {
            const response = await fetch('/user/password/change', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify(testData)
            });
            
            console.log(`📥 Status de réponse: ${response.status} ${response.statusText}`);
            console.log(`📥 Content-Type: ${response.headers.get('content-type')}`);
            console.log(`📥 OK: ${response.ok}`);
            
            // Afficher tous les headers de réponse
            console.log('📥 Headers de réponse:');
            for (let [key, value] of response.headers.entries()) {
                console.log(`    ${key}: ${value}`);
            }
            
            const responseText = await response.text();
            console.log('📥 Réponse complète:');
            console.log(responseText);
            
            // Essayer de parser en JSON
            try {
                const jsonResponse = JSON.parse(responseText);
                console.log('✅ JSON parsé avec succès:');
                console.log(jsonResponse);
                
                // Analyser le contenu de la réponse JSON
                if (jsonResponse.success) {
                    console.log('🎉 SUCCÈS: Changement de mot de passe réussi!');
                } else if (jsonResponse.error) {
                    console.log('❌ ERREUR:', jsonResponse.error);
                }
                
            } catch (parseError) {
                console.log('⚠️ La réponse n\'est pas du JSON valide');
                console.log('📄 Contenu HTML détecté:', responseText.includes('<html>'));
                
                // Si c'est du HTML, chercher des indices d'erreur
                if (responseText.includes('error') || responseText.includes('Error')) {
                    console.log('🚨 Possible erreur détectée dans le HTML');
                }
            }
            
        } catch (error) {
            console.error('❌ Erreur lors du test:', error);
        }
    } else {
        console.log('❌ Impossible de tester l\'endpoint - CSRF manquant');
    }
    
    // Étape 5: Test avec mot de passe incorrect
    if (csrfToken && csrfHeader) {
        console.log('\n🚨 ÉTAPE 5: Test avec mot de passe incorrect');
        console.log('=============================================');
        
        const wrongPasswordData = {
            currentPassword: 'WRONG_PASSWORD_123',
            newPassword: debugConfig.newPassword,
            confirmPassword: debugConfig.newPassword
        };
        
        try {
            const response = await fetch('/user/password/change', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify(wrongPasswordData)
            });
            
            console.log(`📥 Status (mauvais MDP): ${response.status}`);
            const wrongResponseText = await response.text();
            console.log('📥 Réponse (mauvais MDP):');
            console.log(wrongResponseText);
            
            // Parser la réponse d'erreur
            try {
                const errorJson = JSON.parse(wrongResponseText);
                console.log('✅ Réponse d\'erreur JSON parsée:', errorJson);
            } catch (e) {
                console.log('⚠️ Réponse d\'erreur n\'est pas du JSON');
            }
            
        } catch (error) {
            console.error('❌ Erreur avec mauvais mot de passe:', error);
        }
    }
    
    console.log('\n🏁 DIAGNOSTIC TERMINÉ');
    console.log('====================');
    console.log('Vérifiez les résultats ci-dessus et les logs du serveur Spring Boot');
    
    return {
        csrf: { token: !!csrfToken, header: csrfHeader },
        forms: forms,
        functions: functions.map(f => ({ name: f, exists: !!window[f] })),
        testAccount: { email: currentEmail, isAdmin: useAdmin }
    };
}

// Fonction pour tester uniquement l'endpoint avec les bons identifiants
async function testPasswordEndpointOnly(useAdmin = false) {
    console.log('🎯 Test rapide de l\'endpoint uniquement');
    
    const currentPassword = useAdmin ? debugConfig.adminPassword : debugConfig.userPassword;
    const currentEmail = useAdmin ? debugConfig.adminEmail : debugConfig.userEmail;
    
    console.log(`Test avec: ${currentEmail} (${useAdmin ? 'ADMIN' : 'USER'})`);
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    
    if (!csrfToken || !csrfHeader) {
        console.error('❌ CSRF Token ou Header manquant');
        return;
    }
    
    try {
        const response = await fetch('/user/password/change', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({
                currentPassword: currentPassword,
                newPassword: debugConfig.newPassword,
                confirmPassword: debugConfig.newPassword
            })
        });
        
        console.log(`Status: ${response.status}`);
        const text = await response.text();
        console.log('Response:', text);
        
        return { status: response.status, response: text, account: currentEmail };
    } catch (error) {
        console.error('Erreur:', error);
        return { error: error.message };
    }
}

// Fonction pour vérifier l'authentification
function checkAuthentication() {
    console.log('🔐 Vérification de l\'authentification');
    
    // Vérifier les éléments UI qui indiquent une connexion
    const authIndicators = {
        userMenu: document.querySelector('.user-menu'),
        logoutButton: document.querySelector('[href="/logout"]'),
        dashboardLink: document.querySelector('[href="/dashboard"]'),
        settingsPage: window.location.href.includes('/settings'),
        loginPage: window.location.href.includes('/login'),
        userInfo: document.querySelector('.user-info, .username, .user-email')
    };
    
    console.log('Indicateurs d\'authentification:');
    Object.entries(authIndicators).forEach(([name, value]) => {
        console.log(`  ${name}: ${!!value}`);
        if (value && value.textContent) {
            console.log(`    Text: ${value.textContent.trim()}`);
        }
    });
    
    return authIndicators;
}

// Fonction pour tester les deux types de comptes
async function testBothAccounts() {
    console.log('🔄 Test avec les deux types de comptes');
    console.log('=====================================');
    
    console.log('\n👤 Test compte USER:');
    const userResult = await testPasswordEndpointOnly(false);
    
    console.log('\n👑 Test compte ADMIN:');
    const adminResult = await testPasswordEndpointOnly(true);
    
    return { user: userResult, admin: adminResult };
}

// Instructions d'utilisation MISES À JOUR
console.log('\n📖 INSTRUCTIONS MISES À JOUR:');
console.log('=============================');
console.log('🚨 IDENTIFIANTS CORRECTS trouvés dans les logs:');
console.log('   User: user@lmp.ca / user123');
console.log('   Admin: admin@lmp.ca / admin123');
console.log('');
console.log('1. Connectez-vous avec un des comptes ci-dessus');
console.log('2. Allez sur la page des paramètres ou du dashboard');
console.log('3. Exécutez: runPasswordDiagnosis(false) pour USER');
console.log('4. Ou: runPasswordDiagnosis(true) pour ADMIN'); 
console.log('5. Test rapide: testPasswordEndpointOnly(false/true)');
console.log('6. Test des deux: testBothAccounts()');
console.log('');
console.log('Fonctions disponibles:');
console.log('- runPasswordDiagnosis(isAdmin) : Diagnostic complet');
console.log('- testPasswordEndpointOnly(isAdmin) : Test rapide endpoint');
console.log('- checkAuthentication() : Vérifier connexion');
console.log('- testBothAccounts() : Tester USER et ADMIN');

// Exporter les fonctions dans le scope global
window.runPasswordDiagnosis = runPasswordDiagnosis;
window.testPasswordEndpointOnly = testPasswordEndpointOnly;
window.checkAuthentication = checkAuthentication;
window.testBothAccounts = testBothAccounts;
window.debugConfig = debugConfig;

console.log('\n🎯 PRÊT POUR LE DIAGNOSTIC!');
console.log('Script chargé avec les bons identifiants.');