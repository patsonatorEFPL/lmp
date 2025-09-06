import java.time.LocalDateTime;
import com.lmp.domain.dto.AppointmentForm;
import com.lmp.domain.dto.AppointmentRequest;

public class DebugTest {
    public static void main(String[] args) {
        try {
            // Test AppointmentRequest
            AppointmentRequest request = new AppointmentRequest();
            request.setName("Test");
            request.setEmail("test@test.com");
            request.setPhone("0123456789");
            request.setService("consultation");
            request.setDate("2025-09-17");
            request.setTime("09:00");
            request.setMessage("Test simple");
            
            System.out.println("AppointmentRequest créé: " + request);
            
            // Test de conversion vers AppointmentForm
            AppointmentForm form = request.toAppointmentForm();
            System.out.println("AppointmentForm créé: " + form);
            
            // Test des validations
            System.out.println("isValidAppointmentTime: " + form.isValidAppointmentTime());
            System.out.println("isProfessionalSubject: " + form.isProfessionalSubject());
            
            // Test de la date
            LocalDateTime dateTime = request.getAppointmentDateTime();
            System.out.println("DateTime parsé: " + dateTime);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
