/**
 * Script pour la carte interactive Lmp
 * Utilise Leaflet.js pour afficher une carte interactive avec des marqueurs
 */

// Variables globales
let map;
let markers = [];

/**
 * Initialise la carte interactive
 */
function initMap() {
    // Coordonnées de Montréal (centre par défaut)
    const montrealCoords = [45.5017, -73.5673];
    
    // Création de la carte Leaflet
    map = L.map('map').setView(montrealCoords, 13);
    
    // Ajout de la couche de tuiles OpenStreetMap
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 18
    }).addTo(map);
    
    // Ajout des marqueurs depuis les données Thymeleaf
    addMarkersFromData();
    
    console.log('Carte initialisée avec succès');
}

/**
 * Ajoute les marqueurs à partir des données Thymeleaf
 */
function addMarkersFromData() {
    // Récupération des données depuis Thymeleaf
    const locationsData = window.locationsData || [];
    
    locationsData.forEach(location => {
        const marker = L.marker([location.latitude, location.longitude])
            .addTo(map)
            .bindPopup(createPopupContent(location));
        
        markers.push(marker);
    });
    
    console.log(`${markers.length} marqueurs ajoutés à la carte`);
}

/**
 * Crée le contenu HTML pour la popup d'un marqueur
 * @param {Object} location - Les données de la localisation
 * @returns {string} Le HTML de la popup
 */
function createPopupContent(location) {
    const iconClass = getIconClass(location.category);
    
    return `
        <div class="popup-content">
            <div class="popup-header">
                <i class="fas fa-${iconClass}"></i>
                <h4>${location.name}</h4>
            </div>
            <div class="popup-body">
                <p>${location.description}</p>
                <div class="popup-actions">
                    <button class="btn btn-sm btn-primary" onclick="showDetails('${location.name}')">
                        Plus d'infos
                    </button>
                </div>
            </div>
        </div>
    `;
}

/**
 * Retourne la classe d'icône FontAwesome basée sur la catégorie
 * @param {string} category - La catégorie de la localisation
 * @returns {string} La classe d'icône
 */
function getIconClass(category) {
    const iconMap = {
        'restaurant': 'utensils',
        'coffee': 'coffee',
        'shopping-bag': 'shopping-bag',
        'heartbeat': 'heartbeat',
        'university': 'university',
        'tree': 'tree',
        'subway': 'subway',
        'book': 'book',
        'default': 'map-marker-alt'
    };
    
    return iconMap[category] || iconMap.default;
}

/**
 * Affiche les détails d'une localisation
 * @param {string} locationName - Le nom de la localisation
 */
function showDetails(locationName) {
    // Pour l'instant, on affiche juste une alerte
    // Dans une vraie application, vous pourriez ouvrir une modal ou naviguer vers une page détaillée
    alert(`Détails pour ${locationName}\n\nCette fonctionnalité sera implémentée dans une version future.`);
}

/**
 * Centre la carte sur une localisation spécifique
 * @param {number} lat - Latitude
 * @param {number} lng - Longitude
 * @param {number} zoom - Niveau de zoom (optionnel)
 */
function centerOnLocation(lat, lng, zoom = 15) {
    map.setView([lat, lng], zoom);
}

/**
 * Filtre les marqueurs par catégorie
 * @param {string} category - La catégorie à afficher (ou 'all' pour tout afficher)
 */
function filterMarkers(category) {
    markers.forEach(marker => {
        const location = marker.locationData;
        if (category === 'all' || location.category === category) {
            marker.addTo(map);
        } else {
            marker.remove();
        }
    });
}

/**
 * Recherche une localisation par nom
 * @param {string} searchTerm - Le terme de recherche
 */
function searchLocation(searchTerm) {
    const foundLocation = window.locationsData.find(location => 
        location.name.toLowerCase().includes(searchTerm.toLowerCase())
    );
    
    if (foundLocation) {
        centerOnLocation(foundLocation.latitude, foundLocation.longitude);
        
        // Trouve et ouvre le marqueur correspondant
        markers.forEach(marker => {
            const location = marker.locationData;
            if (location.name === foundLocation.name) {
                marker.openPopup();
            }
        });
    } else {
        alert('Aucune localisation trouvée pour ce terme de recherche.');
    }
}

/**
 * Ajoute un nouveau marqueur à la carte
 * @param {Object} locationData - Les données de la nouvelle localisation
 */
function addNewMarker(locationData) {
    const marker = L.marker([locationData.latitude, locationData.longitude])
        .addTo(map)
        .bindPopup(createPopupContent(locationData));
    
    marker.locationData = locationData;
    markers.push(marker);
}

/**
 * Supprime tous les marqueurs de la carte
 */
function clearAllMarkers() {
    markers.forEach(marker => marker.remove());
    markers = [];
}

/**
 * Exporte les données de la carte (pour développement)
 */
function exportMapData() {
    const data = {
        center: map.getCenter(),
        zoom: map.getZoom(),
        markers: markers.map(marker => marker.locationData)
    };
    
    console.log('Données de la carte:', data);
    return data;
}

// Initialisation de la carte quand le DOM est chargé
document.addEventListener('DOMContentLoaded', function() {
    // Vérification que Leaflet est chargé
    if (typeof L !== 'undefined') {
        initMap();
    } else {
        console.error('Leaflet n\'est pas chargé. Vérifiez que le CDN est inclus.');
    }
});

// Fonctions utilitaires pour le développement
window.mapUtils = {
    initMap,
    addNewMarker,
    clearAllMarkers,
    filterMarkers,
    searchLocation,
    exportMapData
}; 