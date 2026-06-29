using Lmp.Domain.Auth;

namespace Lmp.Domain.Billing;

/// <summary>
/// Customer order — maps to <c>orders</c>. This port models the fields consumed
/// by the read/checkout surface (more columns exist on the table and are added
/// as further billing features are ported).
/// </summary>
public class Order
{
    public Guid Id { get; set; }
    public Guid? UserId { get; set; }
    public User? User { get; set; }

    public string ServiceName { get; set; } = null!;
    public Guid? ServiceOfferId { get; set; }
    public decimal TotalAmount { get; set; }
    public string? Currency { get; set; } = "EUR";
    public OrderStatus Status { get; set; } = OrderStatus.PaymentPending;

    public string? PaymentMethod { get; set; }
    public string? PaymentStatus { get; set; }
    public string? StripeSessionId { get; set; }
    public string? StripePaymentIntentId { get; set; }

    public DateTime? CreatedAt { get; set; }
    public DateTime? UpdatedAt { get; set; }
    public DateTime? PaidAt { get; set; }

    public int? ProgressPercentage { get; set; }
    public string? ProgressStatus { get; set; }
    public string? ProcessingNotes { get; set; }
    public string? CheckoutToken { get; set; }

    // Billing snapshot.
    public string? BillingName { get; set; }
    public string? BillingAddress { get; set; }
    public string? BillingCity { get; set; }
    public string? BillingPostalCode { get; set; }
    public string? BillingCountry { get; set; }

    // VAT snapshot.
    public bool VatReverseCharge { get; set; }
    public string? CustomerVatNumber { get; set; }
    public string? VatCompanyName { get; set; }
    public decimal? AppliedVatRate { get; set; }

    // FX snapshot.
    public decimal? AmountBaseEur { get; set; }
    public decimal? FxRate { get; set; }
    public string? FxSource { get; set; }

    // Fraud scoring.
    public string? IpCountry { get; set; }
    public string? IpAddress { get; set; }
    public decimal? VpnScore { get; set; }
    public string? VpnSources { get; set; }
    public string? BrowserTimezone { get; set; }
    public string? GeoCountry { get; set; }
    public string? CardCountry { get; set; }
    public int? FraudScore { get; set; }
    public string? FraudFlags { get; set; }

    public ICollection<Refund> Refunds { get; set; } = new List<Refund>();
}
