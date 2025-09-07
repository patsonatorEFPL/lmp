package com.lmp.web.controller;

import com.lmp.domain.dto.AppointmentForm;
import com.lmp.domain.dto.AppointmentRequest;
import com.lmp.domain.entity.Appointment;
import com.lmp.domain.entity.User;
import com.lmp.domain.enums.AppointmentStatus;
import com.lmp.service.AppointmentService;
import com.lmp.service.user.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AppointmentController (contrôleur REST)
 * Couvre les fonctionnalités de création de rendez-vous via API REST
 */
@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private UserService userService;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private Principal principal;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AppointmentController appointmentController;

    private User testUser;
    private Appointment testAppointment;
    private AppointmentRequest validRequest;
    private LocalDateTime tomorrow9AM;

    @BeforeEach
    void setUp() {
        // Configuration des objets de test
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");

        tomorrow9AM = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);

        testAppointment = new Appointment();
        testAppointment.setId(1L);
        testAppointment.setUser(testUser);
        testAppointment.setSubject("Test Appointment");
        testAppointment.setDescription("Test Description");
        testAppointment.setAppointmentDate(tomorrow9AM);
        testAppointment.setDurationMinutes(60);
        testAppointment.setStatus(AppointmentStatus.PENDING);

        validRequest = new AppointmentRequest();
        validRequest.setName("John Doe");
        validRequest.setEmail("test@example.com");
        validRequest.setPhone("0123456789");
        validRequest.setService("consultation");
        validRequest.setDate("2025-09-08");
        validRequest.setTime("09:00");
        validRequest.setMessage("Test appointment request");
    }

    @Test
    void testCreateAppointment_AuthenticatedUser_Success() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        when(principal.getName()).thenReturn("test@example.com");
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(appointmentService.createAppointment(any(), eq(testUser))).thenReturn(testAppointment);
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Test Browser");
        when(httpRequest.getHeader("Referer")).thenReturn("http://localhost:8080/appointments");

        // When
        ResponseEntity<?> result = appointmentController.createAppointment(validRequest, bindingResult, principal, httpRequest);

        // Then
        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        verify(appointmentService).createAppointment(any(), eq(testUser));
    }

    @Test
    void testCreateAppointment_ValidationErrors() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(true);
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Test Browser");

        // When
        ResponseEntity<?> result = appointmentController.createAppointment(validRequest, bindingResult, principal, httpRequest);

        // Then
        assertNotNull(result);
        assertEquals(400, result.getStatusCode().value());
        verify(appointmentService, never()).createAppointment(any(AppointmentForm.class), any(User.class));
    }

    @Test
    void testCreateAppointment_NoUserAgent() {
        // Given
        when(httpRequest.getHeader("User-Agent")).thenReturn(null);

        // When
        ResponseEntity<?> result = appointmentController.createAppointment(validRequest, bindingResult, principal, httpRequest);

        // Then
        assertNotNull(result);
        assertEquals(400, result.getStatusCode().value());
        verify(appointmentService, never()).createAppointment(any(AppointmentForm.class), any(User.class));
    }
}
