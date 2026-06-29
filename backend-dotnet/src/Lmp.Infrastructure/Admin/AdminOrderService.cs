using System.Text.Json;
using Lmp.Application.Admin;
using Lmp.Application.Billing;
using Lmp.Application.Pricing;
using Lmp.Application.SiteConfiguration;
using Lmp.Domain.Billing;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Lmp.Infrastructure.Admin;

/// <summary>EF Core admin order management. Faithful port of the AdminRestController order methods.</summary>
public sealed class AdminOrderService(LmpDbContext db, ISiteConfigManager siteConfig) : IAdminOrderService
{
    public async Task<IDictionary<string, object?>?> GetDetailAsync(Guid id, CancellationToken ct = default)
    {
        var order = await db.Orders.AsNoTracking().Include(o => o.User).FirstOrDefaultAsync(o => o.Id == id, ct);
        if (order is null)
        {
            return null;
        }

        var detail = new Dictionary<string, object?>(StringComparer.Ordinal)
        {
            ["id"] = order.Id,
            ["serviceName"] = order.ServiceName,
            ["totalAmount"] = order.TotalAmount,
            ["currency"] = order.Currency,
            ["status"] = order.Status.ToWire(),
            ["paymentStatus"] = order.PaymentStatus,
            ["paymentMethod"] = order.PaymentMethod,
            ["createdAt"] = order.CreatedAt,
            ["paidAt"] = order.PaidAt,
            ["shippedAt"] = order.ShippedAt,
            ["deliveredAt"] = order.DeliveredAt,
            ["cancelledAt"] = order.CancelledAt,
            ["notes"] = order.Notes,
            ["adminNotes"] = order.AdminNotes,
            ["processingNotes"] = order.ProcessingNotes,
            ["progressPercentage"] = order.ProgressPercentage,
            ["progressStatus"] = order.ProgressStatus,
            ["priority"] = order.Priority,
            ["stripeSessionId"] = order.StripeSessionId,
            ["stripePaymentIntentId"] = order.StripePaymentIntentId,
            ["cancellationReason"] = order.CancellationReason,
            ["amountBaseEur"] = order.AmountBaseEur,
            ["appliedVatRate"] = order.AppliedVatRate,
            ["totalAmountEur"] = ComputeTotalAmountEur(order),
            ["billingAddress"] = order.BillingAddress,
            ["billingCity"] = order.BillingCity,
            ["billingPostalCode"] = order.BillingPostalCode,
            ["billingCountry"] = order.BillingCountry,
        };

        if (order.User is not null)
        {
            detail["userEmail"] = order.User.Email;
            detail["userName"] = order.User.GetDisplayName();
            detail["userId"] = order.User.Id;
        }

        detail["guestPaymentLink"] = OrderResponse.ComputeGuestPaymentLink(order, siteConfig.GetFrontendUrl());
        detail["billingName"] = order.BillingName;
        detail["customerVatNumber"] = order.CustomerVatNumber;
        detail["vatCompanyName"] = order.VatCompanyName;
        detail["vatReverseCharge"] = order.VatReverseCharge;
        detail["ipCountry"] = order.IpCountry;
        detail["ipAddress"] = order.IpAddress;
        detail["vpnScore"] = order.VpnScore;
        detail["vpnSources"] = order.VpnSources;
        detail["browserTimezone"] = order.BrowserTimezone;
        detail["geoCountry"] = order.GeoCountry;
        detail["cardCountry"] = order.CardCountry;
        detail["fraudScore"] = order.FraudScore;
        detail["fraudFlags"] = order.FraudFlags;
        return detail;
    }

    public async Task UpdateAsync(Guid id, IDictionary<string, JsonElement> data, CancellationToken ct = default)
    {
        var order = await db.Orders.FirstOrDefaultAsync(o => o.Id == id, ct)
            ?? throw new ArgumentException("Commande non trouvée");

        var statusChanged = false;
        if (data.TryGetValue("status", out var status))
        {
            order.Status = ParseStatus(status.GetString() ?? string.Empty);
            statusChanged = true;
        }

        if (data.TryGetValue("progressPercentage", out var pct) && pct.ValueKind == JsonValueKind.Number)
        {
            order.ProgressPercentage = pct.GetInt32();
        }

        if (data.TryGetValue("progressStatus", out var ps))
        {
            order.ProgressStatus = ps.ValueKind == JsonValueKind.Null ? null : ps.GetString();
        }

        if (statusChanged)
        {
            OrderProgressSync.ApplyMinimumForStatus(order);
        }

        if (data.TryGetValue("adminNotes", out var an))
        {
            order.AdminNotes = an.ValueKind == JsonValueKind.Null ? null : an.GetString();
        }

        if (data.TryGetValue("processingNotes", out var pn))
        {
            order.ProcessingNotes = pn.ValueKind == JsonValueKind.Null ? null : pn.GetString();
        }

        if (data.TryGetValue("priority", out var prio) && prio.ValueKind == JsonValueKind.Number)
        {
            order.Priority = prio.GetInt32();
        }

        var now = DateTime.UtcNow;
        order.UpdatedAt = now;
        order.LastModifiedAt = now;
        await db.SaveChangesAsync(ct);
    }

    public async Task<DeleteOrderResult> DeleteAsync(Guid id, CancellationToken ct = default)
    {
        var order = await db.Orders.FirstOrDefaultAsync(o => o.Id == id, ct);
        if (order is null)
        {
            return DeleteOrderResult.NotFound;
        }

        if (order.PaidAt is not null)
        {
            return DeleteOrderResult.Paid;
        }

        if (order.Status is not (OrderStatus.PaymentPending or OrderStatus.Cancelled))
        {
            return DeleteOrderResult.InvalidStatus;
        }

        if (await db.Refunds.AnyAsync(r => r.OrderId == id, ct))
        {
            return DeleteOrderResult.HasRefunds;
        }

        db.Orders.Remove(order);
        await db.SaveChangesAsync(ct);
        return DeleteOrderResult.Deleted;
    }

    private static decimal? ComputeTotalAmountEur(Order order)
    {
        if (order.Currency is null || string.Equals(order.Currency, "EUR", StringComparison.OrdinalIgnoreCase))
        {
            return order.TotalAmount;
        }

        if (order.AmountBaseEur is not { } baseEur)
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

    private static OrderStatus ParseStatus(string status)
    {
        try
        {
            return OrderStatusExtensions.FromWire(status);
        }
        catch (ArgumentOutOfRangeException)
        {
            throw new ArgumentException("Invalid status: " + status);
        }
    }
}
