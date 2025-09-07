package com.lmp.service.user;

import com.lmp.domain.entity.User;
import com.lmp.domain.enums.UserStatus;
import com.lmp.repository.AppointmentRepository;
import com.lmp.repository.OrderRepository;
import com.lmp.repository.ReviewRepository;
import com.lmp.repository.UserRepository;
import com.lmp.service.security.SessionSecurityService;

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

    @BeforeEach
    void setUp() {
        // Utilisateur de test normal
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPassword("encoded_password");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setAccountLocked(false);

        // Utilisateur admin
        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setEmail("admin@example.com");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setPassword("encoded_admin_password");
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setAccountLocked(false);
    }

    @Test
    void testFindById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getId());
        assertEquals(testUser.getEmail(), result.get().getEmail());
        verify(userRepository).findById(1L);
    }

    @Test
    void testFindById_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findById(999L);
    }

    @Test
    void testFindByEmail_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByEmail("test@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getEmail(), result.get().getEmail());
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void testFindByEmail_NotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByEmail("nonexistent@example.com");

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void testFindAll() {
        // Given
        List<User> users = Arrays.asList(testUser, adminUser);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<User> result = userService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void testFindByStatus() {
        // Given
        List<User> allUsers = Arrays.asList(testUser, adminUser);
        when(userRepository.findAll()).thenReturn(allUsers);

        // When
        List<User> activeUsers = userService.findByStatus(UserStatus.ACTIVE);

        // Then
        assertNotNull(activeUsers);
        assertEquals(2, activeUsers.size());
        activeUsers.forEach(user -> assertEquals(UserStatus.ACTIVE, user.getStatus()));
    }

    @Test
    void testSave() {
        // Given
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        User result = userService.save(testUser);

        // Then
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        verify(userRepository).save(testUser);
    }

    @Test
    void testSetUserActive() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.setUserActive(1L, false);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testSetUserActive_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            userService.setUserActive(999L, true)
        );
        
        assertTrue(exception.getMessage().contains("Utilisateur non trouvé avec l'ID: 999"));
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testSetUserLocked() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.setUserLocked(1L, true);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testChangePassword_Success() {
        // Given
        String newPassword = "NewPassword123!";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.changePassword(1L, newPassword);

        // Then
        verify(userRepository).findById(1L);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testChangePassword_WeakPassword() {
        // Given
        String weakPassword = "weak";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            userService.changePassword(1L, weakPassword)
        );
        
        assertTrue(exception.getMessage().contains("Le mot de passe doit contenir au moins 8 caractères"));
        verify(userRepository).findById(1L);
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testChangePassword_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            userService.changePassword(999L, "NewPassword123!")
        );
        
        assertTrue(exception.getMessage().contains("Utilisateur non trouvé avec l'ID: 999"));
        verify(userRepository).findById(999L);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void testChangePasswordWithValidation_Success() {
        // Given
        String currentPassword = "currentPassword";
        String newPassword = "NewPassword123!";
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(newPassword, testUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.changePasswordWithValidation(1L, currentPassword, newPassword);

        // Then
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches(currentPassword, "encoded_password"); // Vérification avec le mot de passe hashé stocké
        verify(passwordEncoder).matches(newPassword, "encoded_password"); // Vérification que nouveau != ancien
        verify(passwordEncoder).encode(newPassword); // Encodage du nouveau
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testChangePasswordWithValidation_InvalidCurrentPassword() {
        // Given
        String currentPassword = "wrongPassword";
        String newPassword = "NewPassword123!";
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            userService.changePasswordWithValidation(1L, currentPassword, newPassword)
        );
        
        assertEquals("Le mot de passe actuel est incorrect", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testChangePasswordWithValidation_SamePassword() {
        // Given
        String currentPassword = "currentPassword";
        String newPassword = "currentPassword"; // Même mot de passe
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(newPassword, testUser.getPassword())).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            userService.changePasswordWithValidation(1L, currentPassword, newPassword)
        );
        
        assertEquals("Le nouveau mot de passe doit être différent du mot de passe actuel", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testChangePasswordByAdmin_Success() {
        // Given
        String newPassword = "NewPassword123!";
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Mock hasRole pour simuler un admin
        UserServiceImpl spyUserService = spy(userService);
        doReturn(true).when(spyUserService).hasRole(2L, "ADMIN");

        // When
        spyUserService.changePasswordByAdmin(1L, newPassword, 2L);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).findById(2L);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testChangePasswordByAdmin_NotAdmin() {
        // Given
        String newPassword = "NewPassword123!";
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));

        // Mock hasRole pour simuler un non-admin
        UserServiceImpl spyUserService = spy(userService);
        doReturn(false).when(spyUserService).hasRole(2L, "ADMIN");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            spyUserService.changePasswordByAdmin(1L, newPassword, 2L)
        );
        
        assertTrue(exception.getMessage().contains("Seuls les administrateurs peuvent changer les mots de passe"));
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testIsPasswordStrong_ValidPassword() {
        // Given
        String strongPassword = "StrongPass123!";

        // When
        boolean result = userService.isPasswordStrong(strongPassword);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsPasswordStrong_TooShort() {
        // Given
        String shortPassword = "Short1!";

        // When
        boolean result = userService.isPasswordStrong(shortPassword);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsPasswordStrong_NoUppercase() {
        // Given
        String noUppercasePassword = "nouppercasepass123!";

        // When
        boolean result = userService.isPasswordStrong(noUppercasePassword);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsPasswordStrong_NoLowercase() {
        // Given
        String noLowercasePassword = "NOLOWERCASEPASS123!";

        // When
        boolean result = userService.isPasswordStrong(noLowercasePassword);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsPasswordStrong_NoDigit() {
        // Given
        String noDigitPassword = "NoDigitPassword!";

        // When
        boolean result = userService.isPasswordStrong(noDigitPassword);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsPasswordStrong_NoSpecialChar() {
        // Given
        String noSpecialCharPassword = "NoSpecialChar123";

        // When
        boolean result = userService.isPasswordStrong(noSpecialCharPassword);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsPasswordStrong_NullPassword() {
        // Given
        String nullPassword = null;

        // When
        boolean result = userService.isPasswordStrong(nullPassword);

        // Then
        assertFalse(result);
    }

    @Test
    void testCheckCurrentPassword_Success() {
        // Given
        String password = "testPassword";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);

        // When
        boolean result = userService.checkCurrentPassword(1L, password);

        // Then
        assertTrue(result);
        verify(passwordEncoder).matches(password, testUser.getPassword());
    }

    @Test
    void testCheckCurrentPassword_Failure() {
        // Given
        String wrongPassword = "wrongPassword";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(wrongPassword, testUser.getPassword())).thenReturn(false);

        // When
        boolean result = userService.checkCurrentPassword(1L, wrongPassword);

        // Then
        assertFalse(result);
        verify(passwordEncoder).matches(wrongPassword, testUser.getPassword());
    }

    @Test
    void testCheckCurrentPassword_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            userService.checkCurrentPassword(999L, "anyPassword")
        );
        
        assertTrue(exception.getMessage().contains("Utilisateur non trouvé avec l'ID: 999"));
        verify(passwordEncoder, never()).matches(any(), any());
    }
}
