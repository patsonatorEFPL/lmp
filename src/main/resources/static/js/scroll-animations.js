/**
 * Script d'animations de défilement communes pour toutes les pages
 * Gère l'apparition fluide des éléments lors du scroll
 */

class ScrollAnimationManager {
    constructor() {
        this.animatedElements = [];
        this.isInitialized = false;
        this.observer = null;
        this.scrollTimeout = null;
        
        // Options de l'Intersection Observer
        this.observerOptions = {
            threshold: 0.1,
            rootMargin: '0px 0px -50px 0px'
        };
        
        this.init();
    }
    
    init() {
        if (this.isInitialized) return;
        
        // Vérifier si l'utilisateur préfère une animation réduite
        if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            this.showAllElements();
            return;
        }
        
        // Initialiser l'Intersection Observer
        if ('IntersectionObserver' in window) {
            this.initIntersectionObserver();
        } else {
            // Fallback pour les navigateurs non supportés
            this.initScrollFallback();
        }
        
        // Ajouter les animations de parallaxe léger
        this.initParallax();
        
        // Ajouter l'animation au chargement de la page
        this.animateOnLoad();
        
        this.isInitialized = true;
        console.log('🎬 Gestionnaire d\'animations de défilement initialisé');
    }
    
    /**
     * Initialise l'Intersection Observer pour des performances optimales
     */
    initIntersectionObserver() {
        this.observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    this.animateElement(entry.target);
                    this.observer.unobserve(entry.target);
                }
            });
        }, this.observerOptions);
        
        // Observer tous les éléments avec des classes d'animation
        this.observeElements();
    }
    
    /**
     * Observe les éléments à animer
     */
    observeElements() {
        const selectors = [
            '.fade-in-up',
            '.fade-in-left', 
            '.fade-in-right',
            '.fade-in',
            '.scale-in',
            '.slide-in-bottom',
            '.title-animation',
            '.card-animation',
            '.counter-animation',
            '.image-animation',
            '.form-animation',
            '.button-animation',
            '.nav-animation',
            '.icon-animation',
            '.hero-animation',
            '.section-animation',
            '.rotate-in',
            '.blur-in',
            '.zoom-in-bounce',
            '.slide-in-left',
            '.slide-in-right',
            '.flip-in-y',
            '.stagger-fade-up',
            '.clip-reveal-up'
        ];
        
        selectors.forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(element => {
                if (!element.classList.contains('animate')) {
                    this.observer.observe(element);
                }
            });
        });
    }
    
    /**
     * Anime un élément spécifique
     */
    animateElement(element) {
        // Ajouter un petit délai pour un effet plus naturel
        const delay = element.dataset.delay || 0;
        
        setTimeout(() => {
            element.classList.add('animate');
            
            // Animation spéciale pour les compteurs
            if (element.classList.contains('counter-animation')) {
                this.animateCounter(element);
            }
            
            // Déclencher un événement personnalisé
            element.dispatchEvent(new CustomEvent('elementAnimated', {
                detail: { element: element }
            }));
            
        }, delay);
    }
    
    /**
     * Animation des compteurs numériques
     */
    animateCounter(element) {
        const target = element.querySelector('[data-target]');
        if (!target) return;
        
        const finalValue = parseInt(target.dataset.target) || 0;
        const duration = 2000; // 2 secondes
        const steps = 60; // 60 FPS
        const increment = finalValue / steps;
        let current = 0;
        let step = 0;
        
        const timer = setInterval(() => {
            current += increment;
            step++;
            
            if (step >= steps) {
                current = finalValue;
                clearInterval(timer);
            }
            
            // Formatage selon le type de compteur
            if (target.textContent.includes('%')) {
                target.textContent = Math.round(current) + '%';
            } else if (target.textContent.includes('+')) {
                target.textContent = Math.round(current) + '+';
            } else {
                target.textContent = Math.round(current);
            }
        }, duration / steps);
    }
    
    /**
     * Fallback avec scroll classique pour les navigateurs non supportés
     */
    initScrollFallback() {
        const checkScroll = () => {
            const elements = document.querySelectorAll('[class*="animation"]:not(.animate)');
            
            elements.forEach(element => {
                if (this.isElementInViewport(element)) {
                    this.animateElement(element);
                }
            });
        };
        
        window.addEventListener('scroll', () => {
            clearTimeout(this.scrollTimeout);
            this.scrollTimeout = setTimeout(checkScroll, 10);
        });
        
        // Vérification initiale
        checkScroll();
    }
    
    /**
     * Vérifie si un élément est dans le viewport
     */
    isElementInViewport(element) {
        const rect = element.getBoundingClientRect();
        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
            rect.right <= (window.innerWidth || document.documentElement.clientWidth)
        );
    }
    
    /**
     * Ajoute un effet de parallaxe léger
     */
    initParallax() {
        const parallaxElements = document.querySelectorAll('.parallax-light');
        
        if (parallaxElements.length === 0) return;
        
        let ticking = false;
        
        const updateParallax = () => {
            const scrolled = window.pageYOffset;
            
            parallaxElements.forEach(element => {
                const rate = scrolled * -0.1;
                element.style.transform = `translateY(${rate}px)`;
            });
            
            ticking = false;
        };
        
        const requestTick = () => {
            if (!ticking) {
                requestAnimationFrame(updateParallax);
                ticking = true;
            }
        };
        
        window.addEventListener('scroll', requestTick);
    }
    
    /**
     * Animations au chargement de la page
     */
    animateOnLoad() {
        // Animation séquentielle pour le hero
        const heroElements = document.querySelectorAll('.hero-animation');
        heroElements.forEach((element, index) => {
            setTimeout(() => {
                element.classList.add('animate');
            }, index * 200);
        });
        
        // Animation immédiate pour la navigation
        const navElements = document.querySelectorAll('.nav-animation');
        setTimeout(() => {
            navElements.forEach(element => {
                element.classList.add('animate');
            });
        }, 100);
    }
    
    /**
     * Affiche tous les éléments sans animation (accessibilité)
     */
    showAllElements() {
        const elements = document.querySelectorAll('[class*="animation"]');
        elements.forEach(element => {
            element.style.opacity = '1';
            element.style.transform = 'none';
        });
    }
    
    /**
     * Ajoute une animation en cascade pour un conteneur
     */
    animateCascade(container, delay = 100) {
        const elements = container.querySelectorAll('[class*="animation"]');
        elements.forEach((element, index) => {
            setTimeout(() => {
                this.animateElement(element);
            }, index * delay);
        });
    }
    
    /**
     * Méthode publique pour animer manuellement un élément
     */
    triggerAnimation(selector) {
        const element = document.querySelector(selector);
        if (element) {
            this.animateElement(element);
        }
    }
    
    /**
     * Méthode publique pour réinitialiser une animation
     */
    resetAnimation(selector) {
        const element = document.querySelector(selector);
        if (element) {
            element.classList.remove('animate');
            if (this.observer) {
                this.observer.observe(element);
            }
        }
    }
    
    /**
     * Nettoyage des ressources
     */
    destroy() {
        if (this.observer) {
            this.observer.disconnect();
        }
        
        if (this.scrollTimeout) {
            clearTimeout(this.scrollTimeout);
        }
        
        this.isInitialized = false;
    }
}

// Initialisation automatique quand le DOM est prêt
let scrollAnimationManager;

document.addEventListener('DOMContentLoaded', function() {
    scrollAnimationManager = new ScrollAnimationManager();
});

// Export pour utilisation externe
window.ScrollAnimationManager = ScrollAnimationManager;
window.scrollAnimationManager = scrollAnimationManager;

// Utilitaires pour l'utilisation dans d'autres scripts
window.animateElement = function(selector) {
    if (scrollAnimationManager) {
        scrollAnimationManager.triggerAnimation(selector);
    }
};

window.resetAnimation = function(selector) {
    if (scrollAnimationManager) {
        scrollAnimationManager.resetAnimation(selector);
    }
};

window.animateCascade = function(containerSelector, delay = 100) {
    const container = document.querySelector(containerSelector);
    if (container && scrollAnimationManager) {
        scrollAnimationManager.animateCascade(container, delay);
    }
};
