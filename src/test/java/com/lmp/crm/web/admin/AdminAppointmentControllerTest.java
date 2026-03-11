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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private UserService userService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private AdminAppointmentController controller;

    private MockMvc mockMvc;

    private User testUser;
    private Appointment testAppointment;
    private AppointmentForm testAppointmentForm;
    private UUID userId;
    private UUID appointmentId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

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

        testAppointmentForm = new AppointmentForm();
        testAppointmentForm.setSubject("Test Appointment");
        testAppointmentForm.setAppointmentDate(LocalDateTime.now().plusDays(1));
        testAppointmentForm.setDurationMinutes(60);
        testAppointmentForm.setPriority(5);
    }

    @Test
    void testListAppointments_Success() {
        List<Appointment> appointments = Arrays.asList(testAppointment);
        Page<Appointment> appointmentPage = new PageImpl<>(appointments);

        when(appointmentService.findAppointments(any(), any(), any(), any(Pageable.class)))
            .thenReturn(appointmentPage);
        when(appointmentService.countAppointmentsByStatus(any(AppointmentStatus.class)))
            .thenReturn(1L);

        String result = controller.listAppointments(0, 10, "appointmentDate", "desc",
            null, null, null, null, model,
            createMockAuthentication()
        );

        assertEquals("admin/appointments", result);
        verify(model).addAttribute(eq("appointments"), eq(appointmentPage));
        verify(model).addAttribute(eq("statusCounts"), any(Map.class));
    }

    @Test
    void testListAppointments_WithSearch() {
        List<Appointment> appointments = Arrays.asList(testAppointment);
        Page<Appointment> appointmentPage = new PageImpl<>(appointments);

        when(appointmentService.searchAppointments(eq("test"), any(Pageable.class)))
            .thenReturn(appointmentPage);
        when(appointmentService.countAppointmentsByStatus(any(AppointmentStatus.class)))
            .thenReturn(1L);

        String result = controller.listAppointments(0, 10, "appointmentDate", "desc",
            null, null, null, "test", model,
            createMockAuthentication()
        );

        assertEquals("admin/appointments", result);
        verify(appointmentService).searchAppointments(eq("test"), any(Pageable.class));
    }

    @Test
    void testViewAppointment_Success() {
        when(appointmentService.findById(appointmentId)).thenReturn(testAppointment);

        String result = controller.viewAppointment(appointmentId, model, createMockAuthentication());

        assertEquals("admin/appointment-details", result);
        verify(model).addAttribute(eq("appointment"), eq(testAppointment));
    }

    @Test
    void testViewAppointment_NotFound() {
        when(appointmentService.findById(appointmentId))
            .thenThrow(new RuntimeException("Rendez-vous non trouvé"));

        String result = controller.viewAppointment(appointmentId, model, createMockAuthentication());

        assertEquals("redirect:/admin/appointments", result);
        verify(model).addAttribute(eq("errorMessage"), contains("non trouvé"));
    }

    @Test
    void testNewAppointmentForm_Success() {
        when(userService.findAll()).thenReturn(Arrays.asList(testUser));

        String result = controller.newAppointmentForm(model, createMockAuthentication());

        assertEquals("admin/appointment-form", result);
        verify(model).addAttribute(eq("appointmentForm"), any(AppointmentForm.class));
        verify(model).addAttribute(eq("users"), any(List.class));
    }

    @Test
    void testSaveAppointment_Success() {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.findById(userId)).thenReturn(Optional.of(testUser));
        when(appointmentService.createAppointment(any(AppointmentForm.class), any(User.class)))
            .thenReturn(testAppointment);

        String result = controller.saveAppointment(testAppointmentForm, bindingResult,
            userId, model, redirectAttributes, createMockAuthentication());

        assertEquals("redirect:/admin/appointments", result);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    void testSaveAppointment_ValidationErrors() {
        when(bindingResult.hasErrors()).thenReturn(true);
        when(userService.findAll()).thenReturn(Arrays.asList(testUser));

        String result = controller.saveAppointment(testAppointmentForm, bindingResult,
            userId, model, redirectAttributes, createMockAuthentication());

        assertEquals("admin/appointment-form", result);
        verify(model).addAttribute(eq("users"), any(List.class));
        verify(redirectAttributes, never()).addFlashAttribute(anyString(), anyString());
    }

    @Test
    void testEditAppointmentForm_Success() {
        when(appointmentService.findById(appointmentId)).thenReturn(testAppointment);
        when(userService.findAll()).thenReturn(Arrays.asList(testUser));

        String result = controller.editAppointmentForm(appointmentId, model, createMockAuthentication());

        assertEquals("admin/appointment-form", result);
        verify(model).addAttribute(eq("appointmentForm"), any(AppointmentForm.class));
        verify(model).addAttribute(eq("appointment"), eq(testAppointment));
    }

    @Test
    void testUpdateAppointment_Success() {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(appointmentService.updateAppointment(eq(appointmentId), any(AppointmentForm.class)))
            .thenReturn(testAppointment);

        String result = controller.updateAppointment(appointmentId, testAppointmentForm, bindingResult,
            null, model, redirectAttributes, createMockAuthentication());

        assertEquals("redirect:/admin/appointments/" + appointmentId, result);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    void testUpdateAppointment_WithAdminNotes() {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(appointmentService.updateAppointment(eq(appointmentId), any(AppointmentForm.class)))
            .thenReturn(testAppointment);

        String result = controller.updateAppointment(appointmentId, testAppointmentForm, bindingResult,
            "Admin notes", model, redirectAttributes, createMockAuthentication());

        assertEquals("redirect:/admin/appointments/" + appointmentId, result);
        verify(appointmentService).updateAdminNotes(appointmentId, "Admin notes");
    }

    @Test
    void testDeleteAppointmentPermanent_Success() {
        doNothing().when(appointmentService).deleteAppointment(eq(appointmentId), anyString());

        String result = controller.deleteAppointmentPermanent(appointmentId, redirectAttributes, createMockAuthentication());

        assertEquals("redirect:/admin/appointments", result);
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
