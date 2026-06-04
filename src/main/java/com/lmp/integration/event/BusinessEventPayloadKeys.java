package com.lmp.integration.event;

/**
 * Clés standard du payload {@link LmpBusinessEvent} (évite les chaînes magiques entre émetteurs, SSE et persistance).
 */
public final class BusinessEventPayloadKeys {

    private BusinessEventPayloadKeys() {
    }

    public static final String USER_ID = "userId";
    public static final String ORDER_ID = "orderId";
    public static final String CUSTOMER_NAME = "customerName";
    public static final String CUSTOMER_EMAIL = "customerEmail";
    public static final String SERVICE_NAME = "serviceName";
    public static final String AMOUNT = "amount";
    /** Message affiché côté admin / toast unifié. */
    public static final String MESSAGE = "message";
    /**
     * Texte dédié notification in-app client (si absent lors d’un {@link #NOTIFY_USER}, retombée sur {@link #MESSAGE}).
     */
    public static final String USER_IN_APP_MESSAGE = "userInAppMessage";
    public static final String OLD_STATUS = "oldStatus";
    public static final String NEW_STATUS = "newStatus";
    public static final String ERROR = "error";
    public static final String STRIPE_REFUND_ID = "stripeRefundId";
    public static final String EMAIL = "email";
    public static final String CLIENT_NAME = "clientName";
    public static final String SUBJECT = "subject";
    /** Si {@code true}, persistance in-app + SSE utilisateur (types {@link #IN_APP_NOTIFICATION_TYPE}). */
    public static final String NOTIFY_USER = "notifyUser";

    /** Type notification client (ex. {@code STATUS_CHANGED}, {@code PAYMENT_SUCCESS}, {@code APPOINTMENT_CONFIRMED}). */
    public static final String IN_APP_NOTIFICATION_TYPE = "inAppNotificationType";

    /**
     * Si défini, force l’appel à {@code notifyDashboardUpdate} côté admin.
     * Si absent, la politique par défaut s’applique (ex. pas de ping pour facture générée).
     */
    public static final String SSE_DASHBOARD_PING = "sseDashboardPing";
}
