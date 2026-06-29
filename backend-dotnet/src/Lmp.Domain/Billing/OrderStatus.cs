namespace Lmp.Domain.Billing;

/// <summary>
/// Order lifecycle status — persisted as the Java enum name (e.g.
/// <c>PAYMENT_PENDING</c>). Use <see cref="OrderStatusExtensions"/> to convert
/// to/from the wire/DB string.
/// </summary>
public enum OrderStatus
{
    PaymentPending,
    Pending,
    Confirmed,
    Processing,
    InProgress,
    Shipped,
    Delivered,
    Completed,
    UnderReview,
    Cancelled,
    Refunded,
}

public static class OrderStatusExtensions
{
    public static string ToWire(this OrderStatus status) => status switch
    {
        OrderStatus.PaymentPending => "PAYMENT_PENDING",
        OrderStatus.Pending => "PENDING",
        OrderStatus.Confirmed => "CONFIRMED",
        OrderStatus.Processing => "PROCESSING",
        OrderStatus.InProgress => "IN_PROGRESS",
        OrderStatus.Shipped => "SHIPPED",
        OrderStatus.Delivered => "DELIVERED",
        OrderStatus.Completed => "COMPLETED",
        OrderStatus.UnderReview => "UNDER_REVIEW",
        OrderStatus.Cancelled => "CANCELLED",
        OrderStatus.Refunded => "REFUNDED",
        _ => status.ToString(),
    };

    public static OrderStatus FromWire(string value) => value switch
    {
        "PAYMENT_PENDING" => OrderStatus.PaymentPending,
        "PENDING" => OrderStatus.Pending,
        "CONFIRMED" => OrderStatus.Confirmed,
        "PROCESSING" => OrderStatus.Processing,
        "IN_PROGRESS" => OrderStatus.InProgress,
        "SHIPPED" => OrderStatus.Shipped,
        "DELIVERED" => OrderStatus.Delivered,
        "COMPLETED" => OrderStatus.Completed,
        "UNDER_REVIEW" => OrderStatus.UnderReview,
        "CANCELLED" => OrderStatus.Cancelled,
        "REFUNDED" => OrderStatus.Refunded,
        _ => throw new ArgumentOutOfRangeException(nameof(value), value, "Unknown order status"),
    };

    /// <summary>Whether an order in this status can have an invoice generated (≥ confirmed).</summary>
    public static bool IsInvoiceEligible(this OrderStatus status) => status switch
    {
        OrderStatus.Confirmed or OrderStatus.Processing or OrderStatus.InProgress
            or OrderStatus.Shipped or OrderStatus.Delivered or OrderStatus.Completed
            or OrderStatus.Refunded => true,
        _ => false,
    };
}
