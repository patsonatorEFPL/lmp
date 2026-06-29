using System.Globalization;
using Lmp.Application.Auth;
using Lmp.Application.Common;
using Lmp.Application.Portal;
using Lmp.Domain.Billing;
using Lmp.Domain.Crm;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Lmp.Infrastructure.Portal;

/// <summary>EF Core dashboard use-cases. Faithful port of <c>UserDashboardRestController</c> logic.</summary>
public sealed class UserDashboardService(LmpDbContext db, IPasswordEncoder passwordEncoder) : IUserDashboardService
{
    private const string IsoFormat = "yyyy-MM-dd'T'HH:mm:ss.FFFFFFF";

    public async Task<DashboardStatsResponse> GetStatsAsync(Guid userId, CancellationToken ct = default)
    {
        var orders = await db.Orders.AsNoTracking()
            .Where(o => o.UserId == userId)
            .OrderByDescending(o => o.CreatedAt)
            .ToListAsync(ct);

        long totalOrders = orders.Count;
        long completedOrders = orders.Count(o => o.Status is OrderStatus.Completed or OrderStatus.Delivered);
        long inProgressOrders = orders.Count(o => o.Status is OrderStatus.InProgress or OrderStatus.Processing
            or OrderStatus.Confirmed or OrderStatus.Pending);
        var totalSpent = orders
            .Where(o => o.Status is not (OrderStatus.Cancelled or OrderStatus.Refunded))
            .Sum(o => o.TotalAmount);

        var recentOrders = orders.Take(5).Select(o => new DashboardStatsResponse.RecentOrderDto(
            o.Id.ToString(),
            o.ServiceName,
            o.Status.ToWire(),
            o.TotalAmount,
            o.Currency,
            Iso(o.CreatedAt))).ToList();

        var reviews = await db.Reviews.AsNoTracking().Where(r => r.UserId == userId).ToListAsync(ct);
        long totalReviews = reviews.Count;
        var recentReviews = reviews
            .OrderByDescending(r => r.CreatedAt ?? DateTime.MinValue)
            .Take(5)
            .Select(r => new DashboardStatsResponse.RecentReviewDto(
                r.Id.ToString(),
                r.Rating,
                r.Comment,
                r.AdminApproved,
                Iso(r.CreatedAt)))
            .ToList();

        var now = DateTime.Now;
        var upcoming = await db.Appointments.AsNoTracking()
            .Where(a => a.UserId == userId && a.AppointmentDate > now
                && (a.Status == AppointmentStatus.Pending || a.Status == AppointmentStatus.Confirmed))
            .OrderBy(a => a.AppointmentDate)
            .ToListAsync(ct);

        long upcomingCount = upcoming.Count;
        var upcomingList = upcoming.Take(5).Select(a => new DashboardStatsResponse.UpcomingAppointmentDto(
            a.Id.ToString(),
            a.Subject,
            a.Status.ToWire(),
            Iso(a.AppointmentDate),
            a.DurationMinutes ?? 60)).ToList();

        return new DashboardStatsResponse(
            totalOrders, completedOrders, inProgressOrders, totalReviews,
            upcomingCount, totalSpent, recentOrders, recentReviews, upcomingList);
    }

    public async Task<UserResponse> UpdateProfileAsync(Guid userId, UpdateProfileRequest request, CancellationToken ct = default)
    {
        var user = await db.Users.Include(u => u.Roles).FirstOrDefaultAsync(u => u.Id == userId, ct)
            ?? throw new InvalidOperationException("Utilisateur non trouvé");

        if (request.FirstName is not null) user.FirstName = request.FirstName;
        if (request.LastName is not null) user.LastName = request.LastName;
        if (request.Phone is not null) user.Phone = request.Phone;
        if (request.CompanyName is not null) user.CompanyName = request.CompanyName;
        if (request.City is not null) user.City = request.City;
        if (request.Country is not null) user.Country = request.Country;
        if (request.Address is not null) user.Address = request.Address;
        if (request.PostalCode is not null) user.PostalCode = request.PostalCode;

        ApplyVat(user, request);

        await db.SaveChangesAsync(ct);
        return UserResponse.From(user);
    }

    public async Task ChangePasswordAsync(Guid userId, string currentPassword, string newPassword, CancellationToken ct = default)
    {
        var user = await db.Users.FirstOrDefaultAsync(u => u.Id == userId, ct)
            ?? throw new InvalidOperationException("Utilisateur non trouvé avec l'ID: " + userId);

        if (!passwordEncoder.Matches(currentPassword, user.Password))
        {
            throw new InvalidOperationException("Le mot de passe actuel est incorrect");
        }

        if (passwordEncoder.Matches(newPassword, user.Password))
        {
            throw new InvalidOperationException("Le nouveau mot de passe doit être différent du mot de passe actuel");
        }

        if (!PasswordPolicy.IsStrong(newPassword))
        {
            throw new InvalidOperationException(
                "Le mot de passe doit contenir au moins 8 caractères, incluant majuscules, minuscules, chiffres et caractères spéciaux");
        }

        user.Password = passwordEncoder.Encode(newPassword);
        await db.SaveChangesAsync(ct);
    }

    private static void ApplyVat(Domain.Auth.User user, UpdateProfileRequest request)
    {
        if (request.VatReverseCharge is { } reverseCharge)
        {
            if (reverseCharge)
            {
                var vat = VatIdentifierUtils.Normalize(request.VatNumber);
                if (vat.Length == 0)
                {
                    throw new ArgumentException("Le numéro de TVA est obligatoire lorsque l'autoliquidation (auto-reverse) est activée.");
                }

                if (!VatIdentifierUtils.IsPlausibleEuVatFormat(vat))
                {
                    throw new ArgumentException(
                        "Format de numéro de TVA invalide : attendu un code pays à 2 lettres suivi de l'identifiant national (ex. FR12345678901, BE0123456789).");
                }

                user.VatReverseCharge = true;
                user.VatNumber = vat;
            }
            else
            {
                user.VatReverseCharge = false;
                user.VatNumber = null;
            }
        }
        else if (request.VatNumber is not null)
        {
            var vat = VatIdentifierUtils.Normalize(request.VatNumber);
            if (user.VatReverseCharge && vat.Length == 0)
            {
                throw new ArgumentException("Le numéro de TVA ne peut pas être vide.");
            }

            if (vat.Length != 0 && user.VatReverseCharge && !VatIdentifierUtils.IsPlausibleEuVatFormat(vat))
            {
                throw new ArgumentException(
                    "Format de numéro de TVA invalide : attendu un code pays à 2 lettres suivi de l'identifiant national (ex. FR12345678901, BE0123456789).");
            }

            user.VatNumber = vat.Length == 0 ? null : vat;
        }
    }

    private static string? Iso(DateTime? value)
        => value?.ToString(IsoFormat, CultureInfo.InvariantCulture);
}
