package com.lmp.auth.service;

import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.crm.repository.AppointmentRepository;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.repository.ReviewRepository;
import com.lmp.auth.repository.UserRepository;
import com.lmp.auth.service.SessionSecurityService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour UserService
 * Couvre la logique critique de gestion des utilisateurs et sécurité des mots de passe
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SessionSecurityService sessionSecurityService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private User adminUser;
    private UUID testUserId;
    private UUID adminUserId;
    private UUID nonExistentId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        nonExistentId = UUID.randomUUID();

        // Utilisateur de test normal
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPassword("encoded_password");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setAccountLocked(false);

        // Utilisateur admin
        adminUser = new User();
        adminUser.setId(adminUserId);
        adminUser.setEmail("admin@example.com");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setPassword("encoded_admin_password");
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setAccountLocked(false);
    }

    @Test
    void testFindById_Success() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findById(testUserId);

        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getId());
        assertEquals(testUser.getEmail(), result.get().getEmail());
        verify(userRepository).findById(testUserId);
    }

    @Test
    void testFindById_NotFound() {
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        Optional<User> result = userService.findById(nonExistentId);

        assertFalse(result.isPresent());
        verify(userRepository).findById(nonExistentId);
    }

    @Test
    void testFindByEmail_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals(testUser.getEmail(), result.get().getEmail());
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void testFindByEmail_NotFound() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail("nonexistent@example.com");

        assertFalse(result.isPresent());
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void testFindAll() {
        List<User> users = Arrays.asList(testUser, adminUser);
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void testFindByStatus() {
        List<User> activeOnly = Arrays.asList(testUser, adminUser);
        when(userRepository.findByStatus(UserStatus.ACTIVE)).thenReturn(activeOnly);

        List<User> activeUsers = userService.findByStatus(UserStatus.ACTIVE);

        assertNotNull(activeUsers);
        assertEquals(2, activeUsers.size());
        activeUsers.forEach(user -> assertEquals(UserStatus.ACTIVE, user.getStatus()));
        verify(userRepository).findByStatus(UserStatus.ACTIVE);
    }

    @Test
    void testSave() {
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userService.save(testUser);

        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        verify(userRepository).save(testUser);
    }

    @Test
    void testSetUserActive() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.setUserActive(testUserId, false);

        verify(userRepository).findById(testUserId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testSetUserActive_UserNotFound() {
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            userService.setUserActive(nonExistentId, true)
        );

        assertTrue(exception.getMessage().contains("Utilisateur non trouvé"));
        verify(userRepository).findById(nonExistentId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testSetUserLocked() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.setUserLocked(testUserId, true);

        verify(userRepository).findById(testUserId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testChangePassword_Success() {
        String newPassword = "NewPassword123!";
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.changePassword(testUserId, newPassword);

        verify(userRepository).findById(testUserId);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testChangePassword_WeakPassword() {
        String weakPassword = "weak";
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            userService.changePassword(testUserId, weakPassword)
        );

        assertTrue(exception.getMessage().contains("Le mot de passe doit contenir au moins 8 caractères"));
        verify(userRepository).findById(testUserId);
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testChangePassword_UserNotFound() {
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            userService.changePassword(nonExistentId, "NewPassword123!")
        );

        assertTrue(exception.getMessage().contains("Utilisateur non trouvé"));
        verify(userRepository).findById(nonExistentId);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void testChangePasswordWithValidation_Success() {
        String currentPassword = "currentPassword";
        String newPassword = "NewPassword123!";

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(newPassword, testUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.changePasswordWithValidation(testUserId, currentPassword, newPassword);

        verify(userRepository).findById(testUserId);
        verify(passwordEncoder).matches(currentPassword, "encoded_password");
        verify(passwordEncoder).matches(newPassword, "encoded_password");
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testChangePasswordWithValidation_InvalidCurrentPassword() {
        String currentPassword = "wrongPassword";
        String newPassword = "NewPassword123!";

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            userService.changePasswordWithValidation(testUserId, currentPassword, newPassword)
        );

        assertEquals("Le mot de passe actuel est incorrect", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testChangePasswordWithValidation_SamePassword() {
        String currentPassword = "currentPassword";
        String newPassword = "currentPassword"; // Même mot de passe

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(newPassword, testUser.getPassword())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            userService.changePasswordWithValidation(testUserId, currentPassword, newPassword)
        );

        assertEquals("Le nouveau mot de passe doit être différent du mot de passe actuel", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testChangePasswordByAdmin_Success() {
        String newPassword = "NewPassword123!";

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserServiceImpl spyUserService = spy(userService);
        doReturn(true).when(spyUserService).hasRole(adminUserId, "ADMIN");

        spyUserService.changePasswordByAdmin(testUserId, newPassword, adminUserId);

        verify(userRepository).findById(testUserId);
        verify(userRepository).findById(adminUserId);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testChangePasswordByAdmin_NotAdmin() {
        String newPassword = "NewPassword123!";

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));

        UserServiceImpl spyUserService = spy(userService);
        doReturn(false).when(spyUserService).hasRole(adminUserId, "ADMIN");

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            spyUserService.changePasswordByAdmin(testUserId, newPassword, adminUserId)
        );

        assertTrue(exception.getMessage().contains("Seuls les administrateurs peuvent changer les mots de passe"));
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testIsPasswordStrong_ValidPassword() {
        assertTrue(userService.isPasswordStrong("StrongPass123!"));
    }

    @Test
    void testIsPasswordStrong_TooShort() {
        assertFalse(userService.isPasswordStrong("Short1!"));
    }

    @Test
    void testIsPasswordStrong_NoUppercase() {
        assertFalse(userService.isPasswordStrong("nouppercasepass123!"));
    }

    @Test
    void testIsPasswordStrong_NoLowercase() {
        assertFalse(userService.isPasswordStrong("NOLOWERCASEPASS123!"));
    }

    @Test
    void testIsPasswordStrong_NoDigit() {
        assertFalse(userService.isPasswordStrong("NoDigitPassword!"));
    }

    @Test
    void testIsPasswordStrong_NoSpecialChar() {
        assertFalse(userService.isPasswordStrong("NoSpecialChar123"));
    }

    @Test
    void testIsPasswordStrong_NullPassword() {
        assertFalse(userService.isPasswordStrong(null));
    }

    @Test
    void testCheckCurrentPassword_Success() {
        String password = "testPassword";
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);

        boolean result = userService.checkCurrentPassword(testUserId, password);

        assertTrue(result);
        verify(passwordEncoder).matches(password, testUser.getPassword());
    }

    @Test
    void testCheckCurrentPassword_Failure() {
        String wrongPassword = "wrongPassword";
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(wrongPassword, testUser.getPassword())).thenReturn(false);

        boolean result = userService.checkCurrentPassword(testUserId, wrongPassword);

        assertFalse(result);
        verify(passwordEncoder).matches(wrongPassword, testUser.getPassword());
    }

    @Test
    void testCheckCurrentPassword_UserNotFound() {
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            userService.checkCurrentPassword(nonExistentId, "anyPassword")
        );

        assertTrue(exception.getMessage().contains("Utilisateur non trouvé"));
        verify(passwordEncoder, never()).matches(any(), any());
    }
}
