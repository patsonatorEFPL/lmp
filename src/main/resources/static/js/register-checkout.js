/**
 * Script pour la gestion du flux d'inscription et de checkout intégré
 * Gère la validation des formulaires, l'inscription utilisateur et la redirection Stripe
 */

// Variables globales pour le service sélectionné
let selectedService = {
  name: "",
  amount: 0,
  currency: "EUR",
};

/**
 * Ouvre le modal de commande avec les informations du service
 * @param {string} serviceName - Nom du service (optionnel, défaut: "Consultation")
 * @param {number} amount - Montant du service (optionnel, défaut: 0)
 * @param {string} currency - Devise (par défaut EUR)
 */
function openBookingModal(
  serviceName = "Consultation",
  amount = 0,
  currency = "EUR"
) {
  // DEBUG: Ajouter des logs pour diagnostiquer le problème d'authentification
  console.log("DEBUG: openBookingModal appelé pour service:", serviceName);
  console.log(
    "DEBUG: Paramètres reçus - service:",
    serviceName,
    "amount:",
    amount,
    "currency:",
    currency
  );
  console.log("DEBUG: Vérification état d'authentification...");
  console.log("DEBUG: AUTH_INFO disponible:", window.AUTH_INFO);

  // Vérifier si l'utilisateur est connecté
  if (window.AUTH_INFO && window.AUTH_INFO.isAuthenticated) {
    console.log(
      "DEBUG: Utilisateur connecté - redirection vers flux utilisateur connecté"
    );
    // Utilisateur connecté : créer une commande directement
    handleAuthenticatedUserOrder(serviceName, amount, currency);
    return;
  }

  console.log(
    "DEBUG: Utilisateur non connecté - ouverture modal d'inscription directe"
  );

  // Pour les utilisateurs non connectés : afficher directement le modal d'inscription
  // Plus besoin de sauvegarder l'intention car l'endpoint /register-and-checkout gère tout
  showRegistrationModal(serviceName, amount, currency);
}

/**
 * OBSOLÈTE : Fonction supprimée car nous utilisons maintenant l'endpoint /register-and-checkout
 * qui gère directement l'inscription + commande sans besoin d'intention de paiement en session
 */

/**
 * Affiche le modal d'inscription avec les informations du service
 */
function showRegistrationModal(serviceName, amount, currency) {
  // Stocker les informations du service
  selectedService.name = serviceName;
  selectedService.amount = amount;
  selectedService.currency = currency;

  // Mettre à jour les champs cachés du formulaire
  document.getElementById("modalServiceName").value = serviceName;
  document.getElementById("modalAmount").value = amount;

  // Mettre à jour le résumé de commande
  document.getElementById("orderSummaryService").textContent = serviceName;
  document.getElementById("orderSummaryAmount").textContent = `${amount.toFixed(
    2
  )} ${currency}`;

  // Afficher le modal
  document.getElementById("bookingModal").classList.remove("hidden");

  // Focus sur le premier champ (email maintenant)
  setTimeout(() => {
    const emailField = document.querySelector('input[name="email"]');
    if (emailField) emailField.focus();
  }, 100);
}

/**
 * Ferme le modal de commande
 */
function closeBookingModal() {
  document.getElementById("bookingModal").classList.add("hidden");

  // Réinitialiser le formulaire
  document.getElementById("registerAndCheckoutForm").reset();

  // Masquer les messages
  hideMessage("errorMessage");
  hideMessage("successMessage");

  // Réinitialiser l'état du bouton
  resetSubmitButton();
}

/**
 * Gère la soumission du formulaire d'inscription et de checkout
 * @param {Event} event - Événement de soumission du formulaire
 */
async function handleRegisterAndCheckout(event) {
  event.preventDefault();

  const form = event.target;
  const formData = new FormData(form);

  // Validation côté client
  if (!validateForm(formData)) {
    return;
  }

  // Afficher l'état de chargement
  setLoadingState(true);
  hideMessage("errorMessage");
  hideMessage("successMessage");

  try {
    // Préparer les données pour l'inscription avec commande intégrée (RegisterWithOrderDto)
    const registrationData = {
      firstName: formData.get("firstName") || "",
      lastName: formData.get("lastName") || "",
      email: formData.get("email"),
      password: formData.get("password"),
      confirmPassword: formData.get("confirmPassword"),
      phone: formData.get("phone") || "",
      address: formData.get("address") || "",
      city: formData.get("city") || "",
      postalCode: formData.get("postalCode") || "",
      country: formData.get("country") || "",
      companyName: formData.get("companyName") || "",
      serviceName: selectedService.name,
      amount: selectedService.amount,
      currency: selectedService.currency,
      acceptTerms: formData.get("acceptTerms") === "on",
    };

    console.log(
      "DEBUG: Envoi des données d'inscription avec commande intégrée:",
      registrationData
    );
    console.log("DEBUG: Service sélectionné:", selectedService);

    // Récupérer le token CSRF depuis les métadonnées
    // L'endpoint /register-and-checkout ignore CSRF d'après SecurityConfig
    // Donc on n'envoie pas de token CSRF pour éviter les conflits
    console.log(
      "DEBUG: Préparation requête sans CSRF (endpoint configuré pour ignorer CSRF)"
    );

    // Appeler l'API d'inscription avec commande intégrée
    console.log("DEBUG: Envoi de la requête vers /register-and-checkout");
    console.log("DEBUG: Headers:", {
      "Content-Type": "application/json",
      "X-Requested-With": "XMLHttpRequest",
    });

    const response = await fetch("/register-and-checkout", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Requested-With": "XMLHttpRequest",
      },
      body: JSON.stringify(registrationData),
    });

    console.log(
      "DEBUG: Réponse reçue - Status:",
      response.status,
      "StatusText:",
      response.statusText
    );
    console.log(
      "DEBUG: Response headers:",
      Object.fromEntries(response.headers.entries())
    );

    let result;
    try {
      result = await response.json();
      console.log("DEBUG: Réponse JSON:", result);
    } catch (jsonError) {
      console.error("DEBUG: Erreur parsing JSON:", jsonError);
      console.log("DEBUG: Response text:", await response.text());
      throw new Error("Réponse serveur invalide");
    }

    if (response.ok && result.success) {
      // Inscription et commande créées avec succès
      showMessage(
        "successMessage",
        result.message ||
          "Inscription réussie ! Redirection vers le paiement..."
      );

      console.log("Inscription et commande créées avec succès:", result);
      console.log("Utilisateur authentifié:", result.authenticated);

      // Créer la session Stripe pour la commande créée
      if (result.orderId) {
        setTimeout(async () => {
          try {
            console.log(
              "DEBUG: Création de session Stripe pour commande:",
              result.orderId
            );
            await createStripeSession(result.orderId);
          } catch (error) {
            console.error(
              "Erreur lors de la création de session Stripe:",
              error
            );
            showMessage(
              "errorMessage",
              "Erreur lors de la redirection vers le paiement : " +
                error.message
            );
            setLoadingState(false);
          }
        }, 1000);
      } else {
        console.error("ID de commande manquant dans la réponse");
        showMessage("errorMessage", "Erreur : ID de commande manquant");
        setLoadingState(false);
      }
    } else {
      // Gestion des erreurs
      let errorMessage = "Une erreur s'est produite lors de l'inscription";
      if (result && result.message) {
        errorMessage = result.message;
      }

      showMessage("errorMessage", errorMessage);
      setLoadingState(false);
    }
  } catch (error) {
    console.error("Erreur lors de l'inscription:", error);
    showMessage(
      "errorMessage",
      "Erreur de connexion. Veuillez vérifier votre connexion internet et réessayer."
    );
    setLoadingState(false);
  }
}

/**
 * Crée une session de checkout Stripe directement avec les données du service
 * Architecture webhook-driven : ne dépend plus d'une commande existante
 * @param {Object} serviceData - Données du service
 */
async function createDirectStripeSession(serviceData) {
  try {
    // Récupérer le token CSRF depuis les métadonnées
    const csrfToken = document
      .querySelector('meta[name="_csrf"]')
      ?.getAttribute("content");
    const csrfHeaderName =
      document
        .querySelector('meta[name="_csrf_header"]')
        ?.getAttribute("content") || "X-CSRF-TOKEN";

    if (!csrfToken) {
      console.error("Token CSRF non trouvé dans les métadonnées");
      throw new Error("Token CSRF non disponible");
    }

    console.log(
      "DEBUG: Creating direct Stripe session with data:",
      serviceData
    );

    const response = await fetch("/stripe/checkout/create-session-direct", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Requested-With": "XMLHttpRequest",
        [csrfHeaderName]: csrfToken,
      },
      body: JSON.stringify(serviceData),
    });

    const result = await response.json();

    if (response.ok && result.success && result.redirectUrl) {
      // Rediriger vers Stripe Checkout
      console.log("Redirection vers Stripe:", result.redirectUrl);
      window.location.href = result.redirectUrl;
    } else {
      throw new Error(
        result.message || "Impossible de créer la session de paiement"
      );
    }
  } catch (error) {
    console.error("Erreur lors de la création de la session Stripe:", error);
    throw error;
  }
}

/**
 * Fonction legacy pour les commandes existantes - maintenue pour compatibilité
 * @param {number} orderId - ID de la commande
 */
async function createStripeSession(orderId) {
  try {
    // Récupérer le token CSRF depuis les métadonnées
    const csrfToken = document
      .querySelector('meta[name="_csrf"]')
      ?.getAttribute("content");
    const csrfHeaderName =
      document
        .querySelector('meta[name="_csrf_header"]')
        ?.getAttribute("content") || "X-CSRF-TOKEN";

    if (!csrfToken) {
      console.error("Token CSRF non trouvé dans les métadonnées");
      throw new Error("Token CSRF non disponible");
    }

    console.log("DEBUG: Creating legacy Stripe session for order:", orderId);

    const response = await fetch(`/stripe/checkout/create-session/${orderId}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Requested-With": "XMLHttpRequest",
        [csrfHeaderName]: csrfToken,
      },
    });

    const result = await response.json();

    if (response.ok && result.success && result.redirectUrl) {
      // Rediriger vers Stripe Checkout
      console.log("Redirection vers Stripe:", result.redirectUrl);
      window.location.href = result.redirectUrl;
    } else {
      throw new Error(
        result.message || "Impossible de créer la session de paiement"
      );
    }
  } catch (error) {
    console.error("Erreur lors de la création de la session Stripe:", error);
    throw error;
  }
}

/**
 * Valide le formulaire côté client
 * @param {FormData} formData - Données du formulaire
 * @returns {boolean} - True si le formulaire est valide
 */
function validateForm(formData) {
  // Vérifier que les mots de passe correspondent
  const password = formData.get("password");
  const confirmPassword = formData.get("confirmPassword");

  if (password !== confirmPassword) {
    showMessage("errorMessage", "Les mots de passe ne correspondent pas");
    return false;
  }

  // Vérifier la longueur du mot de passe
  if (password.length < 6) {
    showMessage(
      "errorMessage",
      "Le mot de passe doit contenir au moins 6 caractères"
    );
    return false;
  }

  // Vérifier que les conditions sont acceptées
  if (!formData.get("acceptTerms")) {
    showMessage(
      "errorMessage",
      "Vous devez accepter les conditions d'utilisation"
    );
    return false;
  }

  // Vérifier que les informations de service sont disponibles
  if (
    !selectedService.name ||
    selectedService.amount === null ||
    selectedService.amount === undefined ||
    selectedService.amount < 0
  ) {
    showMessage(
      "errorMessage",
      "Informations du service manquantes. Veuillez recharger la page."
    );
    return false;
  }

  // Vérifier le montant du formulaire si présent
  const formAmount = formData.get("amount");
  if (formAmount) {
    const amount = parseFloat(formAmount);
    if (isNaN(amount) || amount <= 0) {
      showMessage("errorMessage", "Montant invalide");
      return false;
    }
  }

  return true;
}

/**
 * Affiche un message dans le modal
 * @param {string} elementId - ID de l'élément de message
 * @param {string} message - Message à afficher
 */
function showMessage(elementId, message) {
  const element = document.getElementById(elementId);
  if (element) {
    element.textContent = message;
    element.classList.remove("hidden");
  }
}

/**
 * Masque un message dans le modal
 * @param {string} elementId - ID de l'élément de message
 */
function hideMessage(elementId) {
  const element = document.getElementById(elementId);
  if (element) {
    element.classList.add("hidden");
  }
}

/**
 * Active ou désactive l'état de chargement du bouton de soumission
 * @param {boolean} loading - True pour activer le chargement
 */
function setLoadingState(loading) {
  const submitButton = document.getElementById("submitButton");
  const submitButtonText = document.getElementById("submitButtonText");
  const submitButtonSpinner = document.getElementById("submitButtonSpinner");

  if (loading) {
    submitButton.disabled = true;
    submitButtonText.classList.add("hidden");
    submitButtonSpinner.classList.remove("hidden");
  } else {
    submitButton.disabled = false;
    submitButtonText.classList.remove("hidden");
    submitButtonSpinner.classList.add("hidden");
  }
}

/**
 * Remet le bouton de soumission à son état initial
 */
function resetSubmitButton() {
  setLoadingState(false);
}

/**
 * Validation en temps réel des mots de passe
 */
document.addEventListener("DOMContentLoaded", function () {
  // Ajouter la validation en temps réel pour les mots de passe
  const passwordField = document.querySelector('input[name="password"]');
  const confirmPasswordField = document.querySelector(
    'input[name="confirmPassword"]'
  );

  if (passwordField && confirmPasswordField) {
    function validatePasswords() {
      const password = passwordField.value;
      const confirmPassword = confirmPasswordField.value;

      if (confirmPassword && password !== confirmPassword) {
        confirmPasswordField.setCustomValidity(
          "Les mots de passe ne correspondent pas"
        );
      } else {
        confirmPasswordField.setCustomValidity("");
      }
    }

    passwordField.addEventListener("input", validatePasswords);
    confirmPasswordField.addEventListener("input", validatePasswords);
  }

  // Fermer le modal en cliquant à l'extérieur
  document
    .getElementById("bookingModal")
    .addEventListener("click", function (event) {
      if (event.target === this) {
        closeBookingModal();
      }
    });

  // Fermer le modal avec la touche Escape
  document.addEventListener("keydown", function (event) {
    if (
      event.key === "Escape" &&
      !document.getElementById("bookingModal").classList.contains("hidden")
    ) {
      closeBookingModal();
    }
  });
});

/**
 * Fonctions globales pour compatibilité avec les anciens appels
 */
window.openBookingModal = openBookingModal;
window.closeBookingModal = closeBookingModal;
window.handleRegisterAndCheckout = handleRegisterAndCheckout;

// Alias pour openServiceModal (utilisé sur la page services)
window.openServiceModal = openBookingModal;

// Ancienne fonction pour compatibilité
function handleBookingSubmit(event) {
  console.warn(
    "handleBookingSubmit is deprecated, use handleRegisterAndCheckout instead"
  );
  return handleRegisterAndCheckout(event);
}

window.handleBookingSubmit = handleBookingSubmit;

/**
 * Toggle l'affichage des champs optionnels (prénom/nom)
 */
function toggleOptionalFields() {
  const optionalFields = document.getElementById("optionalFields");
  const toggleText = document.getElementById("toggleText");
  const toggleIcon = document.getElementById("toggleIcon");

  if (optionalFields && toggleText && toggleIcon) {
    if (optionalFields.classList.contains("hidden")) {
      // Montrer les champs
      optionalFields.classList.remove("hidden");
      toggleText.textContent = "- Masquer prénom et nom";
      toggleIcon.classList.remove("fa-chevron-down");
      toggleIcon.classList.add("fa-chevron-up");
    } else {
      // Cacher les champs
      optionalFields.classList.add("hidden");
      toggleText.textContent = "+ Ajouter prénom et nom (optionnel)";
      toggleIcon.classList.remove("fa-chevron-up");
      toggleIcon.classList.add("fa-chevron-down");

      // Vider les champs quand on les masque
      const firstNameField = document.querySelector('input[name="firstName"]');
      const lastNameField = document.querySelector('input[name="lastName"]');
      if (firstNameField) firstNameField.value = "";
      if (lastNameField) lastNameField.value = "";
    }
  }
}

/**
 * Toggle l'affichage des champs d'adresse de facturation
 */
function toggleBillingAddress() {
  const billingFields = document.getElementById("billingFields");
  const toggleText = document.getElementById("billingToggleText");
  const toggleIcon = document.getElementById("billingToggleIcon");

  if (billingFields && toggleText && toggleIcon) {
    if (billingFields.classList.contains("hidden")) {
      // Montrer les champs
      billingFields.classList.remove("hidden");
      toggleText.textContent = "- Masquer l'adresse de facturation";
      toggleIcon.classList.remove("fa-chevron-down");
      toggleIcon.classList.add("fa-chevron-up");
    } else {
      // Cacher les champs
      billingFields.classList.add("hidden");
      toggleText.textContent =
        "+ Ajouter une adresse de facturation (optionnel)";
      toggleIcon.classList.remove("fa-chevron-up");
      toggleIcon.classList.add("fa-chevron-down");

      // Vider les champs quand on les masque
      const addressField = document.querySelector('input[name="address"]');
      const cityField = document.querySelector('input[name="city"]');
      const postalCodeField = document.querySelector(
        'input[name="postalCode"]'
      );
      const countryField = document.querySelector('select[name="country"]');
      const companyNameField = document.querySelector(
        'input[name="companyName"]'
      );

      if (addressField) addressField.value = "";
      if (cityField) cityField.value = "";
      if (postalCodeField) postalCodeField.value = "";
      if (countryField) countryField.value = "";
      if (companyNameField) companyNameField.value = "";
    }
  }
}

/**
 * Gère la commande pour un utilisateur déjà connecté
 * Architecture webhook-driven : utilise la méthode directe sans commande temporaire
 * @param {string} serviceName - Nom du service
 * @param {number} amount - Montant du service
 * @param {string} currency - Devise
 */
async function handleAuthenticatedUserOrder(
  serviceName,
  amount,
  currency = "EUR"
) {
  console.log(
    "DEBUG: Création de session Stripe directe pour utilisateur connecté:",
    {
      service: serviceName,
      amount: amount,
      currency: currency,
      user: window.AUTH_INFO.currentUser,
    }
  );

  try {
    // Récupérer le token CSRF depuis les métadonnées
    const csrfToken = document
      .querySelector('meta[name="_csrf"]')
      ?.getAttribute("content");
    const csrfHeaderName =
      document
        .querySelector('meta[name="_csrf_header"]')
        ?.getAttribute("content") || "X-CSRF-TOKEN";

    if (!csrfToken) {
      console.error("Token CSRF non trouvé dans les métadonnées");
      alert("Erreur de sécurité. Veuillez recharger la page.");
      return;
    }

    // ÉTAPE 1 : Récupérer les données utilisateur depuis l'API (sans créer de commande)
    console.log("DEBUG: Récupération des données utilisateur...");

    const userDataResponse = await fetch("/api/orders/create-temp", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Requested-With": "XMLHttpRequest",
        [csrfHeaderName]: csrfToken,
      },
      body: JSON.stringify({
        serviceName: serviceName,
        amount: amount,
        currency: currency,
      }),
    });

    const userData = await userDataResponse.json();

    if (!userDataResponse.ok || !userData.serviceName) {
      console.error(
        "Erreur lors de la récupération des données utilisateur:",
        userData
      );
      alert(userData.message || "Erreur lors de la préparation des données");
      return;
    }

    console.log("DEBUG: Données utilisateur récupérées:", userData);

    // ÉTAPE 2 : Créer directement la session Stripe avec les données
    const serviceData = {
      serviceName: userData.serviceName,
      amount: userData.amount,
      currency: userData.currency,
      userId: userData.userId,
      userEmail: userData.userEmail,
      userFirstName: userData.userFirstName,
      userLastName: userData.userLastName,
    };

    console.log(
      "DEBUG: Création de session Stripe directe avec les données:",
      serviceData
    );

    // Utiliser la nouvelle méthode directe
    await createDirectStripeSession(serviceData);
  } catch (error) {
    console.error("Erreur lors de la création de la session directe:", error);
    alert("Erreur de connexion. Veuillez réessayer.");
  }
}

// Rendre les fonctions globales
window.toggleOptionalFields = toggleOptionalFields;
window.toggleBillingAddress = toggleBillingAddress;
window.handleAuthenticatedUserOrder = handleAuthenticatedUserOrder;
