using Lmp.Application.Pricing;
using Lmp.Domain.Billing;

namespace Lmp.Application.Billing;

/// <summary>Order response DTO. Faithful port of <c>com.lmp.billing.dto.OrderResponse</c>.</summary>
public sealed record OrderResponse(
    Guid Id,
    string? ServiceName,
    decimal TotalAmount,
    string? Currency,
    string? Status,
    string? PaymentStatus,
    string? PaymentMethod,
    DateTime? CreatedAt,
    DateTime? PaidAt,
    string? UserEmail,
    string? UserName,
    int? ProgressPercentage,
    string? ProgressStatus,
    string? ProcessingNotes,
    string? GuestPaymentLink,
    string? BillingName,
    string? BillingAddress,
    string? BillingCity,
    string? BillingPostalCode,
    string? BillingCountry,
    bool? VatReverseCharge,
    string? CustomerVatNumber,
    string? VatCompanyName,
    decimal? AppliedVatRate,
    decimal? AmountBaseEur,
    decimal? TotalAmountEur,
    string? IpCountry,
    string? IpAddress,
    decimal? VpnScore,
    string? VpnSources,
    string? BrowserTimezone,
    string? GeoCountry,
    string? CardCountry,
    int? FraudScore,
    string? FraudFlags)
{
    public static OrderResponse From(Order order) => ForAdmin(order, null);

    /// <summary>Admin view: includes <c>guestPaymentLink</c> for guest orders pending payment.</summary>
    public static OrderResponse ForAdmin(Order order, string? frontendBaseUrl)
        => new(
            order.Id,
            order.ServiceName,
            order.TotalAmount,
            order.Currency,
            order.Status.ToWire(),
            order.PaymentStatus,
            order.PaymentMethod,
            order.CreatedAt,
            order.PaidAt,
            order.User?.Email,
            order.User?.GetDisplayName(),
            order.ProgressPercentage,
            order.ProgressStatus,
            order.ProcessingNotes,
            ComputeGuestPaymentLink(order, frontendBaseUrl),
            order.BillingName,
            order.BillingAddress,
            order.BillingCity,
            order.BillingPostalCode,
            order.BillingCountry,
            order.VatReverseCharge,
            order.CustomerVatNumber,
            order.VatCompanyName,
            order.AppliedVatRate,
            order.AmountBaseEur,
            ComputeTotalAmountEur(order),
            order.IpCountry,
            order.IpAddress,
            order.VpnScore,
            order.VpnSources,
            order.BrowserTimezone,
            order.GeoCountry,
            order.CardCountry,
            order.FraudScore,
            order.FraudFlags);

    /// <summary>Gross (TTC) amount in EUR. Mirrors <c>OrderResponse.computeTotalAmountEur</c>.</summary>
    private static decimal? ComputeTotalAmountEur(Order order)
    {
        var currency = order.Currency;
        if (currency is null || string.Equals(currency, "EUR", StringComparison.OrdinalIgnoreCase))
        {
            return order.TotalAmount;
        }

        var baseEur = order.AmountBaseEur;
        if (baseEur is null)
        {
            return order.TotalAmount;
        }

        if (order.VatReverseCharge)
        {
            return baseEur;
        }

        var vatRate = order.AppliedVatRate ?? 0.20m;
        return MoneyUtils.Multiply(baseEur, 1m + vatRate);
    }

    /// <summary>Guest payment URL for the admin order detail. Mirrors <c>computeGuestPaymentLink</c>.</summary>
    public static string? ComputeGuestPaymentLink(Order order, string? frontendBaseUrl)
    {
        if (string.IsNullOrWhiteSpace(frontendBaseUrl))
        {
            return null;
        }

        var token = order.CheckoutToken;
        if (string.IsNullOrWhiteSpace(token) || order.Status != OrderStatus.PaymentPending)
        {
            return null;
        }

        var @base = frontendBaseUrl.TrimEnd('/');
        return $"{@base}/payment/guest?t={token.Trim()}";
    }
}
