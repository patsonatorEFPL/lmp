using Lmp.Domain.Billing;

namespace Lmp.Application.Billing;

/// <summary>Read-side order queries for the authenticated user. Backs OrderRestController.</summary>
public interface IOrderQueryService
{
    /// <summary>The user's orders, newest first (with user + refunds loaded).</summary>
    Task<IReadOnlyList<Order>> GetOrdersForUserAsync(Guid userId, CancellationToken ct = default);

    /// <summary>An order by id, only if it belongs to the user; otherwise null.</summary>
    Task<Order?> GetOrderForUserAsync(Guid orderId, Guid userId, CancellationToken ct = default);

    /// <summary>An order's refunds, newest first (ownership must be checked by the caller).</summary>
    Task<IReadOnlyList<Refund>> GetRefundsForOrderAsync(Guid orderId, CancellationToken ct = default);
}
