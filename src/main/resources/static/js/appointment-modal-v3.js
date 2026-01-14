/**
 * Gestion du modal de prise de rendez-vous avec calendrier v3
 * - Gestion du service "Autre" avec message obligatoire
 * - Validation améliorée avec filtrage de mots inappropriés
 * - Support des règles métier configurées
 */

const AppointmentModal = {
    // État du modal
    selectedDate: null,
    selectedTime: null,
    hasUserInteracted: false, // Pour savoir si l'utilisateur a commencé à remplir le formulaire
    
    // Initialisation
    init() {
        console.log('[AppointmentModal] 🚀 Initialisation v3.2');
        
        // Initialiser les événements
        this.bindEvents();
        
        // Initialiser le calendrier
        this.initCalendar();
        
        // Gérer le service "Autre"
        this.handleServiceChange();
        
        console.log('[AppointmentModal] ✅ Modal prêt');
    },
    
    // Ouvrir le modal
    open() {
        console.log('[AppointmentModal] 📂 Ouverture du modal');
        
        // D'abord, assurons-nous que le modal est visible
        $('#appointmentModal').removeClass('hidden');
        
        // Ensuite, réinitialisons complètement le formulaire
        this.resetForm();
        
        // Puis pré-remplir si l'utilisateur est connecté
        if (window.AUTH_INFO && window.AUTH_INFO.isAuthenticated) {
            this.prefillUserData();
        }
    },
    
    // Fermer le modal
    close() {
        console.log('[AppointmentModal] 📁 Fermeture du modal');
        $('#appointmentModal').addClass('hidden');
        this.resetForm();
    },
    
    // Pré-remplir les données utilisateur
    prefillUserData() {
        if (window.AUTH_INFO && window.AUTH_INFO.isAuthenticated && window.AUTH_INFO.user) {
            const user = window.AUTH_INFO.user;
            console.log('[AppointmentModal] 📄 Pré-remplissage des données utilisateur');
            
            // Pré-remplir le nom complet
            const fullName = `${user.firstName || ''} ${user.lastName || ''}`.trim();
            $('#appointmentName').val(fullName);
            
            // Pré-remplir l'email et le rendre en lecture seule
            $('#appointmentEmail').val(user.email || '').prop('readonly', true).addClass('bg-gray-50');
            
            // Pré-remplir le téléphone si disponible
            $('#appointmentPhone').val(user.phone || '');
        }
    },
    
    // Réinitialiser le formulaire
    resetForm() {
        console.log('[AppointmentModal] 🧩 Réinitialisation du formulaire');
        
        // Réinitialiser le formulaire HTML standard
        $('#appointmentForm')[0].reset();
        
        // Vider explicitement tous les champs pour être sûr
        $('#appointmentName').val('');
        $('#appointmentEmail').val('').prop('readonly', false).removeClass('bg-gray-50');
        $('#appointmentPhone').val('');
        $('#appointmentService').val('');
        $('#appointmentMessageField').val('');
        $('#appointmentDate').val('');
        $('#appointmentTime').val('');
        
        // Réinitialiser l'état du modal
        this.selectedDate = null;
        this.selectedTime = null;
        this.hasUserInteracted = false;
        
        // Masquer les éléments
        $('#appointmentSummary').addClass('hidden');
        $('#appointmentSubmitBtn').prop('disabled', true);
        
        // Nettoyer le calendrier et les créneaux
        $('.calendar-day').removeClass('bg-green-500 text-white bg-red-500');
        $('.time-slot-btn').removeClass('bg-green-500 text-white');
        $('#timeSlotsContainer').addClass('hidden').html('<div class="text-gray-500 text-center">Sélectionnez une date pour voir les créneaux disponibles</div>');
        
        // Masquer toutes les erreurs
        this.hideAllErrors();
        
        console.log('[AppointmentModal] ✅ Formulaire réinitialisé');
    },
    
    // Cacher toutes les erreurs
    hideAllErrors() {
        $('.text-red-500').addClass('hidden').text('');
        $('#appointmentMessage').addClass('hidden');
    },
    
    // Convertir les noms de champs techniques en noms lisibles
    getFieldDisplayName(fieldName) {
        const fieldNames = {
            'name': 'Nom',
            'email': 'Email',
            'phone': 'Téléphone',
            'service': 'Service',
            'date': 'Date',
            'time': 'Heure',
            'message': 'Message'
        };
        return fieldNames[fieldName] || fieldName;
    },
    
    // Afficher une erreur
    showError(fieldId, message) {
        $(`#${fieldId}Error`).removeClass('hidden').text(message);
    },
    
    // Gérer le changement de service
    handleServiceChange() {
        $('#appointmentService').on('change', function() {
            const service = $(this).val();
            const messageField = $('#appointmentMessageField');
            const messageLabel = messageField.prev('label');
            
            if (service === 'Autre') {
                // Rendre le message obligatoire pour "Autre"
                messageLabel.html('Message <span class="text-red-500">*</span> <small class="text-gray-500">(Décrivez votre besoin en détail)</small>');
                messageField.attr('required', true);
                messageField.attr('minlength', 20);
                messageField.attr('placeholder', 'Décrivez précisément votre besoin professionnel (minimum 20 caractères)...');
            } else {
                // Message optionnel pour les autres services
                messageLabel.html('Message (optionnel)');
                messageField.removeAttr('required');
                messageField.removeAttr('minlength');
                messageField.attr('placeholder', 'Décrivez brièvement votre projet ou vos besoins spécifiques...');
            }
        });
    },
    
    // Valider les mots inappropriés
    containsInappropriateWords(text) {
        const inappropriateWords = [
            'gratuit', 'urgent', 'rapide', 'immédiat', 'arnaque',
            'scam', 'hack', 'crack', 'pirate', 'illegal',
            'casino', 'pari', 'jeu', 'poker', 'sexe',
            'drogue', 'alcool', 'cigarette', 'violence', 'arme'
        ];
        
        const lowerText = text.toLowerCase();
        return inappropriateWords.some(word => lowerText.includes(word));
    },
    
    // Valider le formulaire
    validateForm(forceValidation = false) {
        // Ne pas valider si l'utilisateur n'a pas encore interagi, sauf si forcé (lors de la soumission)
        if (!this.hasUserInteracted && !forceValidation) {
            return true;
        }
        
        this.hideAllErrors();
        let isValid = true;
        
        // Nom
        const name = $('#appointmentName').val().trim();
        if (!name) {
            this.showError('appointmentName', 'Le nom est obligatoire');
            isValid = false;
        } else if (this.containsInappropriateWords(name)) {
            this.showError('appointmentName', 'Le nom contient des termes inappropriés');
            isValid = false;
        }
        
        // Email
        const email = $('#appointmentEmail').val().trim();
        if (!email) {
            this.showError('appointmentEmail', 'L\'email est obligatoire');
            isValid = false;
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            this.showError('appointmentEmail', 'Email invalide');
            isValid = false;
        }
        
        // Téléphone
        const phone = $('#appointmentPhone').val().trim();
        if (!phone) {
            this.showError('appointmentPhone', 'Le téléphone est obligatoire');
            isValid = false;
        }
        
        // Service
        const service = $('#appointmentService').val();
        if (!service) {
            this.showError('appointmentService', 'Veuillez sélectionner un service');
            isValid = false;
        }
        
        // Message (obligatoire si service = Autre)
        const message = $('#appointmentMessageField').val().trim();
        if (service === 'Autre') {
            if (!message) {
                this.showError('appointmentMessageField', 'Le message est obligatoire pour le service "Autre"');
                isValid = false;
            } else if (message.length < 20) {
                this.showError('appointmentMessageField', 'Le message doit contenir au moins 20 caractères');
                isValid = false;
            } else if (this.containsInappropriateWords(message)) {
                this.showError('appointmentMessageField', 'Le message contient des termes inappropriés');
                isValid = false;
            }
        }
        
        // Date et heure
        if (!this.selectedDate) {
            isValid = false;
            this.showMessage('Veuillez sélectionner une date', 'error');
        } else if (!this.isDateWithinAllowedRange(this.selectedDate)) {
            isValid = false;
            this.showMessage('La date sélectionnée est trop éloignée. Veuillez choisir une date dans les 30 prochains jours ouvrables.', 'error');
        }
        
        if (!this.selectedTime) {
            isValid = false;
            this.showMessage('Veuillez sélectionner un créneau horaire', 'error');
        }
        
        return isValid;
    },
    
    // Afficher un message
    showMessage(message, type = 'info') {
        const messageDiv = $('#appointmentMessage');
        messageDiv.removeClass('hidden bg-green-100 bg-red-100 bg-blue-100 text-green-800 text-red-800 text-blue-800');
        
        if (type === 'success') {
            messageDiv.addClass('bg-green-100 text-green-800');
        } else if (type === 'error') {
            messageDiv.addClass('bg-red-100 text-red-800');
        } else {
            messageDiv.addClass('bg-blue-100 text-blue-800');
        }
        
        messageDiv.html(message);
    },
    
    // Soumettre le rendez-vous
    submitAppointment() {
        console.log('[AppointmentModal] 📤 Soumission du rendez-vous');
        
        // Marquer que l'utilisateur a interagi et forcer la validation
        this.hasUserInteracted = true;
        if (!this.validateForm(true)) {
            return;
        }
        
        const formData = {
            name: $('#appointmentName').val().trim(),
            email: $('#appointmentEmail').val().trim(),
            phone: $('#appointmentPhone').val().trim(),
            service: $('#appointmentService').val(),
            date: $('#appointmentDate').val(),
            time: $('#appointmentTime').val(),
            message: $('#appointmentMessageField').val().trim()
        };
        
        console.log('[AppointmentModal] 📋 Données:', formData);
        
        // Déterminer l'endpoint
        const isTestPage = window.location.pathname.includes('test');
        const endpoint = isTestPage ? '/appointments-test/create' : '/appointments/create';
        
        // Désactiver le bouton
        const submitBtn = $('#appointmentSubmitBtn');
        submitBtn.prop('disabled', true);
        $('#appointmentSubmitText').addClass('hidden');
        $('#appointmentSubmitLoader').removeClass('hidden');
        
        // Token CSRF
        const csrfToken = $('input[name="_csrf"]').val() || $('meta[name="_csrf"]').attr('content');
        const csrfHeader = $('meta[name="_csrf_header"]').attr('content') || 'X-CSRF-TOKEN';
        
        $.ajax({
            url: endpoint,
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(formData),
            beforeSend: function(xhr) {
                if (csrfToken) {
                    xhr.setRequestHeader(csrfHeader, csrfToken);
                }
            },
            success: (response) => {
                console.log('[AppointmentModal] ✅ Succès:', response);
                this.showMessage('✅ Votre rendez-vous a été créé avec succès ! Vous recevrez un email de confirmation.', 'success');
                
                setTimeout(() => {
                    this.close();
                }, 3000);
            },
            error: (xhr) => {
                console.error('[AppointmentModal] ❌ Erreur:', xhr);
                const response = xhr.responseJSON || {};
                const errorMsg = response.message || 'Une erreur est survenue';
                
                // Afficher les erreurs de validation spécifiques si elles existent
                if (response.data && Array.isArray(response.data) && response.data.length > 0) {
                    let errorHtml = '<strong>❌ ' + errorMsg + '</strong><ul class="mt-2 list-disc pl-5 text-sm">';
                    
                    // Parcourir toutes les erreurs et les afficher
                    response.data.forEach(error => {
                        const fieldName = error.field;
                        const fieldError = error.defaultMessage;
                        
                        // Ajouter l'erreur à la liste
                        errorHtml += `<li><strong>${this.getFieldDisplayName(fieldName)}</strong>: ${fieldError}</li>`;
                        
                        // Marquer le champ correspondant comme invalide
                        this.showError('appointment' + fieldName.charAt(0).toUpperCase() + fieldName.slice(1), fieldError);
                    });
                    
                    errorHtml += '</ul>';
                    this.showMessage(errorHtml, 'error');
                } else {
                    this.showMessage('❌ ' + errorMsg, 'error');
                }
            },
            complete: () => {
                submitBtn.prop('disabled', false);
                $('#appointmentSubmitText').removeClass('hidden');
                $('#appointmentSubmitLoader').addClass('hidden');
            }
        });
    },
    
    // Initialiser le calendrier
    initCalendar() {
        const today = new Date();
        let currentMonth = today.getMonth();
        let currentYear = today.getFullYear();
        
        const monthNames = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
                          'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];
        
        const renderCalendar = () => {
            const firstDay = new Date(currentYear, currentMonth, 1).getDay();
            const lastDate = new Date(currentYear, currentMonth + 1, 0).getDate();
            
            $('#currentMonth').text(monthNames[currentMonth] + ' ' + currentYear);
            
            let html = '';
            
            // Jours vides avant le premier jour
            for (let i = 0; i < firstDay; i++) {
                html += '<div></div>';
            }
            
            // Jours du mois
            for (let day = 1; day <= lastDate; day++) {
                const date = new Date(currentYear, currentMonth, day);
                const dateStr = this.formatDate(date);
                const isToday = this.isSameDay(date, today);
                const isPast = date < today && !isToday;
                const isWeekend = date.getDay() === 0 || date.getDay() === 6;
                const isTooFar = !this.isDateWithinAllowedRange(dateStr);
                
                let classes = 'p-2 text-center cursor-pointer rounded-lg transition-colors ';
                let title = '';
                
                if (isPast || isWeekend) {
                    classes += 'bg-gray-100 text-gray-400 cursor-not-allowed';
                    title = isWeekend ? 'Week-end' : 'Date passée';
                } else if (isTooFar) {
                    classes += 'bg-orange-100 text-orange-400 hover:bg-orange-200';
                    title = 'Date trop éloignée (> 30 jours ouvrables)';
                } else if (isToday) {
                    classes += 'bg-blue-500 text-white font-bold hover:bg-blue-600';
                    title = "Aujourd'hui";
                } else {
                    classes += 'hover:bg-blue-100';
                    title = 'Disponible';
                }
                
                const disabled = isPast || isWeekend ? 'data-disabled="true"' : '';
                
                html += `<div class="calendar-day ${classes}" data-date="${dateStr}" ${disabled} title="${title}">${day}</div>`;
            }
            
            $('#calendarDays').html(html);
        };
        
        // Naviguer entre les mois
        $('#prevMonth').on('click', () => {
            currentMonth--;
            if (currentMonth < 0) {
                currentMonth = 11;
                currentYear--;
            }
            renderCalendar();
        });
        
        $('#nextMonth').on('click', () => {
            // Limiter à 3 mois dans le futur
            const maxDate = new Date();
            maxDate.setMonth(maxDate.getMonth() + 3);
            
            const nextMonth = currentMonth + 1;
            const nextYear = nextMonth > 11 ? currentYear + 1 : currentYear;
            const nextMonthNormalized = nextMonth > 11 ? 0 : nextMonth;
            
            if (new Date(nextYear, nextMonthNormalized, 1) <= maxDate) {
                currentMonth = nextMonthNormalized;
                currentYear = nextYear;
                renderCalendar();
            }
        });
        
        // Sélectionner une date
        $(document).on('click', '.calendar-day:not([data-disabled="true"])', (e) => {
            const dateStr = $(e.target).data('date');
            this.selectDate(dateStr);
        });
        
        renderCalendar();
    },
    
    // Sélectionner une date
    selectDate(dateStr) {
        console.log('[AppointmentModal] 📅 Date sélectionnée:', dateStr);
        
        // Vérifier si la date est dans la plage autorisée
        if (!this.isDateWithinAllowedRange(dateStr)) {
            const businessDays = this.calculateBusinessDaysBetween(new Date(), new Date(dateStr));
            console.log('[AppointmentModal] ⚠️ Date trop éloignée:', businessDays, 'jours ouvrables');
            
            // Afficher un message d'erreur
            this.showMessage(
                '⚠️ La date sélectionnée est trop éloignée. Veuillez choisir une date dans les 30 prochains jours ouvrables.',
                'error'
            );
            
            // Marquer visuellement la date comme invalide
            $('.calendar-day').removeClass('bg-green-500 text-white bg-red-500');
            $(`.calendar-day[data-date="${dateStr}"]`).addClass('bg-red-500 text-white');
            
            // Désactiver le bouton de soumission
            $('#appointmentSubmitBtn').prop('disabled', true);
            
            // Ne pas charger les créneaux
            $('#timeSlotsContainer').html('<div class="text-red-500 text-center py-4">⚠️ Veuillez sélectionner une date dans les 30 prochains jours ouvrables</div>');
            
            this.selectedDate = null;
            $('#appointmentDate').val('');
            
            return;
        }
        
        // La date est valide, continuer normalement
        this.selectedDate = dateStr;
        $('#appointmentDate').val(dateStr);
        
        // Mettre en évidence la date sélectionnée
        $('.calendar-day').removeClass('bg-green-500 text-white bg-red-500');
        $(`.calendar-day[data-date="${dateStr}"]`).addClass('bg-green-500 text-white');
        
        // Charger les créneaux
        this.loadTimeSlots(dateStr);
        
        // Mettre à jour le résumé
        this.updateSummary();
    },
    
    // Charger les créneaux horaires
    loadTimeSlots(dateStr) {
        console.log('[AppointmentModal] 🔍 Chargement des créneaux pour:', dateStr);
        
        const container = $('#timeSlotsContainer');
        // Retirer la classe hidden pour afficher le conteneur
        container.removeClass('hidden');
        container.html('<div class="text-center py-4">⏳ Chargement des créneaux...</div>');
        
        const isTestPage = window.location.pathname.includes('test');
        const endpoint = isTestPage ? '/appointments-test/available-slots' : '/appointments/available-slots';
        
        $.ajax({
            url: endpoint,
            method: 'GET',
            data: { date: dateStr },
            success: (slots) => {
                console.log('[AppointmentModal] ✅ Créneaux reçus:', slots);
                this.displayTimeSlots(slots);
            },
            error: (xhr) => {
                console.error('[AppointmentModal] ❌ Erreur chargement créneaux:', xhr);
                container.html('<div class="text-red-500 text-center">Erreur de chargement des créneaux</div>');
            }
        });
    },
    
    // Afficher les créneaux
    displayTimeSlots(slots) {
        const container = $('#timeSlotsContainer');
        // S'assurer que le conteneur est visible
        container.removeClass('hidden');
        
        if (!slots || slots.length === 0) {
            container.html('<div class="text-gray-500 text-center py-4">Aucun créneau disponible pour cette date</div>');
            return;
        }
        
        // Ajouter un titre
        let html = '<h5 class="text-sm font-semibold text-gray-700 mb-4">⏰ Créneaux disponibles</h5>';
        
        // Séparer matin et après-midi
        const morningSlots = slots.filter(s => parseInt(s.split(':')[0]) < 12);
        const afternoonSlots = slots.filter(s => parseInt(s.split(':')[0]) >= 12);
        
        // Conteneur principal : sections empilées
        html += '<div class="space-y-6">';
        
        // Section Matin
        if (morningSlots.length > 0) {
            html += '<div>';
            html += '<h6 class="text-xs font-medium text-gray-500 mb-3">🌅 Matin</h6>';
            html += '<div class="grid grid-cols-4 gap-3">';
            morningSlots.forEach(slot => {
                html += `<button type="button" class="time-slot-btn text-center text-xs bg-blue-50 hover:bg-blue-100 text-blue-800 font-medium py-2 px-2 rounded-lg border border-blue-200 transition-all" data-time="${slot}">${slot}</button>`;
            });
            html += '</div>';
            html += '</div>';
        }
        
        // Section Après-midi
        if (afternoonSlots.length > 0) {
            html += '<div>';
            html += '<h6 class="text-xs font-medium text-gray-500 mb-3">☀️ Après-midi</h6>';
            html += '<div class="grid grid-cols-4 gap-3">';
            afternoonSlots.forEach(slot => {
                html += `<button type="button" class="time-slot-btn text-center text-xs bg-blue-50 hover:bg-blue-100 text-blue-800 font-medium py-2 px-2 rounded-lg border border-blue-200 transition-all" data-time="${slot}">${slot}</button>`;
            });
            html += '</div>';
            html += '</div>';
        }
        
        html += '</div>';
        container.html(html);
        
        // Gérer la sélection des créneaux
        $('.time-slot-btn').on('click', (e) => {
            const time = $(e.target).data('time');
            this.selectTimeSlot(time, e.target);
        });
    },
    
    // Sélectionner un créneau
    selectTimeSlot(time, button) {
        console.log('[AppointmentModal] ⏰ Créneau sélectionné:', time);
        
        this.selectedTime = time;
        $('#appointmentTime').val(time);
        
        // Mettre en évidence le créneau sélectionné
        $('.time-slot-btn').removeClass('bg-green-500 text-white').addClass('bg-blue-50 text-blue-800');
        $(button).removeClass('bg-blue-50 text-blue-800').addClass('bg-green-500 text-white');
        
        // Activer le bouton de soumission
        $('#appointmentSubmitBtn').prop('disabled', false);
        
        // Mettre à jour le résumé
        this.updateSummary();
    },
    
    // Mettre à jour le résumé
    updateSummary() {
        if (this.selectedDate || this.selectedTime) {
            const summaryHtml = [];
            
            if ($('#appointmentService').val()) {
                summaryHtml.push(`<div>💼 Service: <strong>${$('#appointmentService').val()}</strong></div>`);
            }
            if (this.selectedDate) {
                summaryHtml.push(`<div>📅 Date: <strong>${this.selectedDate}</strong></div>`);
            }
            if (this.selectedTime) {
                summaryHtml.push(`<div>⏰ Heure: <strong>${this.selectedTime}</strong></div>`);
            }
            
            $('#summaryContent').html(summaryHtml.join(''));
            $('#appointmentSummary').removeClass('hidden');
        }
    },
    
    // Utilitaires
    formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    },
    
    isSameDay(date1, date2) {
        return date1.getFullYear() === date2.getFullYear() &&
               date1.getMonth() === date2.getMonth() &&
               date1.getDate() === date2.getDate();
    },
    
    // Calculer le nombre de jours ouvrables entre deux dates
    calculateBusinessDaysBetween(startDate, endDate) {
        let businessDays = 0;
        let currentDate = new Date(startDate);
        currentDate.setDate(currentDate.getDate() + 1); // Commencer le lendemain
        
        while (currentDate <= endDate) {
            const dayOfWeek = currentDate.getDay();
            // 0 = Dimanche, 6 = Samedi
            if (dayOfWeek !== 0 && dayOfWeek !== 6) {
                businessDays++;
            }
            currentDate.setDate(currentDate.getDate() + 1);
        }
        
        return businessDays;
    },
    
    // Vérifier si une date est dans la plage autorisée (30 jours ouvrables)
    isDateWithinAllowedRange(selectedDate) {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        const selected = new Date(selectedDate);
        selected.setHours(0, 0, 0, 0);
        
        const businessDays = this.calculateBusinessDaysBetween(today, selected);
        return businessDays <= 30;
    },
    
    // Lier les événements
    bindEvents() {
        // Fermer avec l'overlay
        $('#appointmentModal').on('click', (e) => {
            if (e.target.id === 'appointmentModal') {
                this.close();
            }
        });
        
        // Détecter quand l'utilisateur commence à interagir
        $('#appointmentName, #appointmentEmail, #appointmentPhone, #appointmentService, #appointmentMessageField').on('input change', () => {
            this.hasUserInteracted = true;
        });
        
        // Pas de validation automatique - seulement lors de la soumission
    }
};

// Initialiser au chargement de la page
$(document).ready(() => {
    AppointmentModal.init();
    
    // Rendre le modal accessible globalement
    window.AppointmentModal = AppointmentModal;
    
    console.log('[AppointmentModal] 🎯 Modal disponible globalement');
});
