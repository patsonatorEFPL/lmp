using Lmp.Application.Billing;
using Lmp.Domain.Billing;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Lmp.Infrastructure.Billing;

/// <summary>EF Core read-side order queries. Backs <c>OrderRestController</c>.</summary>
public sealed class OrderQueryService(LmpDbContext db) : IOrderQueryService
{
    public async Task<IReadOnlyList<Order>> GetOrdersForUserAsync(Guid userId, CancellationToken ct = default)
        => await db.Orders
            .AsNoTracking()
            .Include(o => o.User)
            .Where(o => o.UserId == userId)
            .OrderByDescending(o => o.CreatedAt)
            .ToListAsync(ct);

    public async Task<Order?> GetOrderForUserAsync(Guid orderId, Guid userId, CancellationToken ct = default)
        => await db.Orders
            .AsNoTracking()
            .Include(o => o.User)
            .FirstOrDefaultAsync(o => o.Id == orderId && o.UserId == userId, ct);

    public async Task<IReadOnlyList<Refund>> GetRefundsForOrderAsync(Guid orderId, CancellationToken ct = default)
        => await db.Refunds
            .AsNoTracking()
            .Where(r => r.OrderId == orderId)
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync(ct);
}
