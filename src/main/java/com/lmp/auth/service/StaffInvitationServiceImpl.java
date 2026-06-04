package com.lmp.auth.service;

import com.lmp.auth.domain.Role;
import com.lmp.auth.domain.StaffInvitation;
import com.lmp.auth.domain.StaffInvitationStatus;
import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.dto.StaffInvitationAcceptRequest;
import com.lmp.auth.dto.StaffInvitationCreateRequest;
import com.lmp.auth.repository.RoleRepository;
import com.lmp.auth.repository.StaffInvitationRepository;
import com.lmp.auth.repository.UserRepository;
import com.lmp.integration.event.BusinessEventPayloadKeys;
import com.lmp.integration.event.LmpBusinessEvent;
import com.lmp.integration.event.LmpBusinessEvent.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class StaffInvitationServiceImpl implements StaffInvitationService {

    private static final Logger logger = LoggerFactory.getLogger(StaffInvitationServiceImpl.class);
    private static final SecureRandom RNG = new SecureRandom();

    private final StaffInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final AuthService authService;
    private final StaffInvitationMailer mailer;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.staff-invitation.expiry-days:7}")
    private int expiryDays;

    public StaffInvitationServiceImpl(StaffInvitationRepository invitationRepository,
                                      UserRepository userRepository,
                                      RoleRepository roleRepository,
                                      PasswordEncoder passwordEncoder,
                                      @Lazy UserService userService,
                                      AuthService authService,
                                      StaffInvitationMailer mailer,
                                      ApplicationEventPublisher eventPublisher) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.authService = authService;
        this.mailer = mailer;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public StaffInvitation createInvitation(StaffInvitationCreateRequest request, UUID actorId) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        String email = request.getEmail().trim().toLowerCase();
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Format d'email invalide");
        }
        if (authService.isDisposableEmail(email)) {
            throw new IllegalArgumentException("Les adresses email jetables ne sont pas autorisées");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }
        invitationRepository.findByEmailAndStatus(email, StaffInvitationStatus.PENDING)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Une invitation est déjà en attente pour cet email — révoquez-la avant d'en envoyer une nouvelle");
                });

        StaffInvitation inv = new StaffInvitation();
        inv.setEmail(email);
        inv.setFirstName(trimToNull(request.getFirstName()));
        inv.setLastName(trimToNull(request.getLastName()));
        inv.setToken(generateToken());
        inv.setStatus(StaffInvitationStatus.PENDING);
        inv.setInvitedBy(actorId);
        inv.setCreatedAt(LocalDateTime.now());
        inv.setExpiresAt(LocalDateTime.now().plusDays(expiryDays));

        StaffInvitation saved = invitationRepository.save(inv);
        logger.info("[STAFF-INVITE] Invitation créée pour {} (expire {})", email, saved.getExpiresAt());

        mailer.sendInvitationEmail(saved);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public StaffInvitation findValidByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token manquant");
        }
        StaffInvitation inv = invitationRepository.findByToken(token.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invitation introuvable ou expirée"));
        if (inv.getStatus() != StaffInvitationStatus.PENDING) {
            throw new IllegalArgumentException("Cette invitation n'est plus valide (" + inv.getStatus() + ")");
        }
        if (inv.isExpired()) {
            throw new IllegalArgumentException("Cette invitation a expiré");
        }
        return inv;
    }

    @Override
    @Transactional
    public User acceptInvitation(StaffInvitationAcceptRequest request) {
        if (request == null) throw new IllegalArgumentException("Requête vide");
        if (!request.isPasswordMatching()) {
            throw new IllegalArgumentException("Les mots de passe ne correspondent pas");
        }
        if (!userService.isPasswordStrong(request.getPassword())) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins 8 caractères, incluant majuscules, minuscules, chiffres et caractères spéciaux");
        }

        StaffInvitation inv = findValidByToken(request.getToken());

        // Garde-fou : un User pourrait avoir été créé entre l'invite et l'accept
        if (userRepository.existsByEmail(inv.getEmail())) {
            throw new IllegalArgumentException("Un compte existe déjà pour cet email");
        }

        User user = new User();
        user.setEmail(inv.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(firstNonBlank(request.getFirstName(), inv.getFirstName()));
        user.setLastName(firstNonBlank(request.getLastName(), inv.getLastName()));
        user.setPhone(trimToNull(request.getPhone()));
        user.setRegistrationDate(LocalDateTime.now());
        user.setStatus(UserStatus.ACTIVE);
        user.setAccountLocked(false);
        // L'email est implicitement vérifié : le destinataire a cliqué le lien dans son
        // inbox. Pas besoin d'un second tour de vérification.
        user.setEmailVerified(true);

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Rôle USER introuvable"));
        Role staffRole = roleRepository.findByName("STAFF")
                .orElseThrow(() -> new IllegalStateException("Rôle STAFF introuvable"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        roles.add(staffRole);
        user.setRoles(roles);

        User saved = userRepository.save(user);

        inv.setStatus(StaffInvitationStatus.ACCEPTED);
        inv.setAcceptedAt(LocalDateTime.now());
        inv.setAcceptedUser(saved.getId());
        invitationRepository.save(inv);

        logger.info("[STAFF-INVITE] Invitation acceptée par {} (userId={})", saved.getEmail(), saved.getId());

        // Provisioning externalErp User → routé via ErpEventListener (User.isStaff() == true)
        Map<String, Object> payload = new HashMap<>();
        payload.put(BusinessEventPayloadKeys.EMAIL, saved.getEmail());
        payload.put("displayName", saved.getDisplayName() != null ? saved.getDisplayName() : saved.getEmail());
        payload.put("createdBy", "staff-invitation");
        payload.put("invitationId", inv.getId().toString());
        eventPublisher.publishEvent(LmpBusinessEvent.of(EventType.USER_REGISTERED, "auth", saved.getId(), payload));

        return saved;
    }

    @Override
    @Transactional
    public void revokeInvitation(UUID invitationId, UUID actorId) {
        StaffInvitation inv = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation introuvable"));
        if (inv.getStatus() != StaffInvitationStatus.PENDING) {
            throw new IllegalArgumentException("Seule une invitation PENDING peut être révoquée");
        }
        inv.setStatus(StaffInvitationStatus.REVOKED);
        inv.setRevokedAt(LocalDateTime.now());
        invitationRepository.save(inv);
        logger.info("[STAFF-INVITE] Invitation {} révoquée par {}", invitationId, actorId);
    }

    @Override
    @Transactional
    public void resendInvitation(UUID invitationId, UUID actorId) {
        StaffInvitation inv = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation introuvable"));
        if (inv.getStatus() != StaffInvitationStatus.PENDING) {
            throw new IllegalArgumentException("Seule une invitation PENDING peut être renvoyée");
        }
        // Régénère le token et repousse l'expiration : un lien éventé ne sera plus utilisable
        inv.setToken(generateToken());
        inv.setExpiresAt(LocalDateTime.now().plusDays(expiryDays));
        invitationRepository.save(inv);
        mailer.sendInvitationEmail(inv);
        logger.info("[STAFF-INVITE] Invitation {} renvoyée par {}", invitationId, actorId);
    }

    @Override
    @Transactional
    public int markExpired() {
        List<StaffInvitation> stale = invitationRepository.findByStatusAndExpiresAtBefore(
                StaffInvitationStatus.PENDING, LocalDateTime.now());
        for (StaffInvitation inv : stale) {
            inv.setStatus(StaffInvitationStatus.EXPIRED);
        }
        if (!stale.isEmpty()) {
            invitationRepository.saveAll(stale);
            logger.info("[STAFF-INVITE] {} invitation(s) marquée(s) EXPIRED", stale.size());
        }
        return stale.size();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return "";
    }
}
