namespace Lmp.Domain.Billing;

/// <summary>A refund against an order — maps to <c>refunds</c>.</summary>
public class Refund
{
    public Guid Id { get; set; }
    public Guid OrderId { get; set; }
    public Order Order { get; set; } = null!;

    public decimal Amount { get; set; }
    public string? Reason { get; set; }
    public string? Status { get; set; }
    public string? StripeRefundId { get; set; }
    public string? Currency { get; set; }
    public DateTime? CreatedAt { get; set; }
    public DateTime? ProcessedAt { get; set; }
    public string? ProcessedBy { get; set; }
    public string? FailureReason { get; set; }
    public string? Metadata { get; set; }
    public string? CancellationReason { get; set; }
    public DateTime? CancelledAt { get; set; }
    public string? CancelledBy { get; set; }
}
