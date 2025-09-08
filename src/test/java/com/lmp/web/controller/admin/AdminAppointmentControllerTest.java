package com.lmp.web.controller.admin;

import com.lmp.domain.dto.AppointmentForm;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        
        // Configuration des objets de test
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");

        testAppointment = new Appointment();
        testAppointment.setId(1L);
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
        // Arrange
        List<Appointment> appointments = Arrays.asList(testAppointment);
        Page<Appointment> appointmentPage = new PageImpl<>(appointments);
        
        when(appointmentService.findAppointments(any(), any(), any(), any(Pageable.class)))
            .thenReturn(appointmentPage);
        when(appointmentService.countAppointmentsByStatus(any(AppointmentStatus.class)))
            .thenReturn(1L);

        // Act
        String result = controller.listAppointments(0, 10, "appointmentDate", "desc", 
            null, null, null, null, model, 
            createMockAuthentication()
        );

        // Assert
        assertEquals("admin/appointments", result);
        verify(model).addAttribute(eq("appointments"), eq(appointmentPage));
        verify(model).addAttribute(eq("statusCounts"), any(Map.class));
    }

    @Test
    void testListAppointments_WithSearch() {
        // Arrange
        List<Appointment> appointments = Arrays.asList(testAppointment);
        Page<Appointment> appointmentPage = new PageImpl<>(appointments);
        
        when(appointmentService.searchAppointments(eq("test"), any(Pageable.class)))
            .thenReturn(appointmentPage);
        when(appointmentService.countAppointmentsByStatus(any(AppointmentStatus.class)))
            .thenReturn(1L);

        // Act
        String result = controller.listAppointments(0, 10, "appointmentDate", "desc", 
            null, null, null, "test", model, 
            createMockAuthentication()
        );

        // Assert
        assertEquals("admin/appointments", result);
        verify(appointmentService).searchAppointments(eq("test"), any(Pageable.class));
    }

    @Test
    void testViewAppointment_Success() {
        // Arrange
        when(appointmentService.findById(1L)).thenReturn(testAppointment);

        // Act
        String result = controller.viewAppointment(1L, model, createMockAuthentication());

        // Assert
        assertEquals("admin/appointment-details", result);
        verify(model).addAttribute(eq("appointment"), eq(testAppointment));
    }

    @Test
    void testViewAppointment_NotFound() {
        // Arrange
        when(appointmentService.findById(1L))
            .thenThrow(new RuntimeException("Rendez-vous non trouvé"));

        // Act
        String result = controller.viewAppointment(1L, model, createMockAuthentication());

        // Assert
        assertEquals("redirect:/admin/appointments", result);
        verify(model).addAttribute(eq("errorMessage"), contains("non trouvé"));
    }

    @Test
    void testNewAppointmentForm_Success() {
        // Arrange
        when(userService.findAll()).thenReturn(Arrays.asList(testUser));

        // Act
        String result = controller.newAppointmentForm(model, createMockAuthentication());

        // Assert
        assertEquals("admin/appointment-form", result);
        verify(model).addAttribute(eq("appointmentForm"), any(AppointmentForm.class));
        verify(model).addAttribute(eq("users"), any(List.class));
    }

    @Test
    void testSaveAppointment_Success() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(appointmentService.createAppointment(any(AppointmentForm.class), any(User.class)))
            .thenReturn(testAppointment);

        // Act
        String result = controller.saveAppointment(testAppointmentForm, bindingResult, 
            1L, model, redirectAttributes, createMockAuthentication());

        // Assert
        assertEquals("redirect:/admin/appointments", result);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    void testSaveAppointment_ValidationErrors() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(true);
        when(userService.findAll()).thenReturn(Arrays.asList(testUser));

        // Act
        String result = controller.saveAppointment(testAppointmentForm, bindingResult, 
            1L, model, redirectAttributes, createMockAuthentication());

        // Assert
        assertEquals("admin/appointment-form", result);
        verify(model).addAttribute(eq("users"), any(List.class));
        verify(redirectAttributes, never()).addFlashAttribute(anyString(), anyString());
    }

    @Test
    void testEditAppointmentForm_Success() {
        // Arrange
        when(appointmentService.findById(1L)).thenReturn(testAppointment);
        when(userService.findAll()).thenReturn(Arrays.asList(testUser));

        // Act
        String result = controller.editAppointmentForm(1L, model, createMockAuthentication());

        // Assert
        assertEquals("admin/appointment-form", result);
        verify(model).addAttribute(eq("appointmentForm"), any(AppointmentForm.class));
        verify(model).addAttribute(eq("appointment"), eq(testAppointment));
    }

    @Test
    void testUpdateAppointment_Success() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(false);
        when(appointmentService.updateAppointment(eq(1L), any(AppointmentForm.class)))
            .thenReturn(testAppointment);

        // Act
        String result = controller.updateAppointment(1L, testAppointmentForm, bindingResult, 
            null, model, redirectAttributes, createMockAuthentication());

        // Assert
        assertEquals("redirect:/admin/appointments/1", result);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    void testUpdateAppointment_WithAdminNotes() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(false);
        when(appointmentService.updateAppointment(eq(1L), any(AppointmentForm.class)))
            .thenReturn(testAppointment);

        // Act
        String result = controller.updateAppointment(1L, testAppointmentForm, bindingResult, 
            "Admin notes", model, redirectAttributes, createMockAuthentication());

        // Assert
        assertEquals("redirect:/admin/appointments/1", result);
        verify(appointmentService).updateAdminNotes(1L, "Admin notes");
    }

    @Test
    void testDeleteAppointmentPermanent_Success() {
        // Arrange
        doNothing().when(appointmentService).deleteAppointment(eq(1L), anyString());

        // Act
        String result = controller.deleteAppointmentPermanent(1L, redirectAttributes, createMockAuthentication());

        // Assert
        assertEquals("redirect:/admin/appointments", result);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
        verify(appointmentService).deleteAppointment(eq(1L), anyString());
    }

    @Test
    void testConfirmAppointment_Success() {
        // Arrange
        testAppointment.setStatus(AppointmentStatus.CONFIRMED);
        when(appointmentService.confirmAppointment(eq(1L), eq("admin@test.com"))).thenReturn(testAppointment);

        // Act
        Map<String, Object> result = controller.confirmAppointment(1L, createMockAuthentication());

        // Assert
        assertTrue((Boolean) result.get("success"));
        assertEquals("Confirmé", result.get("newStatus"));
        verify(appointmentService).confirmAppointment(eq(1L), eq("admin@test.com"));
    }

    @Test
    void testConfirmAppointment_Error() {
        // Arrange
        when(appointmentService.confirmAppointment(eq(1L), eq("admin@test.com")))
            .thenThrow(new IllegalStateException("Cannot confirm"));

        // Act
        Map<String, Object> result = controller.confirmAppointment(1L, createMockAuthentication());

        // Assert
        assertFalse((Boolean) result.get("success"));
        assertTrue(((String) result.get("message")).contains("Cannot confirm"));
    }

    @Test
    void testCancelAppointment_Success() {
        // Arrange
        testAppointment.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentService.cancelAppointment(eq(1L), eq("Test reason"), eq("admin@test.com")))
            .thenReturn(testAppointment);

        // Act
        Map<String, Object> result = controller.cancelAppointment(1L, "Test reason", createMockAuthentication());

        // Assert
        assertTrue((Boolean) result.get("success"));
        assertEquals("Annulé", result.get("newStatus"));
    }

    @Test
    void testStartAppointment_Success() {
        // Arrange
        testAppointment.setStatus(AppointmentStatus.IN_PROGRESS);
        when(appointmentService.startAppointment(1L)).thenReturn(testAppointment);

        // Act
        Map<String, Object> result = controller.startAppointment(1L, createMockAuthentication());

        // Assert
        assertTrue((Boolean) result.get("success"));
        assertEquals("En cours", result.get("newStatus"));
    }

    @Test
    void testCompleteAppointment_Success() {
        // Arrange
        testAppointment.setStatus(AppointmentStatus.COMPLETED);
        when(appointmentService.completeAppointment(eq(1L), eq("Notes")))
            .thenReturn(testAppointment);

        // Act
        Map<String, Object> result = controller.completeAppointment(1L, "Notes", createMockAuthentication());

        // Assert
        assertTrue((Boolean) result.get("success"));
        assertEquals("Terminé", result.get("newStatus"));
    }

    @Test
    void testMarkAsNoShow_Success() {
        // Arrange
        testAppointment.setStatus(AppointmentStatus.NO_SHOW);
        when(appointmentService.markAsNoShow(1L)).thenReturn(testAppointment);

        // Act
        Map<String, Object> result = controller.markAsNoShow(1L, createMockAuthentication());

        // Assert
        assertTrue((Boolean) result.get("success"));
        assertEquals("Absence", result.get("newStatus"));
    }

    @Test
    void testExportAppointments() throws Exception {
        // Arrange
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        
        when(response.getWriter()).thenReturn(printWriter);
        
        List<Appointment> appointments = Arrays.asList(testAppointment);
        Page<Appointment> appointmentPage = new PageImpl<>(appointments);
        when(appointmentService.searchAppointments(anyString(), any(Pageable.class)))
            .thenReturn(appointmentPage);

        // Act
        controller.exportAppointments(null, null, null, "test", response, createMockAuthentication());

        // Assert
        verify(response).setContentType("text/csv");
        verify(response).setCharacterEncoding("UTF-8");
        verify(response).setHeader(eq("Content-Disposition"), contains("attachment"));
        
        printWriter.flush();
        String csvContent = stringWriter.toString();
        assertTrue(csvContent.contains("ID,Client,Email"));
        assertTrue(csvContent.contains("Test Appointment"));
    }

    // Méthode utilitaire pour créer une authentication mock
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
