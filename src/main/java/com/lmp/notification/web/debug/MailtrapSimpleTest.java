package com.lmp.notification.web.debug;

import io.mailtrap.client.MailtrapClient;
import io.mailtrap.config.MailtrapConfig;
import io.mailtrap.factory.MailtrapClientFactory;
import io.mailtrap.model.request.emails.Address;
import io.mailtrap.model.request.emails.MailtrapMail;

import java.util.List;

/**
 * Test simple pour vérifier la configuration Mailtrap
 * Basé sur votre exemple
 */
public class MailtrapSimpleTest {

    // IMPORTANT: Ne jamais committer de tokens en dur — utiliser les variables d'environnement
    private static final String TOKEN = System.getenv("MAILTRAP_API_TOKEN") != null 
            ? System.getenv("MAILTRAP_API_TOKEN") : "CHANGE_ME";

    public static void main(String[] args) {
        System.out.println("🔧 Test de configuration Mailtrap...");
        
        final MailtrapConfig config = new MailtrapConfig.Builder()
            .token(TOKEN)
            .build();

        final MailtrapClient client = MailtrapClientFactory.createMailtrapClient(config);

        final MailtrapMail mail = MailtrapMail.builder()
            .from(new Address(System.getenv("MAIL_FROM_NOREPLY") != null ? System.getenv("MAIL_FROM_NOREPLY") : "noreply@localhost", "Mailtrap Test"))
            .to(List.of(new Address(args.length > 0 ? args[0] : "test@example.com")))
            .subject("You are awesome!")
            .text("Congrats for sending test email with Mailtrap!")
            .category("Integration Test")
            .build();

        try {
            var result = client.send(mail);
            System.out.println("✅ Email envoyé avec succès ! Résultat: " + result.toString());
        } catch (Exception e) {
            System.out.println("❌ Erreur lors de l'envoi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
