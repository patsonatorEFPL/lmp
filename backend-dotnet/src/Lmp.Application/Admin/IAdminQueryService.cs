using Lmp.Application.Auth;
using Lmp.Application.Billing;
using Lmp.Application.Common;

namespace Lmp.Application.Admin;

/// <summary>Admin read queries. Backs the read endpoints of <c>AdminRestController</c>.</summary>
public interface IAdminQueryService
{
    /// <summary>Dashboard KPIs (counts, MRR/revenue split, orders-by-status).</summary>
    Task<IDictionary<string, object>> GetStatsAsync(CancellationToken ct = default);

    /// <summary>Paginated users (newest first); optional status filter or email search. Throws <see cref="ArgumentException"/> on an invalid status.</summary>
    Task<PagedResponse<UserResponse>> GetUsersAsync(int page, int size, string? status, string? search, CancellationToken ct = default);

    /// <summary>Paginated orders (newest first); optional status filter. Throws <see cref="ArgumentException"/> on an invalid status.</summary>
    Task<PagedResponse<OrderResponse>> GetOrdersAsync(int page, int size, string? status, CancellationToken ct = default);

    /// <summary>Revenue line-chart series. <paramref name="period"/> is <c>week</c>, <c>month</c> or <c>quarter</c>; throws <see cref="ArgumentException"/> otherwise.</summary>
    Task<RevenueSeriesResponse> GetRevenueSeriesAsync(string period, CancellationToken ct = default);

    /// <summary>Top 5 services by real revenue over the last 90 days, with growth vs the comparison window.</summary>
    Task<IReadOnlyList<TopServiceResponse>> GetTopServicesAsync(CancellationToken ct = default);
}
