package com.lmp.crm.service;

import com.lmp.notification.config.MailAddressConfig;
import com.lmp.crm.dto.AppointmentForm;
import com.lmp.crm.domain.Appointment;
import com.lmp.auth.domain.User;
import com.lmp.crm.domain.AppointmentStatus;
import com.lmp.crm.repository.AppointmentRepository;
import com.lmp.auth.repository.UserRepository;
import com.lmp.shared.util.DateUtils;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private UUID appointmentId;
    private UUID appointmentId2;
    private UUID nonExistentId;

    @BeforeEach
    void setUp() {
        appointmentId = UUID.randomUUID();
        appointmentId2 = UUID.randomUUID();
        nonExistentId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(UUID.randomUUID());
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
        validForm.setPriority(5);

        // Rendez-vous existant pour les tests de conflit
        existingAppointment = new Appointment();
        existingAppointment.setId(appointmentId);
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
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(appointmentRepository.findActiveAppointments()).thenReturn(Arrays.asList());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            appointment.setId(appointmentId);
            return appointment;
        });

        try (MockedStatic<DateUtils> mockedDateUtils = mockStatic(DateUtils.class)) {
            mockedDateUtils.when(() -> DateUtils.calculateBusinessDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                          .thenReturn(1);
            mockedDateUtils.when(() -> DateUtils.isBusinessDay(any(LocalDate.class)))
                          .thenReturn(true);

            Appointment result = appointmentService.createAppointment(validForm, "test@example.com");

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
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            appointmentService.createAppointment(validForm, "nonexistent@example.com")
        );

        assertEquals("Utilisateur non trouvé: nonexistent@example.com", exception.getMessage());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void testCreateAppointment_TimeConflict() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        validForm.setAppointmentDate(tomorrow9AM.plusMinutes(30));
        validForm.setDurationMinutes(60);
        when(appointmentRepository.findActiveAppointments()).thenReturn(Arrays.asList(existingAppointment));

        try (MockedStatic<DateUtils> mockedDateUtils = mockStatic(DateUtils.class)) {
            mockedDateUtils.when(() -> DateUtils.calculateBusinessDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                          .thenReturn(1);
            mockedDateUtils.when(() -> DateUtils.isBusinessDay(any(LocalDate.class)))
                          .thenReturn(true);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                appointmentService.createAppointment(validForm, "test@example.com")
            );

            assertTrue(exception.getMessage().contains("Un rendez-vous existe déjà à ce créneau horaire"));
            verify(appointmentRepository, never()).save(any());
        }
    }

    @Test
    void testCheckTimeConflicts_NoConflict() {
        when(appointmentRepository.findActiveAppointments()).thenReturn(Arrays.asList(existingAppointment));

        try (MockedStatic<DateUtils> mockedDateUtils = mockStatic(DateUtils.class)) {
            mockedDateUtils.when(() -> DateUtils.calculateBusinessDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                          .thenReturn(1);
            mockedDateUtils.when(() -> DateUtils.isBusinessDay(any(LocalDate.class)))
                          .thenReturn(true);

            validForm.setAppointmentDate(tomorrow11AM);
            validForm.setDurationMinutes(60);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
                Appointment appointment = invocation.getArgument(0);
                appointment.setId(appointmentId2);
                return appointment;
            });

            Appointment result = appointmentService.createAppointment(validForm, "test@example.com");
            assertNotNull(result);
        }
    }

    @Test
    void testCheckTimeConflicts_OverlapStart() {
        when(appointmentRepository.findActiveAppointments()).thenReturn(Arrays.asList(existingAppointment));

        try (MockedStatic<DateUtils> mockedDateUtils = mockStatic(DateUtils.class)) {
            mockedDateUtils.when(() -> DateUtils.calculateBusinessDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                          .thenReturn(1);
            mockedDateUtils.when(() -> DateUtils.isBusinessDay(any(LocalDate.class)))
                          .thenReturn(true);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            validForm.setAppointmentDate(tomorrow9AM.plusMinutes(30));
            validForm.setDurationMinutes(60);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                appointmentService.createAppointment(validForm, "test@example.com")
            );

            assertTrue(exception.getMessage().contains("Un rendez-vous existe déjà à ce créneau horaire"));
        }
    }

    @Test
    void testCheckTimeConflicts_CompleteOverlap() {
        when(appointmentRepository.findActiveAppointments()).thenReturn(Arrays.asList(existingAppointment));

        try (MockedStatic<DateUtils> mockedDateUtils = mockStatic(DateUtils.class)) {
            mockedDateUtils.when(() -> DateUtils.calculateBusinessDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                          .thenReturn(1);
            mockedDateUtils.when(() -> DateUtils.isBusinessDay(any(LocalDate.class)))
                          .thenReturn(true);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            validForm.setAppointmentDate(tomorrow9AM);
            validForm.setDurationMinutes(180);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                appointmentService.createAppointment(validForm, "test@example.com")
            );

            assertTrue(exception.getMessage().contains("Un rendez-vous existe déjà à ce créneau horaire"));
        }
    }

    @Test
    void testUpdateAppointment_Success() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(existingAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(existingAppointment);

        try (MockedStatic<DateUtils> mockedDateUtils = mockStatic(DateUtils.class)) {
            mockedDateUtils.when(() -> DateUtils.calculateBusinessDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                          .thenReturn(1);
            mockedDateUtils.when(() -> DateUtils.isBusinessDay(any(LocalDate.class)))
                          .thenReturn(true);

            validForm.setAppointmentDate(tomorrow10AM);
            validForm.setDurationMinutes(60);

            Appointment result = appointmentService.updateAppointment(appointmentId, validForm);
            assertNotNull(result);
            verify(appointmentRepository).save(any(Appointment.class));
        }
    }

    @Test
    void testFindActiveAppointments() {
        when(appointmentRepository.findActiveAppointments()).thenReturn(Arrays.asList(existingAppointment));

        List<Appointment> activeAppointments = appointmentService.findActiveAppointments();

        assertNotNull(activeAppointments);
        assertFalse(activeAppointments.isEmpty());
        assertEquals(1, activeAppointments.size());
        assertEquals(existingAppointment.getId(), activeAppointments.get(0).getId());
    }

    @Test
    void testSearchAppointments() {
        String keyword = "consultation";
        when(appointmentRepository.searchByKeyword(keyword)).thenReturn(Arrays.asList(existingAppointment));

        List<Appointment> searchResults = appointmentService.searchAppointments(keyword);

        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());
        assertEquals(existingAppointment.getId(), searchResults.get(0).getId());
        verify(appointmentRepository).searchByKeyword(keyword);
    }

    @Test
    void testValidateAppointmentForm_InvalidTime() {
        validForm.setAppointmentDate(tomorrow9AM.withHour(18));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            appointmentService.createAppointment(validForm, "test@example.com")
        );

        assertTrue(exception.getMessage().contains("L'heure du rendez-vous doit être entre 9h et 17h"));
    }

    @Test
    void testValidateAppointmentForm_PastDate() {
        LocalDateTime nearFuture = LocalDateTime.now().plusHours(12);
        nearFuture = nearFuture.withHour(10).withMinute(0).withSecond(0).withNano(0);
        validForm.setAppointmentDate(nearFuture);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        try (MockedStatic<DateUtils> mockedDateUtils = mockStatic(DateUtils.class)) {
            mockedDateUtils.when(() -> DateUtils.calculateBusinessDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                          .thenReturn(1);
            mockedDateUtils.when(() -> DateUtils.isBusinessDay(any(LocalDate.class)))
                          .thenReturn(true);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                appointmentService.createAppointment(validForm, "test@example.com")
            );

            assertTrue(exception.getMessage().contains("Les rendez-vous doivent être pris au moins 24h à l'avance"));
        }
    }

    @Test
    void testFindAppointmentById_Success() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(existingAppointment));

        Appointment result = appointmentService.findAppointmentById(appointmentId);

        assertNotNull(result);
        assertEquals(existingAppointment.getId(), result.getId());
        assertEquals(existingAppointment.getSubject(), result.getSubject());
    }

    @Test
    void testFindAppointmentById_NotFound() {
        when(appointmentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            appointmentService.findAppointmentById(nonExistentId)
        );

        assertTrue(exception.getMessage().contains("Rendez-vous non trouvé"));
    }

    @Test
    void testConfirmAppointment_Success() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(existingAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(existingAppointment);

        Appointment result = appointmentService.confirmAppointment(appointmentId);

        assertNotNull(result);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void testCancelAppointment_Success() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(existingAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(existingAppointment);

        Appointment result = appointmentService.cancelAppointment(appointmentId, "Test reason");

        assertNotNull(result);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void testDeleteAppointment_Success() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(existingAppointment));
        doNothing().when(appointmentRepository).delete(any(Appointment.class));

        appointmentService.deleteAppointment(appointmentId);

        verify(appointmentRepository).delete(existingAppointment);
    }
}
