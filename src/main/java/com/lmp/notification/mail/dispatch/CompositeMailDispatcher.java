package com.lmp.notification.mail.dispatch;

import com.lmp.notification.mail.queue.EmailQueueEvent;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Dispatcher composite qui délègue à {@link SmtpMailDispatcher} ou
 * {@link ExternalCrmMailDispatcher} selon la stratégie active configurée
 * dans {@link EmailDispatcherConfigService}.
 */
@Component
@Primary
public class CompositeMailDispatcher implements MailDispatcher {

    private final SmtpMailDispatcher smtpDispatcher;
    private final ExternalCrmMailDispatcher externalCrmDispatcher;
    private final EmailDispatcherConfigService configService;

    public CompositeMailDispatcher(SmtpMailDispatcher smtpDispatcher,
                                   ExternalCrmMailDispatcher externalCrmDispatcher,
                                   EmailDispatcherConfigService configService) {
        this.smtpDispatcher = smtpDispatcher;
        this.externalCrmDispatcher = externalCrmDispatcher;
        this.configService = configService;
    }

    @Override
    public void send(EmailQueueEvent event) throws Exception {
        String strategy = configService.getActiveStrategy();
        switch (strategy) {
            case EmailDispatcherConfigService.STRATEGY_ERPNEXT -> externalCrmDispatcher.send(event);
            case EmailDispatcherConfigService.STRATEGY_SMTP -> smtpDispatcher.send(event);
            default -> throw new IllegalStateException("Unknown dispatcher strategy: " + strategy);
        }
    }
}
