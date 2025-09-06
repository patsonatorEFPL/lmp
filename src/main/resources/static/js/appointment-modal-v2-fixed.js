/**
 * Script amélioré pour le modal de prise de rendez-vous avec calendrier interactif
 * Version 2.0 - Créneaux d'1 heure avec logs détaillés
 * FIXED: Correspondance des IDs avec le HTML
 */

// ============= CLASSE GLOBALE APPOINTMENTMODAL =============
window.AppointmentModal = {
    // État du modal
    isInitialized: false,
    
    // Initialiser le modal
    init: function() {
        if (this.isInitialized) return;
        
        console.log('%c[AppointmentModal] 🚀 Initialisation de la classe globale', 'color: #FF6B35');
        this.isInitialized = true;
        
        // Lancer l'initialisation jQuery
        if (typeof $ !== 'undefined') {
            AppointmentModalController.init();
        } else {
            console.error('[AppointmentModal] ❌ jQuery non disponible');
        }
    },
    
    // Ouvrir le modal
    open: function(options = {}) {
        console.log('%c[AppointmentModal] 📂 Ouverture du modal', 'color: #4CAF50');
        
        if (!this.isInitialized) {
            this.init();
        }
        
        // Pré-remplir les champs si des options sont fournies - FIXED: utiliser les bons IDs
        if (options.name) $('#name').val(options.name);
        if (options.email) $('#email').val(options.email);
        if (options.phone) $('#phone').val(options.phone);
        if (options.service) $('#service').val(options.service);
        if (options.message) $('#message').val(options.message);
        
        // Afficher le modal
        $('#appointmentModal').removeClass('hidden');
        
        // Émettre un événement personnalisé
        document.dispatchEvent(new CustomEvent('modalOpened', { detail: options }));
    },
    
    // Fermer le modal
    close: function() {
        console.log('%c[AppointmentModal] 📁 Fermeture du modal', 'color: #2196F3');
        
        $('#appointmentModal').addClass('hidden');
        
        // Réinitialiser le formulaire
        if (typeof AppointmentModalController !== 'undefined' && AppointmentModalController.resetForm) {
            AppointmentModalController.resetForm();
        }
        
        // Émettre un événement personnalisé
        document.dispatchEvent(new CustomEvent('modalClosed'));
    }
};

// ============= CONTRÔLEUR JQUERY =============
const AppointmentModalController = {
    init: function() {
        $(document).ready(function() {
            AppointmentModalController.initJQuery();
        });
    },
    
    // Variables partagées
    resetCalendarFn: null,
    
    initJQuery: function() {
        
        // ============= CONFIGURATION =============
        const DEBUG = true; // Activer les logs de debug
        
        function log(message, type = 'info') {
            if (!DEBUG) return;
            
            const styles = {
                'info': 'color: #2196F3',
                'success': 'color: #4CAF50',
                'warning': 'color: #FF9800',
                'error': 'color: #F44336'
            };
            
            console.log(`%c[Appointment] ${message}`, styles[type] || styles.info);
        }
    
    // ============= VARIABLES GLOBALES =============
    let currentMonth = new Date().getMonth();
    let currentYear = new Date().getFullYear();
    let selectedDate = null;
    
    const monthNames = [
        'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
        'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'
    ];
    
    const dayNames = ['Dim', 'Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam'];
    
    // ============= FONCTIONS UTILITAIRES =============
    
    // Formater une date en YYYY-MM-DD
    function formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }
    
    // Vérifier si deux dates sont le même jour
    function isSameDay(date1, date2) {
        return date1.getFullYear() === date2.getFullYear() &&
               date1.getMonth() === date2.getMonth() &&
               date1.getDate() === date2.getDate();
    }
    
    // ============= GESTION DU CALENDRIER =============
    
    // Afficher le calendrier
    function renderCalendar() {
        log(`📅 Rendu du calendrier : ${monthNames[currentMonth]} ${currentYear}`);
        
        const firstDay = new Date(currentYear, currentMonth, 1).getDay();
        const lastDate = new Date(currentYear, currentMonth + 1, 0).getDate();
        const today = new Date();
        
        // Générer le calendrier complet avec navigation
        let html = `
            <div class="calendar-widget bg-white border rounded-lg p-4">
                <!-- En-tête avec navigation -->
                <div class="flex justify-between items-center mb-4">
                    <button type="button" id="prevMonth" class="p-2 hover:bg-gray-100 rounded">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                        </svg>
                    </button>
                    <h3 class="text-lg font-semibold text-gray-800">${monthNames[currentMonth]} ${currentYear}</h3>
                    <button type="button" id="nextMonth" class="p-2 hover:bg-gray-100 rounded">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                        </svg>
                    </button>
                </div>
                
                <!-- Calendrier -->
                <table class="w-full">
                    <thead>
                        <tr>`;
        
        // En-têtes des jours
        dayNames.forEach(day => {
            html += `<th class="text-center text-xs font-semibold text-gray-500 py-2">${day}</th>`;
        });
        html += '</tr></thead><tbody><tr>';
        
        // Cases vides avant le premier jour
        for (let i = 0; i < firstDay; i++) {
            html += '<td class="p-1"></td>';
        }
        
        // Jours du mois
        for (let day = 1; day <= lastDate; day++) {
            const date = new Date(currentYear, currentMonth, day);
            const isToday = isSameDay(date, today);
            const isPast = date < today && !isToday;
            const isWeekend = date.getDay() === 0 || date.getDay() === 6;
            
            let classes = 'w-8 h-8 text-sm rounded hover:bg-blue-100 transition-colors';
            if (isToday) classes += ' bg-blue-500 text-white font-bold';
            if (isPast) classes += ' text-gray-400 cursor-not-allowed';
            if (isWeekend) classes += ' text-red-400 cursor-not-allowed';
            if (!isPast && !isWeekend && !isToday) classes += ' text-gray-700 hover:bg-blue-50';
            
            // Désactiver les jours passés et week-ends
            const isDisabled = isPast || isWeekend;
            
            html += `<td class="p-1 text-center"><button type="button" class="calendar-day ${classes}" 
                     data-date="${formatDate(date)}" 
                     ${isDisabled ? 'disabled' : ''}>${day}</button></td>`;
            
            // Nouvelle ligne après samedi
            if ((firstDay + day) % 7 === 0 && day !== lastDate) {
                html += '</tr><tr>';
            }
        }
        
        html += '</tr></tbody></table></div>';
        $('#calendar').html(html);
        
        // Gérer les clics sur les jours (délégation d'événements)
        $(document).off('click', '.calendar-day:not([disabled])');
        $(document).on('click', '.calendar-day:not([disabled])', function() {
            const dateStr = $(this).data('date');
            selectDate(new Date(dateStr + 'T00:00:00'));
        });
        
        // Gérer la navigation entre les mois
        $(document).off('click', '#prevMonth');
        $(document).on('click', '#prevMonth', function() {
            changeMonth(-1);
        });
        
        $(document).off('click', '#nextMonth');
        $(document).on('click', '#nextMonth', function() {
            changeMonth(1);
        });
    }
    
    // Changer de mois
    function changeMonth(delta) {
        currentMonth += delta;
        if (currentMonth < 0) {
            currentMonth = 11;
            currentYear--;
        } else if (currentMonth > 11) {
            currentMonth = 0;
            currentYear++;
        }
        log(`📆 Navigation : ${monthNames[currentMonth]} ${currentYear}`);
        renderCalendar();
    }
    
    // Sélectionner une date
    function selectDate(date) {
        selectedDate = date;
        const dateStr = formatDate(date);
        
        log(`📌 Date sélectionnée : ${dateStr}`, 'success');
        
        // Mettre à jour l'input caché
        $('#appointmentDate').val(dateStr);
        
        // Mettre en évidence la date sélectionnée
        $('.calendar-day').removeClass('bg-green-500 text-white');
        $(`.calendar-day[data-date="${dateStr}"]`).addClass('bg-green-500 text-white');
        
        // Charger les créneaux pour cette date
        loadTimeSlots(date);
        
        // Mettre à jour le résumé
        updateSummary();
    }
    
    // ============= GESTION DES CRÉNEAUX HORAIRES =============
    
    // Générer les créneaux horaires fixes (fallback local)
    function generateTimeSlots(date) {
        const slots = ['09:00', '10:00', '11:00', '13:00', '14:00', '15:00', '16:00'];
        const now = new Date();
        
        // Si c'est aujourd'hui, filtrer les créneaux passés
        if (isSameDay(date, now)) {
            const currentHour = now.getHours();
            const currentMinutes = now.getMinutes();
            
            return slots.filter(slot => {
                const [hours, minutes] = slot.split(':').map(Number);
                // Garder seulement les créneaux au moins 1h dans le futur
                if (hours > currentHour + 1) return true;
                if (hours === currentHour + 1 && currentMinutes === 0) return true;
                return false;
            });
        }
        
        return slots;
    }
    
    // Simuler une disponibilité aléatoire
    function simulateAvailability(slots) {
        // Simuler que certains créneaux sont déjà pris
        return slots.filter(() => Math.random() > 0.3);
    }
    
    // Charger les créneaux disponibles
    function loadTimeSlots(date) {
        const dateStr = formatDate(date);
        log(`🔍 Chargement des créneaux pour : ${dateStr}`);
        
        // Nettoyer le conteneur
        $('#timeSlotsContainer').removeClass('hidden');
        
        // Masquer les anciens créneaux
        const existingSlots = $('#timeSlotsContainer .time-slot-btn');
        if (existingSlots.length > 0) {
            existingSlots.fadeOut(200, function() {
                $(this).remove();
            });
        }
        
        // Déterminer l'endpoint selon le contexte
        const isTestPage = window.location.pathname.includes('test');
        const endpoint = isTestPage ? '/appointments-test/available-slots' : '/appointments/available-slots';
        log(`🌐 Endpoint utilisé : ${endpoint}`);
        
        // Afficher un indicateur de chargement
        $('#timeSlotsContainer').html('<div class="text-center"><span class="spinner-border spinner-border-sm"></span> Chargement des créneaux...</div>');
        
        $.ajax({
            url: endpoint,
            method: 'GET',
            data: { date: dateStr },
            cache: true, // Activer le cache navigateur
            success: function(response) {
                log(`✅ Réponse API : ${response.length} créneaux reçus`, 'success');
                
                if (Array.isArray(response) && response.length > 0) {
                    log(`📅 Créneaux disponibles : ${response.join(', ')}`);
                    displayTimeSlots(response);
                } else if (response.slots) {
                    // Si la réponse est un objet avec une propriété slots
                    log(`📅 Créneaux disponibles : ${response.slots.join(', ')}`);
                    displayTimeSlots(response.slots);
                } else {
                    log('⚠️ Aucun créneau disponible depuis l\'API', 'warning');
                    displayTimeSlots([]);
                }
            },
            error: function(xhr, status, error) {
                log(`❌ Erreur API : ${error}`, 'error');
                log('🔄 Utilisation des créneaux générés localement', 'warning');
                // En cas d'erreur, utiliser les créneaux générés localement
                const slots = generateTimeSlots(date);
                displayTimeSlots(slots);
            }
        });
    }
    
    // Afficher les créneaux horaires
    function displayTimeSlots(slots) {
        const container = $('#timeSlotsContainer');
        
        log(`🎨 Affichage de ${slots.length} créneaux`);
        
        // Vider et réafficher le conteneur
        container.empty();
        
        if (!slots || slots.length === 0) {
            container.removeClass('hidden');
            container.html('<p class="text-gray-500 text-center col-span-4">Aucun créneau disponible pour cette date</p>');
            log('🔴 Aucun créneau à afficher', 'warning');
            return;
        }
        
        // Créer une grille pour les créneaux
        const slotsGrid = $('<div class="grid grid-cols-2 gap-2"></div>');
        
        slots.forEach(function(slot) {
            const slotBtn = $(`
                <button type="button" class="time-slot-btn bg-blue-50 hover:bg-blue-100 text-blue-800 font-medium py-2 px-3 rounded-lg border border-blue-200 transition-colors" data-time="${slot}">
                    🕐 ${slot}
                </button>
            `);
            
            slotBtn.on('click', function() {
                log(`🕐 Créneau sélectionné : ${slot}`, 'success');
                $('.time-slot-btn').removeClass('bg-green-500 text-white').addClass('bg-blue-50 text-blue-800');
                $(this).removeClass('bg-blue-50 text-blue-800').addClass('bg-green-500 text-white');
                $('#appointmentTime').val(slot);
                updateSummary();
            });
            
            slotsGrid.append(slotBtn);
        });
        
        // Remplacer le contenu du conteneur par la grille
        container.html(slotsGrid);
        container.removeClass('hidden');
        
        log('✓ Créneaux affichés avec succès', 'success');
    }
    
    // ============= GESTION DU RÉSUMÉ =============
    
        // Mettre à jour le résumé - FIXED: utiliser les bons IDs
        function updateSummary() {
            const name = $('#name').val();
            const service = $('#service').val();
            const date = $('#appointmentDate').val();
            const time = $('#appointmentTime').val();
            
            if (name || service || date || time) {
                $('#appointmentSummary').removeClass('hidden');
                const summaryContent = $('#summaryContent');
                summaryContent.empty();
                
                if (name) summaryContent.append(`<div>👤 <strong>Nom:</strong> ${name}</div>`);
                if (service) summaryContent.append(`<div>💼 <strong>Service:</strong> ${service}</div>`);
                if (date) summaryContent.append(`<div>📅 <strong>Date:</strong> ${date}</div>`);
                if (time) summaryContent.append(`<div>🕰 <strong>Heure:</strong> ${time}</div>`);
            } else {
                $('#appointmentSummary').addClass('hidden');
            }
            
            log('📝 Résumé mis à jour');
        }
    
        // Écouter les changements des champs - FIXED: utiliser les bons IDs
        $('#name, #service').on('input change', updateSummary);
    
    // ============= SOUMISSION DU FORMULAIRE =============
    
    $('#appointmentForm').on('submit', function(e) {
        e.preventDefault();
        log('📤 Soumission du formulaire de rendez-vous');
        
        // FIXED: utiliser les bons IDs
        const formData = {
            name: $('#name').val(),
            email: $('#email').val(),
            phone: $('#phone').val(),
            service: $('#service').val(),
            date: $('#appointmentDate').val(),
            time: $('#appointmentTime').val(),
            message: $('#message').val()
        };
        
        // Validation
        if (!formData.name || !formData.email || !formData.phone || 
            !formData.service || !formData.date || !formData.time) {
            log('⛔ Validation échouée : champs manquants', 'error');
            
            // Afficher un message d'erreur détaillé
            let missingFields = [];
            if (!formData.name) missingFields.push('Nom');
            if (!formData.email) missingFields.push('Email');
            if (!formData.phone) missingFields.push('Téléphone');
            if (!formData.service) missingFields.push('Service');
            if (!formData.date) missingFields.push('Date');
            if (!formData.time) missingFields.push('Créneau horaire');
            
            alert('Veuillez remplir tous les champs obligatoires :\n- ' + missingFields.join('\n- '));
            return;
        }
        
        log('📋 Données du formulaire :', 'info');
        console.table(formData);
        
        // Déterminer l'endpoint selon le contexte
        const isTestPage = window.location.pathname.includes('test');
        const endpoint = isTestPage ? '/appointments-test/create' : '/appointments/create';
        log(`🌐 Endpoint de création : ${endpoint}`);
        
        // Désactiver le bouton pendant l'envoi
        const submitBtn = $(this).find('button[type="submit"]');
        const originalText = submitBtn.html();
        submitBtn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm"></span> Envoi...');
        
        // Envoyer la requête AJAX
        log('🚀 Envoi de la requête...');
            // Récupération du token CSRF
            const csrfToken = $('meta[name="_csrf"]').attr('content');
            const csrfHeader = $('meta[name="_csrf_header"]').attr('content');
            
            $.ajax({
                url: endpoint,
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(formData),
                beforeSend: function(xhr) {
                    if (csrfToken && csrfHeader) {
                        xhr.setRequestHeader(csrfHeader, csrfToken);
                    }
                },
            success: function(response) {
                log('✅ Succès !', 'success');
                console.log('Réponse serveur :', response);
                
                // Afficher un message de succès
                const successAlert = $(`
                    <div class="alert alert-success alert-dismissible fade show" role="alert">
                        <strong>Succès !</strong> Votre rendez-vous a été créé avec succès.
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                `);
                $('#appointmentForm').prepend(successAlert);
                
                // Réinitialiser et fermer après 2 secondes
                setTimeout(function() {
                    AppointmentModal.close();
                    $('#appointmentForm')[0].reset();
                    resetCalendar();
                    successAlert.remove();
                }, 2000);
            },
            error: function(xhr) {
                log('❌ Erreur lors de la création', 'error');
                console.error('Erreur :', xhr);
                
                const message = xhr.responseJSON ? xhr.responseJSON.message : 'Une erreur est survenue';
                
                // Afficher un message d'erreur
                const errorAlert = $(`
                    <div class="alert alert-danger alert-dismissible fade show" role="alert">
                        <strong>Erreur !</strong> ${message}
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                `);
                $('#appointmentForm').prepend(errorAlert);
                
                // Retirer l'alerte après 5 secondes
                setTimeout(function() {
                    errorAlert.remove();
                }, 5000);
            },
            complete: function() {
                // Réactiver le bouton
                submitBtn.prop('disabled', false).html(originalText);
            }
        });
    });
    
        // ============= RÉINITIALISATION =============
        
        function resetCalendar() {
            currentMonth = new Date().getMonth();
            currentYear = new Date().getFullYear();
            selectedDate = null;
            $('#appointmentDate').val('');
            $('#appointmentTime').val('');
            $('#timeSlotsContainer').empty();
            renderCalendar();
            log('🔄 Calendrier réinitialisé');
        }
        
        // Rendre la fonction accessible depuis l'extérieur
        AppointmentModalController.resetCalendarFn = resetCalendar;
    
    // ============= INITIALISATION =============
    
    log('🎯 Initialisation du modal de rendez-vous v2.0', 'success');
    renderCalendar();
    
    // Réinitialiser le modal à sa fermeture
    $('#appointmentModal').on('hidden.bs.modal', function() {
        log('🚪 Modal fermé - réinitialisation');
        $('#appointmentForm')[0].reset();
        resetCalendar();
        // Retirer les alertes éventuelles
        $('#appointmentForm .alert').remove();
    });
    
    // Afficher un message au chargement
    $('#appointmentModal').on('shown.bs.modal', function() {
        log('🎉 Modal ouvert', 'success');
    });
    
        log('✅ Modal de rendez-vous prêt !', 'success');
    },
    
    // Méthode pour réinitialiser le formulaire
    resetForm: function() {
        $('#appointmentForm')[0].reset();
        // Réinitialiser le calendrier si la fonction existe
        if (this.resetCalendarFn && typeof this.resetCalendarFn === 'function') {
            this.resetCalendarFn();
        }
        // Retirer les alertes éventuelles
        $('#appointmentForm .alert').remove();
        console.log('%c[AppointmentModal] 🔄 Formulaire réinitialisé', 'color: #2196F3');
    },
    
    // Méthode pour soumettre le formulaire de rendez-vous
    submitAppointment: function() {
        $('#appointmentForm').trigger('submit');
    }
};

// ============= INITIALISATION GLOBALE =============
// Auto-initialiser quand le DOM est chargé
document.addEventListener('DOMContentLoaded', function() {
    if (typeof AppointmentModal !== 'undefined') {
        AppointmentModal.init();
    }
});
