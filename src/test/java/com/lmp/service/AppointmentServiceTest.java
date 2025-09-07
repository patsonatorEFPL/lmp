package com.lmp.service;

import com.lmp.config.MailAddressConfig;
import com.lmp.domain.dto.AppointmentForm;
import com.lmp.domain.entity.Appointment;
import com.lmp.domain.entity.User;
import com.lmp.domain.enums.AppointmentStatus;
import com.lmp.repository.AppointmentRepository;
import com.lmp.repository.UserRepository;
import com.lmp.util.DateUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AppointmentService
 * Couvre la logique métier critique incluant la détection de conflits d'horaires
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JavaMailSender mailSender;
    
    @Mock
    private MailAddressConfig mailAddressConfig;

    @InjectMocks
    private AppointmentService appointmentService;

    private User testUser;
    private AppointmentForm validForm;
    private Appointment existingAppointment;
    private LocalDateTime tomorrow9AM;
    private LocalDateTime tomorrow10AM;
    private LocalDateTime tomorrow11AM;

    @BeforeEach
    void setUp() {
        // Configuration des objets de test
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");

        // Date de base : dans 2 jours à 9h (pour respecter les 24h à l'avance)
        tomorrow9AM = LocalDate.now().plusDays(2).atTime(9, 0);
        tomorrow10AM = LocalDate.now().plusDays(2).atTime(10, 0);
        tomorrow11AM = LocalDate.now().plusDays(2).atTime(11, 0);

        // Formulaire valide
        validForm = new AppointmentForm();
        validForm.setSubject("Consultation générale");
        validForm.setDescription("Description du rendez-vous");
        validForm.setAppointmentDate(tomorrow9AM);
        validForm.setDurationMinutes(60);
        validForm.setPriority(5); // 5 = priorité normale

        // Rendez-vous existant pour les tests de conflit
        existingAppointment = new Appointment();
        existingAppointment.setId(1L);
        existingAppointment.setUser(testUser);
        existingAppointment.setSubject("RDV existant");
        existingAppointment.setAppointmentDate(tomorrow10AM);
        existingAppointment.setDurationMinutes(60);
        existingAppointment.setStatus(AppointmentStatus.PENDING);
        
        // Configuration du mock MailAddressConfig
        when(mailAddressConfig.getNoreply()).thenReturn("noreply@lmp-services.ca");
        when(mailAddressConfig.getSupport()).thenReturn("support@lmp-services.ca");
        when(mailAddressConfig.getReplyToSupport()).thenReturn("support@lmp-services.ca");
        when(mailAddressConfig.getName()).thenReturn("LMP Digital Services");
    }

    @Test
    void testCreateAppointment_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(appointmentRepository.findActiveAppointments()).thenReturn(Arrays.asList());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            appointment.setId(1L);
            return appointment;
        });

        // Mock DateUtils pour la validation
        try (MockedStatic<DateUtils> mockedDateUtils = mockStatic(DateUtils.class)) {
            mockedDateUtils.when(() -> DateUtils.calculateBusinessDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                          .thenReturn(1);
            mockedDateUtils.when(() -> DateUtils.isBusinessDay(any(LocalDate.class)))
                          .thenReturn(true);

            // When
            Appointment result = appointmentService.createAppointment(validForm, "test@example.com");

            // Then
            assertNotNull(result);
            assertEquals("Consultation générale", result.getSubject());
            assertEquals(tomorrow9AM, result.getAppointmentDate());
            assertEquals(60, result.getDurationMinutes());
            assertEquals(AppointmentStatus.PENDING, result.getStatus());
            assertEquals(testUser, result.getUser());

            verify(appointmentRepository).save(any(Appointment.class));
        }
    }

    @Test
    void testCreateAppointment_UserNotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            appointmentService.createAppointment(validForm, "nonexistent@example.com")
        );
        
        assertEquals("Utilisateur non trouvé: nonexistent@example.com", exception.getMessage());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void testCreateAppointment_TimeConflict() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        // Simuler un conflit : rendez-vous existant de 10h à 11h, nouveau rendez-vous de 9h30 à 10h30
        validForm.setAppointmentDate(tomorrow9AM.plusMinutes(30)); // 9h30
        validForm.setDurationMinutes(60); // jusqu'à 10h30
        
        when(appointmentRepository.findActiveAppointments()).thenReturn(Arrays.asList(existingAppointment));

        // Mock DateUtils
        try (MockedStatic<DateUtils> mockedDateUtils = mockStatic(DateUtils.class)) {
            mockedDateUtils.when(() -> DateUtils.calculateBusinessDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                          .thenReturn(1);
            mockedDateUtils.when(() -> DateUtils.isBusinessDay(any(LocalDate.class)))
                          .thenReturn(true);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                appointmentService.createAppointment(validForm, "test@example.com")
            );
            
            assertTrue(exception.getMessage().contains("Un rendez-vous existe déjà à ce créneau horaire"));
            verify(appointmentRepository, never()).save(any());
        }
    }

    @Test
    void testCheckTimeConflicts_NoConflict() {
        // Given
        when(appointmentRepository.findActiveAppointments()).thenReturn(Arrays.asList(existingAppointment));

        // Mock DateUtils
        try (MockedStatic<DateUtils> mockedDateUtils = mockStatic(DateUtils.class)) {
            mockedDateUtils.when(() -> DateUtils.calculateBusinessDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                          .thenReturn(1);
            mockedDateUtils.when(() -> DateUtils.isBusinessDay(any(LocalDate.class)))
                          .thenReturn(true);

            // When - Nouveau RDV de 11h à 12h (après l'existant qui est de 10h à 11h, pas de conflit)
            validForm.setAppointmentDate(tomorrow11AM); // 11h
            validForm.setDurationMinutes(60); // jusqu'à 12h

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
                Appointment appointment = invocation.getArgument(0);
                appointment.setId(2L);
                return appointment;
            });

            // Then - Ne doit pas lever d'exception
            Appointment result = appointmentService.createAppointment(validForm, "test@example.com");
            assertNotNull(result);
        }
    }

    @Test
    void testCheckTimeConflicts_OverlapStart() {
        // Given
        when(appointmentRepository.findActiveAppointments()).thenReturn(Arrays.asList(existingAppointment));

        // Mock DateUtils
        try (MockedStatic<DateUtils> mockedDateUtils = mockStatic(DateUtils.class)) {
            mockedDateUtils.when(() -> DateUtils.calculateBusinessDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                          .thenReturn(1);
            mockedDateUtils.when(() -> DateUtils.isBusinessDay(any(LocalDate.class)))
                          .thenReturn(true);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            // When - Nouveau RDV de 9h30 à 10h30 (chevauche avec existant 10h-11h)
            validForm.setAppointmentDate(tomorrow9AM.plusMinutes(30)); // 9h30
            validForm.setDurationMinutes(60); // jusqu'à 10h30

            // Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                appointmentService.createAppointment(validForm, "test@example.com")
            );
            
            assertTrue(exception.getMessage().contains("Un rendez-vous existe déjà à ce créneau horaire"));
        }
    }

    @Test
    void testCheckTimeConflicts_CompleteOverlap() {
        // Given
        when(appointmentRepository.findActiveAppointments()).thenReturn(Arrays.asList(existingAppointment));

        // Mock DateUtils
        try (MockedStatic<DateUtils> mockedDateUtils = mockStatic(DateUtils.class)) {
            mockedDateUtils.when(() -> DateUtils.calculateBusinessDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                          .thenReturn(1);
            mockedDateUtils.when(() -> DateUtils.isBusinessDay(any(LocalDate.class)))
                          .thenReturn(true);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            // When - Nouveau RDV de 9h à 12h (englobe complètement l'existant 10h-11h)
            validForm.setAppointmentDate(tomorrow9AM); // 9h
            validForm.setDurationMinutes(180); // jusqu'à 12h

            // Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                appointmentService.createAppointment(validForm, "test@example.com")
            );
            
            assertTrue(exception.getMessage().contains("Un rendez-vous existe déjà à ce créneau horaire"));
        }
    }

    @Test
    void testUpdateAppointment_Success() {
        // Given - Test de mise à jour d'un rendez-vous existant
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(existingAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(existingAppointment);

        // Mock DateUtils
        try (MockedStatic<DateUtils> mockedDateUtils = mockStatic(DateUtils.class)) {
            mockedDateUtils.when(() -> DateUtils.calculateBusinessDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                          .thenReturn(1);
            mockedDateUtils.when(() -> DateUtils.isBusinessDay(any(LocalDate.class)))
                          .thenReturn(true);

            // When - Modifier le même rendez-vous (même horaire) ne doit pas créer de conflit
            validForm.setAppointmentDate(tomorrow10AM); // Même heure que l'existant
            validForm.setDurationMinutes(60);

            // Then - Ne doit pas lever d'exception car c'est le même rendez-vous
            Appointment result = appointmentService.updateAppointment(1L, validForm);
            assertNotNull(result);
            verify(appointmentRepository).save(any(Appointment.class));
        }
    }

    @Test
    void testFindActiveAppointments() {
        // Given
        when(appointmentRepository.findActiveAppointments()).thenReturn(Arrays.asList(existingAppointment));

        // When
        List<Appointment> activeAppointments = appointmentService.findActiveAppointments();

        // Then
        assertNotNull(activeAppointments);
        assertFalse(activeAppointments.isEmpty());
        assertEquals(1, activeAppointments.size());
        assertEquals(existingAppointment.getId(), activeAppointments.get(0).getId());
    }

    @Test
    void testSearchAppointments() {
        // Given
        String keyword = "consultation";
        when(appointmentRepository.searchByKeyword(keyword)).thenReturn(Arrays.asList(existingAppointment));

        // When
        List<Appointment> searchResults = appointmentService.searchAppointments(keyword);

        // Then
        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());
        assertEquals(existingAppointment.getId(), searchResults.get(0).getId());
        verify(appointmentRepository).searchByKeyword(keyword);
    }

    @Test
    void testValidateAppointmentForm_InvalidTime() {
        // Given - Heure invalide (18h)
        validForm.setAppointmentDate(tomorrow9AM.withHour(18));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            appointmentService.createAppointment(validForm, "test@example.com")
        );
        
        assertTrue(exception.getMessage().contains("L'heure du rendez-vous doit être entre 9h et 17h"));
    }

    @Test
    void testValidateAppointmentForm_PastDate() {
        // Given - Date dans le passé (seulement 12h à l'avance) mais avec une heure valide
        LocalDateTime nearFuture = LocalDateTime.now().plusHours(12);
        // S'assurer que c'est à une heure valide (9h00 ou 9h30, etc.)
        nearFuture = nearFuture.withHour(10).withMinute(0).withSecond(0).withNano(0);
        validForm.setAppointmentDate(nearFuture);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Mock DateUtils pour que les autres validations passent
        try (MockedStatic<DateUtils> mockedDateUtils = mockStatic(DateUtils.class)) {
            mockedDateUtils.when(() -> DateUtils.calculateBusinessDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                          .thenReturn(1);
            mockedDateUtils.when(() -> DateUtils.isBusinessDay(any(LocalDate.class)))
                          .thenReturn(true);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                appointmentService.createAppointment(validForm, "test@example.com")
            );
            
            // Debug: afficher le message réel
            System.out.println("Message d'exception réel: " + exception.getMessage());
            assertTrue(exception.getMessage().contains("Les rendez-vous doivent être pris au moins 24h à l'avance"));
        }
    }

    @Test
    void testFindAppointmentById_Success() {
        // Given
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(existingAppointment));

        // When
        Appointment result = appointmentService.findAppointmentById(1L);

        // Then
        assertNotNull(result);
        assertEquals(existingAppointment.getId(), result.getId());
        assertEquals(existingAppointment.getSubject(), result.getSubject());
    }

    @Test
    void testFindAppointmentById_NotFound() {
        // Given
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            appointmentService.findAppointmentById(999L)
        );
        
        assertTrue(exception.getMessage().contains("Rendez-vous non trouvé avec l'ID: 999"));
    }

    @Test
    void testConfirmAppointment_Success() {
        // Given
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(existingAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(existingAppointment);

        // When
        Appointment result = appointmentService.confirmAppointment(1L);

        // Then
        assertNotNull(result);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void testCancelAppointment_Success() {
        // Given
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(existingAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(existingAppointment);

        // When
        Appointment result = appointmentService.cancelAppointment(1L, "Test reason");

        // Then
        assertNotNull(result);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void testDeleteAppointment_Success() {
        // Given
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(existingAppointment));
        doNothing().when(appointmentRepository).delete(any(Appointment.class));

        // When
        appointmentService.deleteAppointment(1L);

        // Then
        verify(appointmentRepository).delete(existingAppointment);
    }
}
