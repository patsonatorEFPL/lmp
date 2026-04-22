package com.lmp.integration.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration de la synchronisation avec un système externe.
 * <p>
 * Préfixe : {@code lmp.sync.*} dans application.properties.
 */
@Component
@ConfigurationProperties(prefix = "lmp.sync")
public class SyncProperties {

    private boolean enabled = false;
    private External external = new External();
    private Webhook webhook = new Webhook();
    private Features features = new Features();
    private Retry retry = new Retry();
    private Queue queue = new Queue();
    private Reconciliation reconciliation = new Reconciliation();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public External getExternal() { return external; }
    public void setExternal(External external) { this.external = external; }
    public Webhook getWebhook() { return webhook; }
    public void setWebhook(Webhook webhook) { this.webhook = webhook; }
    public Features getFeatures() { return features; }
    public void setFeatures(Features features) { this.features = features; }
    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }
    public Queue getQueue() { return queue; }
    public void setQueue(Queue queue) { this.queue = queue; }
    public Reconciliation getReconciliation() { return reconciliation; }
    public void setReconciliation(Reconciliation reconciliation) { this.reconciliation = reconciliation; }

    public static class External {
        private String baseUrl = "";
        private String apiKey = "";
        private String apiSecret = "";
        private String currency = "EUR";
        private String company = "LMP Services";
        /** Compte comptable de TVA par défaut (fallback). */
        private String taxAccount = "";
        /**
         * Mapping taux TVA (%) → compte comptable ERP.
         * Ex: taxAccounts.21 = "VAT 21% - LS", taxAccounts.12 = "VAT 12% - LS"
         * Si le taux exact n'est pas trouvé, utilise {@code taxAccount} comme fallback.
         */
        private Map<String, String> taxAccounts = new HashMap<>();
        /** Soumettre automatiquement les Sales Order / Invoice après création. */
        private boolean autoSubmit = true;
        /** Compte bancaire pour les Payment Entry (paid_to). */
        private String paymentAccount = "";
        /** Compte débiteur pour les Payment Entry (paid_from). */
        private String receivableAccount = "";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getApiSecret() { return apiSecret; }
        public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }
        public String getTaxAccount() { return taxAccount; }
        public void setTaxAccount(String taxAccount) { this.taxAccount = taxAccount; }
        public Map<String, String> getTaxAccounts() { return taxAccounts; }
        public void setTaxAccounts(Map<String, String> taxAccounts) { this.taxAccounts = taxAccounts; }
        public boolean isAutoSubmit() { return autoSubmit; }
        public void setAutoSubmit(boolean autoSubmit) { this.autoSubmit = autoSubmit; }
        public String getPaymentAccount() { return paymentAccount; }
        public void setPaymentAccount(String paymentAccount) { this.paymentAccount = paymentAccount; }
        public String getReceivableAccount() { return receivableAccount; }
        public void setReceivableAccount(String receivableAccount) { this.receivableAccount = receivableAccount; }

        /**
         * Résout le compte comptable de taxe pour un taux donné (en %).
         * Cherche d'abord une correspondance exacte dans taxAccounts, puis le taux arrondi,
         * sinon utilise le taxAccount par défaut.
         */
        public String resolveTaxAccount(java.math.BigDecimal vatPercent) {
            if (vatPercent == null) return taxAccount;
            // Clé exacte (ex: "21.00", "20", "12")
            String key = vatPercent.stripTrailingZeros().toPlainString();
            if (taxAccounts.containsKey(key)) return taxAccounts.get(key);
            // Clé entière arrondie (ex: "21", "20", "12")
            String intKey = String.valueOf(vatPercent.intValue());
            if (taxAccounts.containsKey(intKey)) return taxAccounts.get(intKey);
            // Fallback
            return taxAccount;
        }
    }

    public static class Webhook {
        private String hmacSecret = "";

        public String getHmacSecret() { return hmacSecret; }
        public void setHmacSecret(String hmacSecret) { this.hmacSecret = hmacSecret; }
    }

    public static class Features {
        private boolean userProvisioning = true;
        private boolean catalogSync = true;
        private boolean orderSync = true;
        private boolean projectSync = false;
        private boolean ticketSync = false;

        public boolean isUserProvisioning() { return userProvisioning; }
        public void setUserProvisioning(boolean userProvisioning) { this.userProvisioning = userProvisioning; }
        public boolean isCatalogSync() { return catalogSync; }
        public void setCatalogSync(boolean catalogSync) { this.catalogSync = catalogSync; }
        public boolean isOrderSync() { return orderSync; }
        public void setOrderSync(boolean orderSync) { this.orderSync = orderSync; }
        public boolean isProjectSync() { return projectSync; }
        public void setProjectSync(boolean projectSync) { this.projectSync = projectSync; }
        public boolean isTicketSync() { return ticketSync; }
        public void setTicketSync(boolean ticketSync) { this.ticketSync = ticketSync; }
    }

    public static class Retry {
        private int maxAttempts = 5;
        private long delaySeconds = 60;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public long getDelaySeconds() { return delaySeconds; }
        public void setDelaySeconds(long delaySeconds) { this.delaySeconds = delaySeconds; }
    }

    public static class Queue {
        private int batchSize = 10;
        private long pollIntervalMs = 5000;

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public long getPollIntervalMs() { return pollIntervalMs; }
        public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
    }

    public static class Reconciliation {
        private boolean enabled = false;
        private int initialIntervalSeconds = 30;
        private int maxIntervalSeconds = 900;
        private int incrementSeconds = 30;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getInitialIntervalSeconds() { return initialIntervalSeconds; }
        public void setInitialIntervalSeconds(int initialIntervalSeconds) { this.initialIntervalSeconds = initialIntervalSeconds; }
        public int getMaxIntervalSeconds() { return maxIntervalSeconds; }
        public void setMaxIntervalSeconds(int maxIntervalSeconds) { this.maxIntervalSeconds = maxIntervalSeconds; }
        public int getIncrementSeconds() { return incrementSeconds; }
        public void setIncrementSeconds(int incrementSeconds) { this.incrementSeconds = incrementSeconds; }
    }
}
