package com.lmp.test;

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

    private static final String TOKEN = "***MAILTRAP_TOKEN_REMOVED***";

    public static void main(String[] args) {
        System.out.println("🔧 Test de configuration Mailtrap...");
        
        final MailtrapConfig config = new MailtrapConfig.Builder()
            .token(TOKEN)
            .build();

        final MailtrapClient client = MailtrapClientFactory.createMailtrapClient(config);

        final MailtrapMail mail = MailtrapMail.builder()
            .from(new Address("hello@lmp-services.ca", "Mailtrap Test"))
            .to(List.of(new Address("patsonator32@gmail.com")))
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
