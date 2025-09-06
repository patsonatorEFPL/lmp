/**
 * Script de test pour déboguer le chargement des créneaux
 */

$(document).ready(() => {
    console.log('🔧 Test Slots Debug Script Loaded');
    
    // Fonction pour tester le chargement des créneaux
    window.testLoadSlots = function(date) {
        const dateStr = date || '2025-09-08'; // Date par défaut
        console.log(`📅 Test de chargement des créneaux pour: ${dateStr}`);
        
        const endpoint = '/appointments-test/available-slots';
        console.log(`🌐 Endpoint: ${endpoint}`);
        
        // Afficher l'état du conteneur
        const container = $('#timeSlotsContainer');
        console.log('📦 Conteneur trouvé:', container.length > 0);
        console.log('👁️ Classes du conteneur:', container.attr('class'));
        console.log('📝 Contenu actuel:', container.html());
        
        // Faire la requête AJAX
        $.ajax({
            url: endpoint,
            method: 'GET',
            data: { date: dateStr },
            success: function(response) {
                console.log('✅ Réponse reçue:', response);
                console.log('📊 Type de réponse:', typeof response);
                console.log('🔢 Nombre de créneaux:', Array.isArray(response) ? response.length : 'Non-array');
                
                // Afficher les créneaux
                if (Array.isArray(response) && response.length > 0) {
                    displayTestSlots(response);
                } else {
                    console.warn('⚠️ Aucun créneau ou format invalide');
                    container.html('<div class="text-red-500">Aucun créneau disponible</div>');
                }
            },
            error: function(xhr, status, error) {
                console.error('❌ Erreur AJAX:', {
                    status: xhr.status,
                    statusText: xhr.statusText,
                    responseText: xhr.responseText,
                    error: error
                });
                container.html(`<div class="text-red-500">Erreur: ${error}</div>`);
            }
        });
    };
    
    // Fonction pour afficher les créneaux
    function displayTestSlots(slots) {
        console.log('🎨 Affichage de', slots.length, 'créneaux');
        
        const container = $('#timeSlotsContainer');
        
        // S'assurer que le conteneur est visible
        container.removeClass('hidden');
        console.log('👁️ Conteneur rendu visible');
        
        // Construire le HTML
        let html = '<div class="p-4 bg-blue-50 rounded-lg">';
        html += '<h5 class="font-semibold mb-3">⏰ Créneaux disponibles</h5>';
        html += '<div class="grid grid-cols-4 gap-2">';
        
        slots.forEach((slot, index) => {
            console.log(`  📍 Créneau ${index + 1}: ${slot}`);
            html += `
                <button type="button" 
                    class="bg-white hover:bg-blue-100 text-blue-800 py-2 px-3 rounded border border-blue-300"
                    onclick="console.log('Sélectionné: ${slot}')">
                    ${slot}
                </button>
            `;
        });
        
        html += '</div></div>';
        
        // Injecter le HTML
        container.html(html);
        console.log('✅ HTML injecté dans le conteneur');
        
        // Vérifier le résultat
        console.log('📏 Hauteur du conteneur:', container.height());
        console.log('📐 Largeur du conteneur:', container.width());
        console.log('👀 Visible?', container.is(':visible'));
    }
    
    // Auto-test au chargement
    console.log('🚀 Lancement du test automatique dans 2 secondes...');
    setTimeout(() => {
        if ($('#appointmentModal').is(':visible')) {
            console.log('📂 Modal ouvert, test des créneaux...');
            testLoadSlots();
        } else {
            console.log('📁 Modal fermé, ouvrez-le d\'abord puis appelez testLoadSlots()');
        }
    }, 2000);
    
    // Ajouter un bouton de test sur la page
    if ($('#dbIndicator').length > 0) {
        $('#dbIndicator').after(`
            <button onclick="testLoadSlots()" 
                class="ml-4 px-4 py-2 bg-yellow-500 text-white rounded hover:bg-yellow-600">
                🔧 Test Créneaux
            </button>
        `);
    }
});
