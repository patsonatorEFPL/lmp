package com.lmp.crm.web.admin;

import com.lmp.crm.dto.AppointmentForm;
import com.lmp.crm.domain.Appointment;
import com.lmp.auth.domain.User;
import com.lmp.crm.domain.AppointmentStatus;
import com.lmp.crm.service.AppointmentService;
import com.lmp.auth.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AdminAppointmentController
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@WithMockUser(roles = "ADMIN")
class AdminAppointmentControllerTest {

    private static final String FRONTEND_URL = "http://localhost:4200";

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private UserService userService;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private AdminAppointmentController controller;

    private User testUser;
    private Appointment testAppointment;
    private UUID userId;
    private UUID appointmentId;

    @BeforeEach
    void setUp() {
        // Inject frontendUrl since @Value is not processed by Mockito
        ReflectionTestUtils.setField(controller, "frontendUrl", FRONTEND_URL);

        userId = UUID.randomUUID();
        appointmentId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");

        testAppointment = new Appointment();
        testAppointment.setId(appointmentId);
        testAppointment.setUser(testUser);
        testAppointment.setSubject("Test Appointment");
        testAppointment.setAppointmentDate(LocalDateTime.now().plusDays(1));
        testAppointment.setDurationMinutes(60);
        testAppointment.setPriority(5);
        testAppointment.setStatus(AppointmentStatus.PENDING);
        testAppointment.setCreatedAt(LocalDateTime.now());
    }

    // ======== REDIRECT TESTS (simplified controller methods) ========

    @Test
    void testListAppointments_RedirectsToAngular() {
        String result = controller.listAppointments();
        assertEquals("redirect:" + FRONTEND_URL + "/admin/appointments", result);
    }

    @Test
    void testViewAppointment_RedirectsToAngular() {
        String result = controller.viewAppointment(appointmentId);
        assertEquals("redirect:" + FRONTEND_URL + "/admin/appointments/" + appointmentId, result);
    }

    @Test
    void testNewAppointmentForm_RedirectsToAngular() {
        String result = controller.newAppointmentForm();
        assertEquals("redirect:" + FRONTEND_URL + "/admin/appointments/new", result);
    }

    @Test
    void testSaveAppointment_RedirectsToAngular() {
        String result = controller.saveAppointment();
        assertEquals("redirect:" + FRONTEND_URL + "/admin/appointments", result);
    }

    @Test
    void testEditAppointmentForm_RedirectsToAngular() {
        String result = controller.editAppointmentForm(appointmentId);
        assertEquals("redirect:" + FRONTEND_URL + "/admin/appointments/" + appointmentId + "/edit", result);
    }

    @Test
    void testUpdateAppointment_RedirectsToAngular() {
        String result = controller.updateAppointment(appointmentId);
        assertEquals("redirect:" + FRONTEND_URL + "/admin/appointments/" + appointmentId, result);
    }

    // ======== ACTION TESTS (still have business logic) ========

    @Test
    void testDeleteAppointmentPermanent_Success() {
        doNothing().when(appointmentService).deleteAppointment(eq(appointmentId), anyString());

        String result = controller.deleteAppointmentPermanent(appointmentId, redirectAttributes, createMockAuthentication());

        assertEquals("redirect:" + FRONTEND_URL + "/admin/appointments", result);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
        verify(appointmentService).deleteAppointment(eq(appointmentId), anyString());
    }

    @Test
    void testConfirmAppointment_Success() {
        testAppointment.setStatus(AppointmentStatus.CONFIRMED);
        when(appointmentService.confirmAppointment(eq(appointmentId), eq("admin@test.com"))).thenReturn(testAppointment);

        Map<String, Object> result = controller.confirmAppointment(appointmentId, createMockAuthentication());

        assertTrue((Boolean) result.get("success"));
        assertEquals("Confirmé", result.get("newStatus"));
        verify(appointmentService).confirmAppointment(eq(appointmentId), eq("admin@test.com"));
    }

    @Test
    void testConfirmAppointment_Error() {
        when(appointmentService.confirmAppointment(eq(appointmentId), eq("admin@test.com")))
            .thenThrow(new IllegalStateException("Cannot confirm"));

        Map<String, Object> result = controller.confirmAppointment(appointmentId, createMockAuthentication());

        assertFalse((Boolean) result.get("success"));
        assertTrue(((String) result.get("message")).contains("Cannot confirm"));
    }

    @Test
    void testCancelAppointment_Success() {
        testAppointment.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentService.cancelAppointment(eq(appointmentId), eq("Test reason"), eq("admin@test.com")))
            .thenReturn(testAppointment);

        Map<String, Object> result = controller.cancelAppointment(appointmentId, "Test reason", createMockAuthentication());

        assertTrue((Boolean) result.get("success"));
        assertEquals("Annulé", result.get("newStatus"));
    }

    @Test
    void testStartAppointment_Success() {
        testAppointment.setStatus(AppointmentStatus.IN_PROGRESS);
        when(appointmentService.startAppointment(appointmentId)).thenReturn(testAppointment);

        Map<String, Object> result = controller.startAppointment(appointmentId, createMockAuthentication());

        assertTrue((Boolean) result.get("success"));
        assertEquals("En cours", result.get("newStatus"));
    }

    @Test
    void testCompleteAppointment_Success() {
        testAppointment.setStatus(AppointmentStatus.COMPLETED);
        when(appointmentService.completeAppointment(eq(appointmentId), eq("Notes")))
            .thenReturn(testAppointment);

        Map<String, Object> result = controller.completeAppointment(appointmentId, "Notes", createMockAuthentication());

        assertTrue((Boolean) result.get("success"));
        assertEquals("Terminé", result.get("newStatus"));
    }

    @Test
    void testMarkAsNoShow_Success() {
        testAppointment.setStatus(AppointmentStatus.NO_SHOW);
        when(appointmentService.markAsNoShow(appointmentId)).thenReturn(testAppointment);

        Map<String, Object> result = controller.markAsNoShow(appointmentId, createMockAuthentication());

        assertTrue((Boolean) result.get("success"));
        assertEquals("Absence", result.get("newStatus"));
    }

    @Test
    void testExportAppointments() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(response.getWriter()).thenReturn(printWriter);

        List<Appointment> appointments = Arrays.asList(testAppointment);
        Page<Appointment> appointmentPage = new PageImpl<>(appointments);
        when(appointmentService.searchAppointments(anyString(), any(Pageable.class)))
            .thenReturn(appointmentPage);

        controller.exportAppointments(null, null, null, "test", response, createMockAuthentication());

        verify(response).setContentType("text/csv");
        verify(response).setCharacterEncoding("UTF-8");
        verify(response).setHeader(eq("Content-Disposition"), contains("attachment"));

        printWriter.flush();
        String csvContent = stringWriter.toString();
        assertTrue(csvContent.contains("ID,Client,Email"));
        assertTrue(csvContent.contains("Test Appointment"));
    }

    private org.springframework.security.core.Authentication createMockAuthentication() {
        return new org.springframework.security.core.Authentication() {
            @Override
            public String getName() { return "admin@test.com"; }
            @Override
            public Object getCredentials() { return null; }
            @Override
            public Object getDetails() { return null; }
            @Override
            public Object getPrincipal() { return null; }
            @Override
            public boolean isAuthenticated() { return true; }
            @Override
            public void setAuthenticated(boolean isAuthenticated) {}
            @Override
            public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
                return Arrays.asList(() -> "ROLE_ADMIN");
            }
        };
    }
}
