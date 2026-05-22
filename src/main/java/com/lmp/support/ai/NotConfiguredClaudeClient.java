package com.lmp.support.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

/**
 * Default {@link ClaudeClient} bean used when no real binding (Anthropic SDK) is
 * wired. Throws on any call so the app starts cleanly but AI exec is disabled
 * until the live client lands. The live impl will replace this via
 * {@code @ConditionalOnMissingBean} ordering.
 */
@Configuration
public class NotConfiguredClaudeClient {

    @Bean
    @ConditionalOnMissingBean(ClaudeClient.class)
    ClaudeClient stubClaudeClient() {
        return (userPrompt, sessionContext) -> {
            throw new IllegalStateException(
                "ClaudeClient not configured. Provide an implementation backed by the Anthropic SDK "
                + "(set ANTHROPIC_API_KEY and bind a ClaudeClient bean).");
        };
    }
}
