using Lmp.Application.Common;
using Lmp.Application.Crm;
using Lmp.Domain.Auth;
using Lmp.Domain.Crm;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Lmp.Infrastructure.Crm;

/// <summary>
/// Appointment booking service. Faithful port of the public-facing logic in
/// <c>com.lmp.crm.service.AppointmentService</c> (slot computation, conflict
/// detection, validation, anonymous vs. linked-user creation). Confirmation
/// emails are handled by the notification module (not yet ported) and do not
/// affect the HTTP contract.
/// </summary>
public sealed class AppointmentService(LmpDbContext db) : IAppointmentService
{
    private static readonly int[] AvailableHours = [9, 10, 11, 13, 14, 15, 16];

    public async Task<IReadOnlyList<DateTime>> GetAvailableTimeSlotsAsync(DateOnly date, CancellationToken ct = default)
    {
        var active = await LoadActiveAppointmentsAsync(ct);
        var now = DateTime.Now;
        var today = DateOnly.FromDateTime(now);

        var slots = new List<DateTime>();
        foreach (var hour in AvailableHours)
        {
            var slotStart = date.ToDateTime(new TimeOnly(hour, 0));
            var slotEnd = slotStart.AddHours(1);

            if (date == today && (slotStart <= now || slotStart < now.AddHours(1)))
            {
                continue;
            }

            if (!active.Any(a => Overlaps(slotStart, slotEnd, a)))
            {
                slots.Add(slotStart);
            }
        }

        return slots;
    }

    public async Task<Appointment> CreateAppointmentAsync(AppointmentRequest request, CancellationToken ct = default)
    {
        var when = request.GetAppointmentDateTime();
        var subject = request.ResolveServiceLabel();
        var description = request.Message;
        const int durationMinutes = 60;

        ValidateForm(when, subject, description);
        await CheckTimeConflictsAsync(when, durationMinutes, ct);

        var existingUser = await db.Users.FirstOrDefaultAsync(u => u.Email == request.Email, ct);

        var appointment = new Appointment
        {
            Subject = subject,
            Description = description,
            AppointmentDate = when,
            DurationMinutes = durationMinutes,
            Priority = 5,
            Status = AppointmentStatus.Pending,
            CreatedAt = DateTime.UtcNow,
        };

        if (existingUser is User user)
        {
            appointment.UserId = user.Id;
        }
        else
        {
            appointment.ClientName = request.Name;
            appointment.ClientEmail = request.Email;
            appointment.ClientPhone = request.Phone;
        }

        db.Appointments.Add(appointment);
        await db.SaveChangesAsync(ct);
        return appointment;
    }

    private static void ValidateForm(DateTime when, string subject, string? description)
    {
        if (!IsValidAppointmentTime(TimeOnly.FromDateTime(when)))
        {
            throw new ArgumentException("L'heure du rendez-vous doit être entre 9h et 17h, par créneaux de 30 minutes");
        }

        if (subject.Contains("autre", StringComparison.OrdinalIgnoreCase)
            && (description is null || description.Trim().Length < 20))
        {
            throw new ArgumentException("Pour le service 'Autre', veuillez décrire votre besoin en détail (minimum 20 caractères)");
        }

        var now = DateTime.Now;
        if (when < now.AddHours(24))
        {
            throw new ArgumentException("Les rendez-vous doivent être pris au moins 24h à l'avance");
        }

        var appointmentDate = DateOnly.FromDateTime(when);
        var today = DateOnly.FromDateTime(now);
        if (DateUtils.CalculateBusinessDaysBetween(today, appointmentDate) > 30)
        {
            throw new ArgumentException("La date sélectionnée est trop éloignée. Veuillez choisir une date dans les 30 prochains jours ouvrables.");
        }

        if (!DateUtils.IsBusinessDay(appointmentDate))
        {
            throw new ArgumentException("Les rendez-vous ne peuvent être pris que les jours ouvrables (lundi à vendredi)");
        }
    }

    private static bool IsValidAppointmentTime(TimeOnly time)
    {
        if (time < new TimeOnly(9, 0) || time > new TimeOnly(17, 0))
        {
            return false;
        }

        return time.Minute is 0 or 30;
    }

    private async Task CheckTimeConflictsAsync(DateTime start, int durationMinutes, CancellationToken ct)
    {
        var end = start.AddMinutes(durationMinutes);
        var active = await LoadActiveAppointmentsAsync(ct);
        var conflicts = active.Count(a => Overlaps(start, end, a));
        if (conflicts > 0)
        {
            throw new ArgumentException($"Un rendez-vous existe déjà à ce créneau horaire. {conflicts} conflit(s) détecté(s).");
        }
    }

    private Task<List<Appointment>> LoadActiveAppointmentsAsync(CancellationToken ct)
        => db.Appointments
            .AsNoTracking()
            .Where(a => a.Status == AppointmentStatus.Pending
                || a.Status == AppointmentStatus.Confirmed
                || a.Status == AppointmentStatus.InProgress)
            .OrderBy(a => a.AppointmentDate)
            .ToListAsync(ct);

    /// <summary>Half-open interval overlap: [start,end) intersects [apptStart, apptStart+duration).</summary>
    private static bool Overlaps(DateTime start, DateTime end, Appointment appt)
    {
        var apptStart = appt.AppointmentDate;
        var apptEnd = apptStart.AddMinutes(appt.DurationMinutes ?? 60);
        return start < apptEnd && end > apptStart;
    }
}
