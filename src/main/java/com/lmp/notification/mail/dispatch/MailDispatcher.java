package com.lmp.notification.mail.dispatch;

import com.lmp.notification.mail.queue.EmailQueueEvent;

/**
 * Stratégie d'envoi d'un email déjà rendu (depuis la queue).
 *
 * <p>Deux implémentations supportées :</p>
 * <ul>
 *   <li>{@link SmtpMailDispatcher} — Spring JavaMailSender direct (Mailtrap, SMTP générique). Default dev/staging.</li>
 *   <li>{@link ExternalCrmMailDispatcher} — relais via l'API du framework externe (Email Queue distante). Default prod.</li>
 * </ul>
 *
 * <p>Sélection par propriété {@code lmp.mail.dispatcher=smtp|external-crm}.</p>
 */
public interface MailDispatcher {

    /**
     * Envoie un email depuis la queue. Doit lever en cas d'échec pour déclencher
     * le retry exponentiel du {@code MailQueueProcessor}.
     */
    void send(EmailQueueEvent event) throws Exception;
}
