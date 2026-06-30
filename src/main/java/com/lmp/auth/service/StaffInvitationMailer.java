package com.lmp.auth.service;

import com.lmp.auth.domain.StaffInvitation;
import com.lmp.auth.domain.User;
import com.lmp.auth.repository.UserRepository;
import com.lmp.notification.config.MailAddressConfig;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class StaffInvitationMailer {

    private static final Logger logger = LoggerFactory.getLogger(StaffInvitationMailer.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailAddressConfig mailAddressConfig;
    private final UserRepository userRepository;

    @Value("${company.name:LMP Services}")
    private String companyName;

    @Value("${company.website:http://localhost:8080}")
    private String companyWebsite;

    @Value("${app.frontend.url:${app.base.url:http://localhost:4200}}")
    private String frontendUrl;

    public StaffInvitationMailer(JavaMailSender mailSender,
                                 TemplateEngine templateEngine,
                                 MailAddressConfig mailAddressConfig,
                                 UserRepository userRepository) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.mailAddressConfig = mailAddressConfig;
        this.userRepository = userRepository;
    }

    @Async
    public void sendInvitationEmail(StaffInvitation invitation) {
        try {
            String acceptUrl = frontendUrl + "/accept-invitation?token=" + invitation.getToken();
            String inviterName = userRepository.findById(invitation.getInvitedBy())
                    .map(User::getDisplayName)
                    .orElse("Un administrateur");
            String recipientName = buildRecipientName(invitation);

            Context ctx = new Context();
            ctx.setVariable("companyName", companyName);
            ctx.setVariable("companyWebsite", companyWebsite);
            ctx.setVariable("acceptUrl", acceptUrl);
            ctx.setVariable("inviterName", inviterName);
            ctx.setVariable("recipientName", recipientName);
            ctx.setVariable("expiresAt", invitation.getExpiresAt());

            String html = templateEngine.process("emails/staff-invitation", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailAddressConfig.getNoreply(), mailAddressConfig.getName());
            helper.setReplyTo(mailAddressConfig.getNoreply());
            helper.setTo(invitation.getEmail());
            helper.setSubject("Invitation à rejoindre l'équipe " + companyName);
            helper.setText(html, true);

            mailSender.send(message);
            logger.info("[STAFF-INVITE] Email envoyé à {}", invitation.getEmail());

        } catch (Exception e) {
            logger.error("[STAFF-INVITE] Échec envoi email à {} : {}",
                    invitation.getEmail(), e.getMessage(), e);
        }
    }

    private String buildRecipientName(StaffInvitation invitation) {
        StringBuilder sb = new StringBuilder();
        if (invitation.getFirstName() != null && !invitation.getFirstName().isBlank()) {
            sb.append(invitation.getFirstName().trim());
        }
        if (invitation.getLastName() != null && !invitation.getLastName().isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(invitation.getLastName().trim());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
