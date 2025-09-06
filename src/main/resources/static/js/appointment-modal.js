/**
 * 🗓️ MODAL DE RENDEZ-VOUS UNIFIÉ AVEC CALENDRIER - LMP
 * 
 * Script unifié pour gérer le modal de rendez-vous sur toutes les pages
 * Fonctionnalités: calendrier interactif, créneaux horaires, validation, soumission AJAX
 * 
 * @author LMP Team
 * @version 2.0.0 - Version avec calendrier complet
 */

console.log('📅 Chargement du script de modal de rendez-vous avec calendrier...');

// ============================================
// VARIABLES GLOBALES ET CONFIGURATION
// ============================================

window.AppointmentModal = {
    isOpen: false,
    isLoading: false,
    
    // État du calendrier
    currentDate: new Date(),
    selectedDate: null,
    selectedTime: null,
    availableSlots: [],
    
    // Gestion du redimensionnement
    resizeTimeout: null,
    lastViewportSize: null,
    
    // Configuration
    config: {
        modalId: 'appointmentModal',
        formId: 'appointmentForm',
        debug: true,
        // Configuration du calendrier
        calendar: {
            minDate: new Date(),
            maxDate: new Date(Date.now() + 60 * 24 * 60 * 60 * 1000), // 60 jours
            availableDays: [1, 2, 3, 4, 5], // Lundi à vendredi (0 = dimanche, 6 = samedi)
            timeSlots: [
                '09:00', '09:30', '10:00', '10:30', '11:00', '11:30',
                '13:00', '13:30', '14:00', '14:30', '15:00', '15:30', '16:00', '16:30'
            ]
        }
    },
    
    // Éléments DOM (cachés pour performance)
    elements: {},
    
    /**
     * Initialisation du modal
     */
    init: function() {
        this.log('🚀 Initialisation du modal de rendez-vous...');
        
        // Cache des éléments DOM
        this.cacheElements();
        
        // Configuration des événements
        this.bindEvents();
        
        // Initialisation du calendrier
        this.initCalendar();
        
        this.log('✅ Modal de rendez-vous avec calendrier initialisé avec succès');
    },
    
    /**
     * Cache les éléments DOM pour optimiser les performances
     */
    cacheElements: function() {
        this.elements = {
            modal: document.getElementById(this.config.modalId),
            form: document.getElementById(this.config.formId),
            submitBtn: document.getElementById('appointmentSubmitBtn'),
            submitText: document.getElementById('appointmentSubmitText'),
            submitLoader: document.getElementById('appointmentSubmitLoader'),
            messageContainer: document.getElementById('appointmentMessage'),
            
            // Champs du formulaire
            name: document.getElementById('appointmentName'),
            email: document.getElementById('appointmentEmail'),
            phone: document.getElementById('appointmentPhone'),
            service: document.getElementById('appointmentService'),
            message: document.getElementById('appointmentMessageField'),
            
            // Conteneurs d'erreurs
            nameError: document.getElementById('appointmentNameError'),
            emailError: document.getElementById('appointmentEmailError'),
            phoneError: document.getElementById('appointmentPhoneError'),
            serviceError: document.getElementById('appointmentServiceError'),
            
            // Éléments du calendrier
            calendar: document.getElementById('appointmentCalendar'),
            calendarDays: document.getElementById('calendarDays'),
            currentMonth: document.getElementById('currentMonth'),
            prevMonth: document.getElementById('prevMonth'),
            nextMonth: document.getElementById('nextMonth'),
            timeSlots: document.getElementById('appointmentTimeSlots'),
            timeSlotsContainer: document.getElementById('timeSlotsContainer'),
            selectedDateTime: document.getElementById('selectedDateTime'),
            summary: document.getElementById('appointmentSummary'),
            summaryContent: document.getElementById('summaryContent')
        };
        
        this.log('📦 Éléments DOM cachés:', {
            modal: !!this.elements.modal,
            form: !!this.elements.form,
            calendar: !!this.elements.calendar,
            fieldsFound: Object.keys(this.elements).length
        });
    },
    
    /**
     * Configuration des événements
     */
    bindEvents: function() {
        // Événement de soumission du formulaire
        if (this.elements.form) {
            this.elements.form.addEventListener('submit', (e) => {
                e.preventDefault();
                this.submitAppointment();
            });
        }
        
        // Fermeture du modal en cliquant à l'extérieur
        if (this.elements.modal) {
            this.elements.modal.addEventListener('click', (e) => {
                if (e.target === this.elements.modal) {
                    this.close();
                }
            });
        }
        
        // Fermeture avec la touche Escape
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.isOpen) {
                this.close();
            }
        });
        
        // Événements du calendrier
        if (this.elements.prevMonth) {
            this.elements.prevMonth.addEventListener('click', () => this.previousMonth());
        }
        
        if (this.elements.nextMonth) {
            this.elements.nextMonth.addEventListener('click', () => this.nextMonth());
        }
        
        // Gestion du redimensionnement
        window.addEventListener('resize', this.debounce(() => this.handleResize(), 100));
        window.addEventListener('orientationchange', () => {
            setTimeout(() => this.handleOrientationChange(), 100);
        });
        
        this.log('🎛️ Événements configurés');
    },
    
    /**
     * Initialisation du calendrier
     */
    initCalendar: function() {
        this.log('📅 Initialisation du calendrier...');
        this.renderCalendar();
    },
    
    /**
     * Rendre le calendrier pour le mois actuel
     */
    renderCalendar: function() {
        const year = this.currentDate.getFullYear();
        const month = this.currentDate.getMonth();
        
        // Mettre à jour le titre du mois
        const monthNames = [
            'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
            'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'
        ];
        this.elements.currentMonth.textContent = `${monthNames[month]} ${year}`;
        
        // Calculer le premier jour du mois et le nombre de jours
        const firstDay = new Date(year, month, 1);
        const lastDay = new Date(year, month + 1, 0);
        const daysInMonth = lastDay.getDate();
        const startingDayOfWeek = firstDay.getDay();
        
        // Créer la grille des jours
        let daysHTML = '';
        
        // Jours vides au début
        for (let i = 0; i < startingDayOfWeek; i++) {
            daysHTML += '<div class="p-2"></div>';
        }
        
        // Jours du mois
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        for (let day = 1; day <= daysInMonth; day++) {
            const currentDateObj = new Date(year, month, day);
            const dayOfWeek = currentDateObj.getDay();
            const isPastDate = currentDateObj < today;
            const isWeekend = dayOfWeek === 0 || dayOfWeek === 6; // Dimanche ou samedi
            const isAvailable = !isPastDate && !isWeekend;
            
            let dayClass = 'day-cell p-2 text-center cursor-pointer rounded-lg transition-all duration-200 ';
            
            if (isPastDate) {
                // Jours passés : même style que les week-ends (non disponibles)
                dayClass += 'text-gray-400 bg-gray-100 cursor-not-allowed opacity-60';
            } else if (isWeekend) {
                // Week-ends : non disponibles
                dayClass += 'text-gray-400 bg-gray-100 cursor-not-allowed opacity-60';
            } else {
                // Jours disponibles
                dayClass += 'text-gray-900 hover:bg-blue-100 hover:text-blue-600 border border-transparent hover:border-blue-300';
            }
            
            // Jour sélectionné
            if (this.selectedDate && 
                this.selectedDate.getDate() === day && 
                this.selectedDate.getMonth() === month && 
                this.selectedDate.getFullYear() === year) {
                dayClass += ' !bg-blue-500 !text-white !border-blue-600';
            }
            
            const clickHandler = isAvailable ? `onclick="AppointmentModal.selectDate(${year}, ${month}, ${day})"` : '';
            
            daysHTML += `<div class="${dayClass}" ${clickHandler} data-date="${year}-${month}-${day}">
                            ${day}
                         </div>`;
        }
        
        this.elements.calendarDays.innerHTML = daysHTML;
        this.log('📅 Calendrier rendu pour', monthNames[month], year);
    },
    
    /**
     * Sélectionner une date
     */
    selectDate: function(year, month, day) {
        const selectedDate = new Date(year, month, day);
        const dayOfWeek = selectedDate.getDay();
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        // Vérifier si la date est valide
        if (selectedDate < today || dayOfWeek === 0 || dayOfWeek === 6) {
            this.log('❌ Date non disponible:', selectedDate);
            return;
        }
        
        this.selectedDate = selectedDate;
        this.selectedTime = null; // Réinitialiser l'heure sélectionnée
        
        this.log('📅 Date sélectionnée:', selectedDate);
        
        // Re-rendre le calendrier pour mettre à jour la sélection
        this.renderCalendar();
        
        // Charger les créneaux horaires
        this.loadTimeSlots();
        
        // Mettre à jour le résumé
        this.updateSummary();
    },
    
    /**
     * Charger les créneaux horaires pour la date sélectionnée
     */
    loadTimeSlots: function() {
        if (!this.selectedDate) return;
        
        this.log('🕐 Chargement des créneaux horaires...');
        
        const dateStr = this.selectedDate.toISOString().split('T')[0];
        
        // Afficher un loader
        this.elements.timeSlotsContainer.innerHTML = `
            <div class="text-center py-4">
                <div class="animate-spin inline-block w-6 h-6 border-2 border-primary border-t-transparent rounded-full"></div>
                <div class="text-sm text-gray-500 mt-2">Chargement des créneaux...</div>
            </div>
        `;
        
        // Charger les créneaux depuis l'API (TEMPORAIRE : endpoint de test)
        fetch(`/appointments-test/available-slots?date=${dateStr}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }
                return response.json();
            })
            .then(slots => {
                this.displayTimeSlots(slots);
            })
            .catch(error => {
                this.log('❌ Erreur lors du chargement des créneaux:', error);
                this.displayDefaultTimeSlots();
            });
    },
    
    /**
     * Afficher les créneaux horaires disponibles
     */
    displayTimeSlots: function(slots) {
        this.availableSlots = slots || [];
        
        if (this.availableSlots.length === 0) {
            this.elements.timeSlotsContainer.innerHTML = `
                <div class="text-center py-4 text-gray-500">
                    <div class="text-sm">Aucun créneau disponible pour cette date</div>
                </div>
            `;
            return;
        }
        
        // Grille adaptative selon la taille d'écran
        const isMobile = window.innerWidth < 640;
        const gridClass = isMobile ? 'grid-cols-2' : 'grid-cols-3 sm:grid-cols-4';
        
        let slotsHTML = `<div class="grid ${gridClass} gap-1.5 sm:gap-2">`;
        
        this.availableSlots.forEach(slot => {
            const time = typeof slot === 'string' ? slot : new Date(slot).toLocaleTimeString('fr-FR', { 
                hour: '2-digit', 
                minute: '2-digit' 
            });
            
            const isSelected = this.selectedTime === time;
            const buttonClass = isSelected 
                ? 'bg-blue-500 text-white border-blue-600' 
                : 'bg-white text-gray-700 border-gray-300 hover:bg-blue-50 hover:border-blue-300';
            
            slotsHTML += `
                <button type="button" 
                        class="time-slot px-2 py-1.5 sm:px-3 sm:py-2 border rounded-lg text-xs sm:text-sm font-medium transition-colors ${buttonClass} min-h-[36px] sm:min-h-[40px]"
                        onclick="AppointmentModal.selectTime('${time}')">
                    ${time}
                </button>
            `;
        });
        
        slotsHTML += '</div>';
        this.elements.timeSlotsContainer.innerHTML = slotsHTML;
        
        this.log('🕐 Créneaux affichés:', this.availableSlots.length);
    },
    
    /**
     * Afficher des créneaux par défaut en cas d'erreur API
     */
    displayDefaultTimeSlots: function() {
        this.log('🕐 Affichage des créneaux par défaut...');
        this.displayTimeSlots(this.config.calendar.timeSlots);
    },
    
    /**
     * Sélectionner une heure
     */
    selectTime: function(time) {
        this.selectedTime = time;
        this.log('🕐 Heure sélectionnée:', time);
        
        // Mettre à jour l'affichage des créneaux
        this.elements.timeSlotsContainer.querySelectorAll('.time-slot').forEach(button => {
            button.className = button.className.replace(/bg-blue-500|text-white|border-blue-600/g, '');
            button.className += ' bg-white text-gray-700 border-gray-300 hover:bg-blue-50 hover:border-blue-300';
        });
        
        const selectedButton = this.elements.timeSlotsContainer.querySelector(`[onclick="AppointmentModal.selectTime('${time}')"]`);
        if (selectedButton) {
            selectedButton.className = selectedButton.className.replace(/bg-white|text-gray-700|border-gray-300|hover:bg-blue-50|hover:border-blue-300/g, '');
            selectedButton.className += ' bg-blue-500 text-white border-blue-600';
        }
        
        // Mettre à jour le champ caché
        const dateTimeStr = `${this.selectedDate.toISOString().split('T')[0]}T${time}:00`;
        this.elements.selectedDateTime.value = dateTimeStr;
        
        // Mettre à jour le résumé
        this.updateSummary();
        
        // Activer le bouton de soumission
        this.elements.submitBtn.disabled = false;
    },
    
    /**
     * Mettre à jour le résumé du rendez-vous
     */
    updateSummary: function() {
        if (!this.selectedDate) {
            this.elements.summary.classList.add('hidden');
            return;
        }
        
        const dateStr = this.selectedDate.toLocaleDateString('fr-FR', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
        
        let summaryContent = `<div>📅 <strong>Date:</strong> ${dateStr}</div>`;
        
        if (this.selectedTime) {
            summaryContent += `<div>🕐 <strong>Heure:</strong> ${this.selectedTime}</div>`;
        }
        
        const service = this.elements.service?.value;
        if (service) {
            summaryContent += `<div>💼 <strong>Service:</strong> ${service}</div>`;
        }
        
        this.elements.summaryContent.innerHTML = summaryContent;
        this.elements.summary.classList.remove('hidden');
    },
    
    /**
     * Mois précédent
     */
    previousMonth: function() {
        this.currentDate.setMonth(this.currentDate.getMonth() - 1);
        this.renderCalendar();
    },
    
    /**
     * Mois suivant
     */
    nextMonth: function() {
        this.currentDate.setMonth(this.currentDate.getMonth() + 1);
        this.renderCalendar();
    },
    
    /**
     * Ouvre le modal de rendez-vous
     */
    open: function(prefillData = {}) {
        this.log('📂 Ouverture du modal de rendez-vous...');
        
        if (!this.elements.modal) {
            this.error('❌ Modal non trouvé !');
            return;
        }
        
        // Préremplir les données si fournies
        this.prefillForm(prefillData);
        
        // Réinitialiser les erreurs
        this.clearErrors();
        
        // Préremplir avec les données de l'utilisateur connecté (si disponible)
        this.prefillUserData();
        
        // Sauvegarder la position de scroll
        this.saveScrollPosition();
        
        // Afficher le modal
        this.elements.modal.classList.remove('hidden');
        this.lockBodyScroll();
        this.isOpen = true;
        
        // Focus sur le premier champ avec amélioration pour mobile
        this.setInitialFocus();
        
        // Vérifier l'orientation
        this.handleViewportChange();
        
        // Rendre le calendrier
        this.renderCalendar();
        
        this.log('✅ Modal ouvert');
    },
    
    /**
     * Ferme le modal de rendez-vous
     */
    close: function() {
        this.log('📁 Fermeture du modal...');
        
        if (!this.elements.modal) return;
        
        // Masquer le modal
        this.elements.modal.classList.add('hidden');
        this.unlockBodyScroll();
        this.isOpen = false;
        
        // Restaurer la position de scroll
        this.restoreScrollPosition();
        
        // Réinitialiser le formulaire
        this.resetForm();
        
        this.log('✅ Modal fermé');
    },
    
    /**
     * Prérempli le formulaire avec des données
     */
    prefillForm: function(data) {
        if (!data || Object.keys(data).length === 0) return;
        
        this.log('📝 Préremplissage du formulaire:', data);
        
        if (data.service && this.elements.service) {
            this.elements.service.value = data.service;
            this.updateSummary();
        }
        if (data.email && this.elements.email) {
            this.elements.email.value = data.email;
        }
        if (data.name && this.elements.name) {
            this.elements.name.value = data.name;
        }
        if (data.phone && this.elements.phone) {
            this.elements.phone.value = data.phone;
        }
    },
    
    /**
     * Valide le formulaire
     */
    validateForm: function() {
        this.log('🔍 Validation du formulaire...');
        
        let isValid = true;
        
        // Effacer les erreurs précédentes
        this.clearErrors();
        
        // Validation du nom
        if (!this.elements.name?.value?.trim()) {
            this.showFieldError('name', 'Le nom est requis');
            isValid = false;
        }
        
        // Validation de l'email
        if (!this.elements.email?.value?.trim()) {
            this.showFieldError('email', 'L\'email est requis');
            isValid = false;
        } else if (!this.isValidEmail(this.elements.email.value)) {
            this.showFieldError('email', 'Email invalide');
            isValid = false;
        }
        
        // Validation du téléphone
        if (!this.elements.phone?.value?.trim()) {
            this.showFieldError('phone', 'Le téléphone est requis');
            isValid = false;
        }
        
        // Validation du service
        if (!this.elements.service?.value) {
            this.showFieldError('service', 'Veuillez sélectionner un service');
            isValid = false;
        }
        
        // Validation de la date et heure
        if (!this.selectedDate || !this.selectedTime) {
            this.showMessage('Veuillez sélectionner une date et une heure', 'error');
            isValid = false;
        }
        
        this.log(isValid ? '✅ Validation réussie' : '❌ Validation échouée');
        return isValid;
    },
    
    /**
     * Affiche une erreur pour un champ spécifique
     */
    showFieldError: function(fieldName, message) {
        const errorElement = this.elements[fieldName + 'Error'];
        if (errorElement) {
            errorElement.textContent = message;
            errorElement.classList.remove('hidden');
        }
    },
    
    /**
     * Efface toutes les erreurs
     */
    clearErrors: function() {
        const errorElements = ['nameError', 'emailError', 'phoneError', 'serviceError'];
        errorElements.forEach(errorKey => {
            const errorElement = this.elements[errorKey];
            if (errorElement) {
                errorElement.classList.add('hidden');
                errorElement.textContent = '';
            }
        });
        
        // Masquer le message global
        if (this.elements.messageContainer) {
            this.elements.messageContainer.classList.add('hidden');
        }
    },
    
    /**
     * Affiche un message global
     */
    showMessage: function(message, type = 'success') {
        if (!this.elements.messageContainer) return;
        
        const colors = {
            success: 'bg-green-100 text-green-800 border border-green-200',
            error: 'bg-red-100 text-red-800 border border-red-200',
            warning: 'bg-yellow-100 text-yellow-800 border border-yellow-200'
        };
        
        this.elements.messageContainer.className = `mb-4 p-4 rounded-lg ${colors[type] || colors.success}`;
        this.elements.messageContainer.textContent = message;
        this.elements.messageContainer.classList.remove('hidden');
        
        // Faire défiler vers le message si nécessaire
        this.elements.messageContainer.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    },
    
    /**
     * Gère l'état de chargement du bouton
     */
    setLoading: function(loading) {
        this.isLoading = loading;
        
        if (this.elements.submitBtn) {
            this.elements.submitBtn.disabled = loading;
        }
        
        if (this.elements.submitText) {
            this.elements.submitText.classList.toggle('hidden', loading);
        }
        
        if (this.elements.submitLoader) {
            this.elements.submitLoader.classList.toggle('hidden', !loading);
        }
    },
    
    /**
     * Soumission du formulaire en AJAX
     */
    submitAppointment: async function() {
        this.log('📤 Soumission du rendez-vous...');
        
        // Validation
        if (!this.validateForm()) {
            return;
        }
        
        // Préparation des données
        const formData = this.collectFormData();
        this.log('📋 Données collectées:', formData);
        
        // État de chargement
        this.setLoading(true);
        
        try {
            // Récupération du token CSRF
            const csrfToken = this.getCsrfToken();
            
            // Requête AJAX (TEMPORAIRE : endpoint de test)
            const response = await fetch('/appointments-test/create', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest',
                    'X-CSRF-TOKEN': csrfToken
                },
                body: JSON.stringify(formData)
            });
            
            // Traitement des réponses JSON ou texte
            let result;
            const contentType = response.headers.get('content-type');
            
            if (contentType && contentType.includes('application/json')) {
                result = await response.json();
            } else {
                result = await response.text();
            }
            
            this.log('📨 Réponse serveur:', { status: response.status, result });
            
            if (response.ok) {
                this.showMessage('🎉 Votre demande de rendez-vous a été envoyée avec succès ! Nous vous contacterons rapidement.', 'success');
                
                // Fermer le modal après 3 secondes
                setTimeout(() => {
                    this.close();
                }, 3000);
            } else if (response.status === 401) {
                // Utilisateur non authentifié
                this.showMessage('🔒 Session expirée. Veuillez vous reconnecter.', 'warning');
                setTimeout(() => {
                    window.location.href = '/login';
                }, 2000);
            } else {
                // Afficher le message d'erreur du serveur si disponible
                const errorMessage = result?.message || 'Une erreur est survenue. Veuillez réessayer plus tard.';
                this.showMessage(`❌ ${errorMessage}`, 'error');
            }
            
        } catch (error) {
            this.error('❌ Erreur lors de l\'envoi:', error);
            this.showMessage('❌ Erreur de connexion. Veuillez vérifier votre connexion internet.', 'error');
        } finally {
            this.setLoading(false);
        }
    },
    
    /**
     * Collecte les données du formulaire
     */
    collectFormData: function() {
        const data = {
            subject: `Demande de rendez-vous - ${this.elements.service?.value || 'Service non spécifié'}`,
            description: this.buildDescription(),
            appointmentDate: this.elements.selectedDateTime?.value || `${this.selectedDate.toISOString().split('T')[0]}T${this.selectedTime}:00`,
            durationMinutes: 60,
            priority: 5
        };
        
        return data;
    },
    
    /**
     * Construit la description du rendez-vous
     */
    buildDescription: function() {
        const lines = [
            `Nom: ${this.elements.name?.value || ''}`,
            `Email: ${this.elements.email?.value || ''}`,
            `Téléphone: ${this.elements.phone?.value || ''}`,
            `Service: ${this.elements.service?.value || ''}`,
            `Date demandée: ${this.selectedDate?.toLocaleDateString('fr-FR')} à ${this.selectedTime || ''}`,
        ];
        
        if (this.elements.message?.value?.trim()) {
            lines.push(`Message: ${this.elements.message.value.trim()}`);
        }
        
        return lines.join('\n');
    },
    
    /**
     * Récupère le token CSRF
     */
    getCsrfToken: function() {
        const tokenElement = document.querySelector('meta[name=_csrf]');
        return tokenElement ? tokenElement.getAttribute('content') : '';
    },
    
    /**
     * Réinitialise le formulaire
     */
    resetForm: function() {
        if (this.elements.form) {
            this.elements.form.reset();
        }
        this.clearErrors();
        this.setLoading(false);
        this.selectedDate = null;
        this.selectedTime = null;
        this.elements.submitBtn.disabled = true;
        this.elements.summary.classList.add('hidden');
        this.elements.timeSlotsContainer.innerHTML = '<div class="text-gray-500 text-sm text-center py-4">Sélectionnez une date pour voir les créneaux disponibles</div>';
    },
    
    /**
     * Validation d'email
     */
    isValidEmail: function(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    },
    
    /**
     * Prérempli le formulaire avec les données de l'utilisateur connecté
     */
    prefillUserData: function() {
        if (!window.AUTH_INFO || !window.AUTH_INFO.isAuthenticated) return;
        
        const user = window.AUTH_INFO.currentUser;
        if (!user) return;
        
        // Préremplir l'email si disponible
        if (user.email && this.elements.email) {
            this.elements.email.value = user.email;
            this.elements.email.readOnly = true;
            this.elements.email.classList.add('bg-gray-100');
        }
        
        // Préremplir le nom si disponible
        if (user.displayName && this.elements.name) {
            this.elements.name.value = user.displayName;
        } else if (user.firstName && user.lastName && this.elements.name) {
            this.elements.name.value = `${user.firstName} ${user.lastName}`;
        }
        
        // Préremplir le téléphone si disponible
        if (user.phone && this.elements.phone) {
            this.elements.phone.value = user.phone;
        }
        
        this.log('👤 Données utilisateur préremplies');
    },
    
    /**
     * Log de debug
     */
    log: function(...args) {
        if (this.config.debug) {
            console.log('🗓️ [AppointmentModal]', ...args);
        }
    },
    
    /**
     * Log d'erreur
     */
    error: function(...args) {
        console.error('❌ [AppointmentModal]', ...args);
    },
    
    /**
     * Fonction debounce pour optimiser les événements resize
     */
    debounce: function(func, wait) {
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(this.resizeTimeout);
                func(...args);
            };
            clearTimeout(this.resizeTimeout);
            this.resizeTimeout = setTimeout(later, wait);
        }.bind(this);
    },
    
    /**
     * Gestion du redimensionnement de la fenêtre
     */
    handleResize: function() {
        if (!this.isOpen) return;
        
        const currentViewport = {
            width: window.innerWidth,
            height: window.innerHeight
        };
        
        // Recalcul du calendrier si nécessaire
        if (this.selectedDate) {
            this.renderCalendar();
        }
        
        this.lastViewportSize = currentViewport;
        this.log('📱 Redimensionnement géré:', currentViewport);
    },
    
    /**
     * Gestion du changement d'orientation
     */
    handleOrientationChange: function() {
        if (!this.isOpen) return;
        
        this.log('🔄 Changement d\'orientation détecté');
        this.handleViewportChange();
    },
    
    /**
     * Gestion du changement de viewport
     */
    handleViewportChange: function() {
        // S'assurer que le modal reste visible
        if (this.elements.modal && this.isOpen) {
            // Force un reflow pour éviter les problèmes d'affichage
            this.elements.modal.offsetHeight;
        }
    },
    
    /**
     * Sauvegarde de la position de scroll
     */
    saveScrollPosition: function() {
        this.scrollY = window.pageYOffset || document.documentElement.scrollTop;
    },
    
    /**
     * Restauration de la position de scroll
     */
    restoreScrollPosition: function() {
        if (typeof this.scrollY === 'number') {
            window.scrollTo(0, this.scrollY);
        }
    },
    
    /**
     * Verrouillage du scroll du body (amélioré pour mobile)
     */
    lockBodyScroll: function() {
        // Méthode améliorée qui fonctionne mieux sur mobile
        document.body.style.overflow = 'hidden';
        document.body.style.position = 'fixed';
        document.body.style.top = `-${this.scrollY}px`;
        document.body.style.width = '100%';
    },
    
    /**
     * Déverrouillage du scroll du body
     */
    unlockBodyScroll: function() {
        document.body.style.overflow = '';
        document.body.style.position = '';
        document.body.style.top = '';
        document.body.style.width = '';
    },
    
    /**
     * Focus initial amélioré pour mobile
     */
    setInitialFocus: function() {
        // Sur mobile, éviter le focus automatique qui peut causer des problèmes
        const isMobile = window.innerWidth < 640;
        
        if (!isMobile && this.elements.name) {
            setTimeout(() => {
                this.elements.name.focus();
            }, 150);
        }
    }
};

// ============================================
// FONCTIONS GLOBALES POUR COMPATIBILITÉ
// ============================================

/**
 * Fonction globale pour ouvrir le modal
 */
window.openAppointmentModal = function(prefillData = {}) {
    console.log('📞 Appel openAppointmentModal:', prefillData);
    window.AppointmentModal.open(prefillData);
};

/**
 * Fonction globale pour fermer le modal
 */
window.closeAppointmentModal = function() {
    console.log('📞 Appel closeAppointmentModal');
    window.AppointmentModal.close();
};

// ============================================
// INITIALISATION AUTOMATIQUE
// ============================================

// Initialisation quand le DOM est prêt
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 DOM prêt, initialisation du modal de rendez-vous...');
    window.AppointmentModal.init();
});

// Initialisation immédiate si le DOM est déjà chargé
if (document.readyState !== 'loading') {
    console.log('🚀 DOM déjà chargé, initialisation immédiate...');
    window.AppointmentModal.init();
}

console.log('✅ Script de modal de rendez-vous avec calendrier complet chargé');
