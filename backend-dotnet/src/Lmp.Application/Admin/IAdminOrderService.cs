using System.Text.Json;

namespace Lmp.Application.Admin;

/// <summary>Outcome of an admin order delete (maps to HTTP status + message).</summary>
public enum DeleteOrderResult
{
    Deleted,
    NotFound,
    Paid,
    InvalidStatus,
    HasRefunds,
}

/// <summary>Admin order management. Port of the order write methods of <c>AdminRestController</c>.</summary>
public interface IAdminOrderService
{
    /// <summary>Full order detail map (admin view), or null if not found.</summary>
    Task<IDictionary<string, object?>?> GetDetailAsync(Guid id, CancellationToken ct = default);

    /// <summary>Apply status/progress/notes/priority; floors progress on a status change. Throws <see cref="ArgumentException"/> if not found or status invalid.</summary>
    Task UpdateAsync(Guid id, IDictionary<string, JsonElement> data, CancellationToken ct = default);

    /// <summary>Delete only an unpaid pending/cancelled order without refunds.</summary>
    Task<DeleteOrderResult> DeleteAsync(Guid id, CancellationToken ct = default);
}
