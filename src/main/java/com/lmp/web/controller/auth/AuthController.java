package com.lmp.web.controller.auth;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.User;
import com.lmp.domain.entity.ServiceOffer;
import com.lmp.domain.enums.OrderStatus;
import com.lmp.repository.OrderRepository;
import com.lmp.service.auth.AuthService;
import com.lmp.service.catalog.ServiceCatalogService;
import com.lmp.service.user.UserService;
import com.lmp.web.dto.LoginDto;
import com.lmp.web.dto.RegisterDto;
import com.lmp.web.dto.RegisterWithOrderDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Contrôleur pour la gestion de l'authentification (connexion, inscription,
 * déconnexion).
 */
@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ServiceCatalogService serviceCatalogService;

    /**
     * Affiche la page de connexion.
     * 
     * @param model   Le modèle pour la vue
     * @param error   Paramètre d'erreur de connexion
     * @param logout  Paramètre de déconnexion
     * @param expired Paramètre de session expirée
     * @return Le nom de la vue
     */
    @GetMapping("/login")
    public String showLoginForm(Model model,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "expired", required = false) String expired) {

        // Rediriger les utilisateurs déjà connectés
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Email ou mot de passe incorrect.");
        }

        if (logout != null) {
            model.addAttribute("successMessage", "Vous avez été déconnecté avec succès.");
        }

        if (expired != null) {
            model.addAttribute("warningMessage", "Votre session a expiré. Veuillez vous reconnecter.");
        }

        return "auth/login";
    }

    /**
     * Affiche la page d'inscription.
     * 
     * @param model Le modèle pour la vue
     * @return Le nom de la vue
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        // Rediriger les utilisateurs déjà connectés
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }

        model.addAttribute("registerDto", new RegisterDto());
        return "auth/register";
    }

    /**
     * Traite l'inscription d'un nouvel utilisateur.
     * 
     * @param registerDto        Les données d'inscription
     * @param bindingResult      Le résultat de la validation
     * @param model              Le modèle pour la vue
     * @param redirectAttributes Les attributs de redirection
     * @return Le nom de la vue ou redirection
     */
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registerDto") RegisterDto registerDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Vérifier les erreurs de validation
            if (bindingResult.hasErrors()) {
                return "auth/register";
            }

            // Validation métier personnalisée
            authService.validateRegistrationData(registerDto);

            // Créer l'utilisateur
            User user = authService.registerUser(registerDto);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Inscription réussie ! Vous pouvez maintenant vous connecter.");
            return "redirect:/login";

        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }

    /**
     * Vérifie l'email d'un utilisateur avec un token.
     * 
     * @param token              Le token de vérification
     * @param redirectAttributes Les attributs de redirection
     * @return Redirection vers la page de connexion
     */
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token,
            RedirectAttributes redirectAttributes) {

        try {
            boolean verified = authService.verifyEmail(token);

            if (verified) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Votre email a été vérifié avec succès ! Vous pouvez maintenant vous connecter.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Token de vérification invalide ou expiré.");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Erreur lors de la vérification de l'email.");
        }

        return "redirect:/login";
    }

    /**
     * Vérification AJAX de la disponibilité d'un email.
     * 
     * @param email L'email à vérifier
     * @return true si l'email est disponible, false sinon
     */
    @GetMapping("/check-email")
    public String checkEmailAvailability(@RequestParam("email") String email, Model model) {
        boolean available = !authService.existsByEmail(email);
        model.addAttribute("available", available);
        return "fragments/email-check :: email-availability";
    }

    /**
     * Affiche la page de profil utilisateur.
     * 
     * @param model Le modèle pour la vue
     * @return Le nom de la vue
     */
    @GetMapping("/profile")
    public String showProfile(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.findByEmail(authentication.getName()).orElse(null);
            if (user != null) {
                model.addAttribute("user", user);
                return "user/profile";
            }
        }
        return "redirect:/login";
    }

    /**
     * Traite la mise à jour du profil utilisateur.
     * 
     * @param user               Les données utilisateur mises à jour
     * @param authentication     L'authentification actuelle
     * @param redirectAttributes Les attributs de redirection
     * @return Redirection vers le profil
     */
    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute("user") User user,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            if (authentication != null && authentication.isAuthenticated()) {
                User currentUser = userService.findByEmail(authentication.getName()).orElse(null);
                if (currentUser != null) {
                    // Mettre à jour uniquement les champs autorisés
                    currentUser.setFirstName(user.getFirstName());
                    currentUser.setLastName(user.getLastName());
                    currentUser.setPhone(user.getPhone());
                    currentUser.setAddress(user.getAddress());
                    currentUser.setCity(user.getCity());
                    currentUser.setPostalCode(user.getPostalCode());
                    currentUser.setCountry(user.getCountry());
                    currentUser.setCompanyName(user.getCompanyName());

                    userService.save(currentUser);

                    redirectAttributes.addFlashAttribute("successMessage",
                            "Profil mis à jour avec succès !");
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Erreur lors de la mise à jour du profil : " + e.getMessage());
        }

        return "redirect:/profile";
    }

    /**
     * Endpoint pour l'inscription avec commande intégrée.
     * Ce endpoint combine l'inscription de l'utilisateur, la connexion automatique,
     * la création de commande et la redirection vers Stripe Checkout.
     *
     * @param registerWithOrderDto Les données d'inscription et de commande
     * @param request              La requête HTTP pour l'authentification
     *                             automatique
     * @return ResponseEntity avec l'URL de redirection Stripe ou les erreurs
     */
    @PostMapping("/register-and-checkout")
    @ResponseBody
    public ResponseEntity<?> registerAndCheckout(@Valid @RequestBody RegisterWithOrderDto registerWithOrderDto,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            // 1. Validation des données d'inscription
            if (!registerWithOrderDto.isPasswordMatching()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "PASSWORD_MISMATCH",
                        "message", "Les mots de passe ne correspondent pas"));
            }

            if (!registerWithOrderDto.getAcceptTerms()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "TERMS_NOT_ACCEPTED",
                        "message", "Vous devez accepter les conditions d'utilisation"));
            }

            // 2. Vérifier que l'email n'existe pas déjà
            if (authService.existsByEmail(registerWithOrderDto.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "EMAIL_EXISTS",
                        "message", "Un utilisateur avec cet email existe déjà"));
            }

            // 3. Sécurisation prix : si offerId fourni, le prix vient de la DB
            java.math.BigDecimal validatedAmount = registerWithOrderDto.getAmount();
            String validatedServiceName = registerWithOrderDto.getServiceName();

            if (registerWithOrderDto.getOfferId() != null) {
                java.util.Optional<ServiceOffer> offerOpt = serviceCatalogService
                        .getValidOffer(registerWithOrderDto.getOfferId());
                if (offerOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "INVALID_OFFER",
                            "message", "L'offre demandée n'existe pas, est inactive ou a expiré"));
                }
                ServiceOffer offer = offerOpt.get();
                validatedAmount = offer.getPrice();
                validatedServiceName = offer.getService().getTitle();
            } else {
                // Legacy: validation basique du montant
                if (validatedAmount == null ||
                        validatedAmount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "INVALID_AMOUNT",
                            "message", "Le montant doit être supérieur à 0"));
                }
            }

            // 4. Créer l'utilisateur
            RegisterDto registerDto = registerWithOrderDto.toRegisterDto();
            User newUser = authService.registerUser(registerDto);

            // 5. Connexion automatique de l'utilisateur
            authenticateUser(registerWithOrderDto.getEmail(),
                    registerWithOrderDto.getPassword(), request, response);

            // 6. Créer la commande avec statut PAYMENT_PENDING (prix validé côté serveur)
            Order order = new Order();
            order.setUser(newUser);
            order.setServiceName(validatedServiceName);
            order.setCurrency(registerWithOrderDto.getCurrency());
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            order.setTotalAmount(validatedAmount);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            // Sauvegarder la commande
            order = orderRepository.save(order);

            // 7. Préparer la réponse avec les informations de redirection
            // L'utilisateur est maintenant authentifié grâce à authenticateUser()
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Inscription réussie ! Redirection vers le paiement...");
            responseData.put("orderId", order.getId());
            responseData.put("userId", newUser.getId());
            responseData.put("redirectUrl", "/stripe/checkout/create-session/" + order.getId());
            responseData.put("authenticated", true); // Indiquer que l'utilisateur est maintenant connecté

            return ResponseEntity.ok(responseData);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "REGISTRATION_FAILED",
                    "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "INTERNAL_ERROR",
                    "message", "Une erreur inattendue s'est produite"));
        }
    }

    /**
     * Connecte automatiquement un utilisateur après inscription.
     *
     * @param email    L'email de l'utilisateur
     * @param password Le mot de passe en clair
     * @param request  La requête HTTP
     * @param response La réponse HTTP
     */
    private void authenticateUser(String email, String password, HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // Créer le token d'authentification
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);

            // Ajouter les détails de la requête
            authToken.setDetails(new WebAuthenticationDetails(request));

            // Authentifier l'utilisateur
            Authentication authentication = authenticationManager.authenticate(authToken);

            // Créer un nouveau contexte de sécurité avec l'authentification
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);

            // Mettre à jour le contexte de sécurité
            SecurityContextHolder.setContext(securityContext);

            // Sauvegarder explicitement le contexte de sécurité dans la session HTTP
            HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
            securityContextRepository.saveContext(securityContext, request, response);

            // Mettre à jour la date de dernière connexion
            userService.updateLastLoginDate(email);

            System.out.println("DEBUG: Authentification réussie pour " + email + ", session sauvegardée");

        } catch (Exception e) {
            // Log l'erreur mais ne pas échouer tout le processus
            System.err.println("Erreur lors de la connexion automatique : " + e.getMessage());
            e.printStackTrace();
        }
    }
}