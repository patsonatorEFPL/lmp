using Lmp.Application.Admin;
using Lmp.Application.Auth;
using Lmp.Application.Billing;
using Lmp.Application.Common;
using Lmp.Application.SiteConfiguration;
using Lmp.Domain.Auth;
using Lmp.Domain.Billing;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Lmp.Infrastructure.Admin;

/// <summary>EF Core admin read queries. Faithful port of the read methods of <c>AdminRestController</c>.</summary>
public sealed class AdminQueryService(LmpDbContext db, ISiteConfigManager siteConfig) : IAdminQueryService
{
    public async Task<IDictionary<string, object>> GetStatsAsync(CancellationToken ct = default)
    {
        var startOfMonth = new DateTime(DateTime.Now.Year, DateTime.Now.Month, 1, 0, 0, 0, DateTimeKind.Unspecified);
        var today = DateTime.Today;

        var stats = new Dictionary<string, object>(StringComparer.Ordinal)
        {
            ["totalUsers"] = await db.Users.LongCountAsync(ct),
            ["activeUsers"] = await db.Users.LongCountAsync(u => u.Status == UserStatus.Active, ct),
            ["totalOrders"] = await db.Orders.LongCountAsync(ct),
            ["totalAppointments"] = await db.Appointments.LongCountAsync(ct),
            ["newUsersThisMonth"] = await db.Users.LongCountAsync(u => u.RegistrationDate >= startOfMonth, ct),
            ["appointmentsToday"] = await db.Appointments.LongCountAsync(a => a.AppointmentDate.Date == today, ct),
        };

        var (mrr, recurring, oneTime) = await ComputeRevenueAsync(ct);
        stats["mrr"] = mrr;
        stats["recurringRevenue30d"] = recurring;
        stats["oneTimeRevenue30d"] = oneTime;

        var byStatus = await db.Orders
            .GroupBy(o => o.Status)
            .Select(g => new { Status = g.Key, Count = g.LongCount(), Revenue = g.Sum(o => o.TotalAmount) })
            .ToListAsync(ct);

        var ordersByStatus = new Dictionary<string, object>(StringComparer.Ordinal);
        foreach (var row in byStatus)
        {
            ordersByStatus[row.Status.ToWire()] = new Dictionary<string, object>
            {
                ["count"] = row.Count,
                ["revenue"] = row.Revenue,
            };
        }

        stats["ordersByStatus"] = ordersByStatus;
        return stats;
    }

    public async Task<PagedResponse<UserResponse>> GetUsersAsync(int page, int size, string? status, string? search, CancellationToken ct = default)
    {
        var safeSize = Math.Clamp(size, 1, 500);
        var query = db.Users.AsNoTracking().Include(u => u.Roles).AsQueryable();

        if (!string.IsNullOrWhiteSpace(search))
        {
            var term = $"%{search.Trim()}%";
            query = query.Where(u => u.Email != null && EF.Functions.ILike(u.Email, term));
        }
        else if (!string.IsNullOrEmpty(status))
        {
            var parsed = ParseUserStatus(status);
            query = query.Where(u => u.Status == parsed);
        }

        var total = await query.LongCountAsync(ct);
        var users = await query
            .OrderByDescending(u => u.RegistrationDate)
            .Skip(page * safeSize)
            .Take(safeSize)
            .ToListAsync(ct);

        var content = users.Select(UserResponse.From).ToList();
        return PagedResponse<UserResponse>.Of(content, total, page, safeSize, sorted: true);
    }

    public async Task<PagedResponse<OrderResponse>> GetOrdersAsync(int page, int size, string? status, CancellationToken ct = default)
    {
        var frontendUrl = siteConfig.GetFrontendUrl();
        var query = db.Orders.AsNoTracking().Include(o => o.User).AsQueryable();

        if (!string.IsNullOrEmpty(status))
        {
            var parsed = ParseOrderStatus(status);
            query = query.Where(o => o.Status == parsed);
        }

        var total = await query.LongCountAsync(ct);
        var orders = await query
            .OrderByDescending(o => o.CreatedAt)
            .Skip(page * size)
            .Take(size)
            .ToListAsync(ct);

        var content = orders.Select(o => OrderResponse.ForAdmin(o, frontendUrl)).ToList();
        return PagedResponse<OrderResponse>.Of(content, total, page, size, sorted: true);
    }

    private async Task<(decimal Mrr, decimal Recurring, decimal OneTime)> ComputeRevenueAsync(CancellationToken ct)
    {
        var thirtyDaysAgo = DateTime.Now.AddDays(-30);
        const string sql = """
            SELECT
                CASE
                    WHEN so.duration_type = 'MONTHLY' THEN 'MONTHLY'
                    WHEN so.duration_type = 'YEARLY' THEN 'YEARLY'
                    ELSE 'ONE_TIME'
                END AS rev_type,
                SUM(o.total_amount - COALESCE(r.refunded, 0)) AS total
            FROM orders o
            LEFT JOIN services s ON LOWER(o.service_name) = LOWER(s.title)
            LEFT JOIN service_offers so ON so.service_id = s.id AND so.is_default = true
            LEFT JOIN (
                SELECT order_id, SUM(amount) AS refunded
                FROM refunds WHERE status = 'COMPLETED' GROUP BY order_id
            ) r ON r.order_id = o.id
            WHERE o.created_at >= @startDate
              AND o.status IN ('CONFIRMED','PROCESSING','IN_PROGRESS','SHIPPED','DELIVERED','COMPLETED','UNDER_REVIEW')
            GROUP BY 1
            """;

        decimal mrr = 0m, recurring = 0m, oneTime = 0m;
        var connection = db.Database.GetDbConnection();
        var shouldClose = connection.State != System.Data.ConnectionState.Open;
        if (shouldClose)
        {
            await connection.OpenAsync(ct);
        }

        try
        {
            await using var cmd = connection.CreateCommand();
            cmd.CommandText = sql;
            var p = cmd.CreateParameter();
            p.ParameterName = "startDate";
            p.Value = thirtyDaysAgo;
            cmd.Parameters.Add(p);

            await using var reader = await cmd.ExecuteReaderAsync(ct);
            while (await reader.ReadAsync(ct))
            {
                if (reader.IsDBNull(1))
                {
                    continue;
                }

                var type = reader.GetString(0);
                var total = reader.GetDecimal(1);
                switch (type)
                {
                    case "MONTHLY":
                        mrr += total;
                        recurring += total;
                        break;
                    case "YEARLY":
                        mrr += Math.Round(total / 12m, 2, MidpointRounding.AwayFromZero);
                        recurring += total;
                        break;
                    default:
                        oneTime += total;
                        break;
                }
            }
        }
        catch (Exception)
        {
            // Non-blocking: a schema mismatch returns zeros, mirroring the Java try/catch.
        }
        finally
        {
            if (shouldClose)
            {
                await connection.CloseAsync();
            }
        }

        return (mrr, recurring, oneTime);
    }

    private static UserStatus ParseUserStatus(string status) => status switch
    {
        "ACTIVE" => UserStatus.Active,
        "INACTIVE" => UserStatus.Inactive,
        "DELETED" => UserStatus.Deleted,
        _ => throw new ArgumentException("Invalid status: " + status),
    };

    private static OrderStatus ParseOrderStatus(string status)
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
