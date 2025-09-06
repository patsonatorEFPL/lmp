/**
 * JavaScript pour la gestion avancée des rendez-vous en administration
 * 
 * Fonctionnalités :
 * - Actions AJAX pour changement de statut
 * - Validation côté client
 * - Export des données
 * - Notifications en temps réel
 * - Modales de confirmation
 */

class AppointmentManager {
    constructor() {
        this.csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
        this.csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
        this.loadingOverlay = document.getElementById('loadingOverlay');
        
        this.initializeEventListeners();
        this.setupAutoRefresh();
    }

    /**
     * Initialise les écouteurs d'événements
     */
    initializeEventListeners() {
        // Actions de statut
        document.addEventListener('click', (e) => {
            if (e.target.matches('[data-action]')) {
                e.preventDefault();
                const action = e.target.dataset.action;
                const appointmentId = e.target.dataset.appointmentId;
                this.handleStatusAction(appointmentId, action);
            }
        });

        // Validation en temps réel des formulaires
        const forms = document.querySelectorAll('form[data-validate="true"]');
        forms.forEach(form => {
            this.setupFormValidation(form);
        });

        // Auto-actualisation des statistiques
        this.updateStatistics();
        setInterval(() => this.updateStatistics(), 60000); // Chaque minute
    }

    /**
     * Configure l'auto-actualisation des données
     */
    setupAutoRefresh() {
        // Actualiser la page toutes les 5 minutes si inactive
        let lastActivity = Date.now();
        
        document.addEventListener('mousemove', () => {
            lastActivity = Date.now();
        });

        setInterval(() => {
            if (Date.now() - lastActivity > 300000) { // 5 minutes d'inactivité
                this.refreshAppointmentsList();
            }
        }, 60000);
    }

    /**
     * Affiche/cache l'overlay de chargement
     */
    showLoading() {
        if (this.loadingOverlay) {
            this.loadingOverlay.style.display = 'flex';
        }
    }

    hideLoading() {
        if (this.loadingOverlay) {
            this.loadingOverlay.style.display = 'none';
        }
    }

    /**
     * Gère les actions de changement de statut
     */
    async handleStatusAction(appointmentId, action) {
        const actionMessages = {
            'confirm': 'Confirmer ce rendez-vous ?',
            'start': 'Démarrer ce rendez-vous ?',
            'complete': 'Marquer ce rendez-vous comme terminé ?',
            'no-show': 'Marquer ce rendez-vous comme absence ?'
        };

        const message = actionMessages[action] || 'Êtes-vous sûr de vouloir effectuer cette action ?';

        const result = await Swal.fire({
            title: 'Confirmation',
            text: message,
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#dc3545',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Oui, continuer',
            cancelButtonText: 'Annuler'
        });

        if (result.isConfirmed) {
            await this.performStatusChange(appointmentId, action);
        }
    }

    /**
     * Effectue le changement de statut via AJAX
     */
    async performStatusChange(appointmentId, action) {
        this.showLoading();

        try {
            const url = `/admin/appointments/${appointmentId}/${action}`;
            
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [this.csrfHeader]: this.csrfToken
                }
            });

            const data = await response.json();
            
            if (data.success) {
                await this.handleSuccessfulStatusChange(appointmentId, action, data);
            } else {
                this.showErrorMessage(data.message);
            }
        } catch (error) {
            console.error('Error:', error);
            this.showErrorMessage('Une erreur est survenue lors de la mise à jour.');
        } finally {
            this.hideLoading();
        }
    }

    /**
     * Gère une mise à jour de statut réussie
     */
    async handleSuccessfulStatusChange(appointmentId, action, data) {
        // Mise à jour du badge de statut
        const statusElement = document.getElementById(`status-${appointmentId}`);
        if (statusElement) {
            statusElement.textContent = data.newStatus;
            statusElement.className = `status-badge status-${action}`;
        }

        // Mise à jour des boutons d'action
        this.updateActionButtons(appointmentId, action);

        // Notification de succès
        await Swal.fire({
            title: 'Succès !',
            text: data.message,
            icon: 'success',
            timer: 2000,
            showConfirmButton: false
        });

        // Actualiser les statistiques
        this.updateStatistics();
    }

    /**
     * Met à jour les boutons d'action selon le nouveau statut
     */
    updateActionButtons(appointmentId, newStatus) {
        const row = document.getElementById(`row-${appointmentId}`);
        if (!row) return;

        const actionsCell = row.querySelector('td:last-child');
        if (!actionsCell) return;

        // Logique pour afficher les bons boutons selon le statut
        const buttonConfigs = {
            'pending': ['confirm', 'cancel'],
            'confirmed': ['start', 'no-show', 'cancel'],
            'in_progress': ['complete'],
            'completed': [],
            'cancelled': [],
            'no_show': []
        };

        const buttonsToShow = buttonConfigs[newStatus] || [];
        
        // Cacher tous les boutons d'action
        const actionButtons = actionsCell.querySelectorAll('.action-btn[data-action]');
        actionButtons.forEach(btn => btn.style.display = 'none');

        // Afficher les boutons appropriés
        buttonsToShow.forEach(action => {
            const button = actionsCell.querySelector(`[data-action="${action}"]`);
            if (button) {
                button.style.display = 'inline-flex';
            }
        });
    }

    /**
     * Gère l'annulation de rendez-vous avec raison
     */
    async handleCancellation(appointmentId) {
        const result = await Swal.fire({
            title: 'Annuler le rendez-vous',
            text: 'Veuillez indiquer la raison de l\'annulation :',
            input: 'textarea',
            inputPlaceholder: 'Raison de l\'annulation...',
            inputValidator: (value) => {
                if (!value || value.trim().length < 3) {
                    return 'Veuillez saisir une raison d\'au moins 3 caractères'
                }
            },
            showCancelButton: true,
            confirmButtonColor: '#dc3545',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Annuler le RDV',
            cancelButtonText: 'Fermer'
        });

        if (result.isConfirmed) {
            await this.performCancellation(appointmentId, result.value);
        }
    }

    /**
     * Effectue l'annulation avec raison
     */
    async performCancellation(appointmentId, reason) {
        this.showLoading();

        try {
            const url = `/admin/appointments/${appointmentId}/cancel`;
            const formData = new FormData();
            formData.append('reason', reason);

            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    [this.csrfHeader]: this.csrfToken
                },
                body: formData
            });

            const data = await response.json();

            if (data.success) {
                await Swal.fire({
                    title: 'Succès !',
                    text: data.message,
                    icon: 'success',
                    timer: 2000,
                    showConfirmButton: false
                });
                
                this.refreshAppointmentsList();
            } else {
                this.showErrorMessage(data.message);
            }
        } catch (error) {
            console.error('Error:', error);
            this.showErrorMessage('Une erreur est survenue lors de l\'annulation.');
        } finally {
            this.hideLoading();
        }
    }

    /**
     * Met à jour les statistiques en temps réel
     */
    async updateStatistics() {
        try {
            const response = await fetch('/admin/appointments/api/statistics', {
                headers: {
                    [this.csrfHeader]: this.csrfToken
                }
            });

            if (response.ok) {
                const stats = await response.json();
                this.updateStatisticsCards(stats);
            }
        } catch (error) {
            console.log('Unable to update statistics:', error.message);
        }
    }

    /**
     * Met à jour les cartes de statistiques
     */
    updateStatisticsCards(stats) {
        Object.entries(stats).forEach(([status, count]) => {
            const card = document.querySelector(`.stat-card.${status.toLowerCase()} .stat-number`);
            if (card && card.textContent !== count.toString()) {
                card.textContent = count;
                
                // Animation de mise à jour
                card.parentElement.classList.add('updated');
                setTimeout(() => {
                    card.parentElement.classList.remove('updated');
                }, 1000);
            }
        });
    }

    /**
     * Actualise la liste des rendez-vous
     */
    refreshAppointmentsList() {
        const currentUrl = new URL(window.location);
        currentUrl.searchParams.set('refresh', Date.now());
        window.location.href = currentUrl.toString();
    }

    /**
     * Configure la validation des formulaires
     */
    setupFormValidation(form) {
        const inputs = form.querySelectorAll('input, select, textarea');
        
        inputs.forEach(input => {
            input.addEventListener('blur', () => this.validateField(input));
            input.addEventListener('input', () => this.clearFieldError(input));
        });

        form.addEventListener('submit', (e) => {
            if (!this.validateForm(form)) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    }

    /**
     * Valide un champ individuel
     */
    validateField(field) {
        let isValid = true;
        const value = field.value.trim();

        // Validation des champs requis
        if (field.hasAttribute('required') && !value) {
            this.showFieldError(field, 'Ce champ est obligatoire');
            return false;
        }

        // Validations spécifiques
        switch (field.id) {
            case 'appointmentDate':
                if (value) {
                    const selectedDate = new Date(value);
                    const now = new Date();
                    
                    if (selectedDate <= now) {
                        this.showFieldError(field, 'La date doit être dans le futur');
                        isValid = false;
                    }
                    
                    const hours = selectedDate.getHours();
                    if (hours < 9 || hours >= 17) {
                        this.showFieldError(field, 'Les rendez-vous doivent être entre 9h et 17h');
                        isValid = false;
                    }
                    
                    const day = selectedDate.getDay();
                    if (day === 0 || day === 6) {
                        this.showFieldError(field, 'Les rendez-vous ne peuvent être pris que du lundi au vendredi');
                        isValid = false;
                    }
                }
                break;

            case 'durationMinutes':
                const duration = parseInt(value);
                if (value && (duration < 30 || duration > 480)) {
                    this.showFieldError(field, 'La durée doit être entre 30 minutes et 8 heures');
                    isValid = false;
                }
                break;
        }

        if (isValid) {
            this.clearFieldError(field);
        }

        return isValid;
    }

    /**
     * Valide un formulaire complet
     */
    validateForm(form) {
        const inputs = form.querySelectorAll('input, select, textarea');
        let isValid = true;

        inputs.forEach(input => {
            if (!this.validateField(input)) {
                isValid = false;
            }
        });

        return isValid;
    }

    /**
     * Affiche une erreur sur un champ
     */
    showFieldError(field, message) {
        field.classList.add('is-invalid');
        const feedback = field.parentElement.querySelector('.invalid-feedback');
        if (feedback) {
            feedback.textContent = message;
            feedback.style.display = 'block';
        }
    }

    /**
     * Efface l'erreur d'un champ
     */
    clearFieldError(field) {
        field.classList.remove('is-invalid');
        const feedback = field.parentElement.querySelector('.invalid-feedback');
        if (feedback) {
            feedback.style.display = 'none';
        }
    }

    /**
     * Affiche un message d'erreur
     */
    showErrorMessage(message) {
        Swal.fire({
            title: 'Erreur !',
            text: message,
            icon: 'error'
        });
    }

    /**
     * Export des données
     */
    exportData(format = 'csv') {
        const params = new URLSearchParams(window.location.search);
        const exportUrl = `/admin/appointments/export?format=${format}&${params.toString()}`;
        
        // Ouvrir dans un nouvel onglet
        const link = document.createElement('a');
        link.href = exportUrl;
        link.target = '_blank';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        // Notification
        this.showSuccessMessage('Export en cours...');
    }

    /**
     * Affiche un message de succès
     */
    showSuccessMessage(message) {
        Swal.fire({
            title: 'Succès !',
            text: message,
            icon: 'success',
            timer: 2000,
            showConfirmButton: false
        });
    }

    /**
     * Filtrage intelligent des rendez-vous
     */
    setupSmartFiltering() {
        const searchInput = document.getElementById('search');
        const statusFilter = document.getElementById('statusFilter');
        
        if (searchInput) {
            let searchTimeout;
            searchInput.addEventListener('input', () => {
                clearTimeout(searchTimeout);
                searchTimeout = setTimeout(() => {
                    this.performSmartSearch();
                }, 300);
            });
        }

        if (statusFilter) {
            statusFilter.addEventListener('change', () => {
                this.performSmartSearch();
            });
        }
    }

    /**
     * Effectue une recherche intelligente
     */
    performSmartSearch() {
        const form = document.querySelector('form[data-smart-filter="true"]');
        if (form) {
            form.submit();
        }
    }
}

// Fonctions globales pour compatibilité avec les templates existants
function changeStatus(appointmentId, action) {
    if (window.appointmentManager) {
        window.appointmentManager.handleStatusAction(appointmentId, action);
    }
}

function cancelAppointment(appointmentId) {
    if (window.appointmentManager) {
        window.appointmentManager.handleCancellation(appointmentId);
    }
}

function exportAppointments(format = 'csv') {
    if (window.appointmentManager) {
        window.appointmentManager.exportData(format);
    }
}

// Initialisation au chargement de la page
document.addEventListener('DOMContentLoaded', function() {
    window.appointmentManager = new AppointmentManager();
    
    // CSS pour les animations
    const style = document.createElement('style');
    style.textContent = `
        .stat-card.updated {
            transform: scale(1.05);
            box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
            transition: all 0.3s ease;
        }
        
        .action-btn {
            transition: all 0.3s ease;
        }
        
        .action-btn:disabled {
            opacity: 0.6;
            cursor: not-allowed;
        }
        
        .loading-spinner {
            display: inline-block;
            width: 12px;
            height: 12px;
            border: 2px solid #f3f3f3;
            border-top: 2px solid #dc3545;
            border-radius: 50%;
            animation: spin 1s linear infinite;
        }
        
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        
        .fade-in {
            animation: fadeIn 0.5s ease-in;
        }
        
        @keyframes fadeIn {
            from { opacity: 0; }
            to { opacity: 1; }
        }
    `;
    document.head.appendChild(style);
});

// Export pour utilisation en module
if (typeof module !== 'undefined' && module.exports) {
    module.exports = AppointmentManager;
}
