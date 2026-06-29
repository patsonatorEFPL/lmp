using System.Text.Json;
using Lmp.Application.Common;
using Lmp.Application.Crm;

namespace Lmp.Application.Admin;

/// <summary>Admin appointment management. Port of the appointment methods of <c>AdminRestController</c>.</summary>
public interface IAdminAppointmentService
{
    /// <summary>Paginated appointments; optional status filter. Throws <see cref="ArgumentException"/> on an invalid status.</summary>
    Task<PagedResponse<AppointmentResponse>> ListAsync(int page, int size, string? status, CancellationToken ct = default);

    /// <summary>Detail map (with effective client + user info), or null if not found.</summary>
    Task<IDictionary<string, object?>?> GetDetailAsync(Guid id, CancellationToken ct = default);

    /// <summary>Apply status/adminNotes/priority/cancellationReason. Throws <see cref="ArgumentException"/> if not found or status invalid.</summary>
    Task UpdateAsync(Guid id, IDictionary<string, JsonElement> data, CancellationToken ct = default);

    Task DeleteAsync(Guid id, CancellationToken ct = default);
}
