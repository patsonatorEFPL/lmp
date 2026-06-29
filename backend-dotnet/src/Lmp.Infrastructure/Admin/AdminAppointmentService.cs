using System.Text.Json;
using Lmp.Application.Admin;
using Lmp.Application.Common;
using Lmp.Application.Crm;
using Lmp.Domain.Crm;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Lmp.Infrastructure.Admin;

/// <summary>EF Core admin appointment management. Faithful port of the AdminRestController methods.</summary>
public sealed class AdminAppointmentService(LmpDbContext db) : IAdminAppointmentService
{
    public async Task<PagedResponse<AppointmentResponse>> ListAsync(int page, int size, string? status, CancellationToken ct = default)
    {
        var query = db.Appointments.AsNoTracking().AsQueryable();

        if (!string.IsNullOrEmpty(status))
        {
            var parsed = ParseStatus(status);
            query = query.Where(a => a.Status == parsed).OrderBy(a => a.AppointmentDate);
        }
        else
        {
            query = query.OrderByDescending(a => a.AppointmentDate);
        }

        var total = await query.LongCountAsync(ct);
        var items = await query.Skip(page * size).Take(size).ToListAsync(ct);
        var content = items.Select(AppointmentResponse.From).ToList();
        return PagedResponse<AppointmentResponse>.Of(content, total, page, size, sorted: true);
    }

    public async Task<IDictionary<string, object?>?> GetDetailAsync(Guid id, CancellationToken ct = default)
    {
        var appt = await db.Appointments.AsNoTracking().Include(a => a.User).FirstOrDefaultAsync(a => a.Id == id, ct);
        if (appt is null)
        {
            return null;
        }

        var detail = new Dictionary<string, object?>(StringComparer.Ordinal)
        {
            ["id"] = appt.Id,
            ["clientName"] = appt.EffectiveClientName,
            ["clientEmail"] = appt.EffectiveClientEmail,
            ["clientPhone"] = appt.EffectiveClientPhone,
            ["appointmentDate"] = appt.AppointmentDate,
            ["subject"] = appt.Subject,
            ["description"] = appt.Description,
            ["status"] = appt.Status.ToWire(),
            ["adminNotes"] = appt.AdminNotes,
            ["durationMinutes"] = appt.DurationMinutes,
            ["priority"] = appt.Priority,
            ["createdAt"] = appt.CreatedAt,
            ["confirmedAt"] = appt.ConfirmedAt,
            ["cancelledAt"] = appt.CancelledAt,
            ["cancellationReason"] = appt.CancellationReason,
            ["isAnonymous"] = appt.IsAnonymous,
        };

        if (appt.User is not null)
        {
            detail["userId"] = appt.User.Id;
            detail["userName"] = appt.User.GetDisplayName();
            detail["userEmail"] = appt.User.Email;
        }

        return detail;
    }

    public async Task UpdateAsync(Guid id, IDictionary<string, JsonElement> data, CancellationToken ct = default)
    {
        var appt = await db.Appointments.FirstOrDefaultAsync(a => a.Id == id, ct)
            ?? throw new ArgumentException("Rendez-vous non trouvé");

        if (data.TryGetValue("status", out var status))
        {
            appt.Status = ParseStatus(status.GetString() ?? string.Empty);
        }

        if (data.TryGetValue("adminNotes", out var adminNotes))
        {
            appt.AdminNotes = adminNotes.ValueKind == JsonValueKind.Null ? null : adminNotes.GetString();
        }

        if (data.TryGetValue("priority", out var priority) && priority.ValueKind == JsonValueKind.Number)
        {
            appt.Priority = priority.GetInt32();
        }

        if (data.TryGetValue("cancellationReason", out var reason))
        {
            appt.CancellationReason = reason.ValueKind == JsonValueKind.Null ? null : reason.GetString();
        }

        await db.SaveChangesAsync(ct);
    }

    public async Task DeleteAsync(Guid id, CancellationToken ct = default)
        => await db.Appointments.Where(a => a.Id == id).ExecuteDeleteAsync(ct);

    private static AppointmentStatus ParseStatus(string status)
    {
        try
        {
            return AppointmentStatusExtensions.FromWire(status);
        }
        catch (ArgumentOutOfRangeException)
        {
            throw new ArgumentException("Invalid status: " + status);
        }
    }
}
