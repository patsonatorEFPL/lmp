using System.Text.Json;
using Lmp.Application.Admin;
using Lmp.Domain.Auth;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Lmp.Infrastructure.Admin;

/// <summary>EF Core admin user management. Faithful port of the AdminRestController/UserService methods.</summary>
public sealed class AdminUserService(LmpDbContext db) : IAdminUserService
{
    public async Task UpdateUserAsync(Guid id, IDictionary<string, JsonElement> data, Guid actorId, CancellationToken ct = default)
    {
        var user = await db.Users.Include(u => u.Roles).FirstOrDefaultAsync(u => u.Id == id, ct)
            ?? throw new ArgumentException("Utilisateur non trouvé");

        if (data.TryGetValue("admin", out var admin))
        {
            await SetAdminRoleAsync(user, admin.ValueKind == JsonValueKind.True, actorId, ct);
        }

        if (data.TryGetValue("status", out var status))
        {
            user.Status = status.GetString() == "ACTIVE" ? UserStatus.Active : UserStatus.Inactive;
        }

        if (data.TryGetValue("locked", out var locked))
        {
            user.AccountLocked = locked.ValueKind == JsonValueKind.True;
        }

        if (data.TryGetValue("email", out var emailEl) && emailEl.GetString() is { } rawEmail)
        {
            var newEmail = rawEmail.Trim().ToLowerInvariant();
            if (!string.Equals(newEmail, user.Email, StringComparison.Ordinal))
            {
                if (await db.Users.AnyAsync(u => u.Email == newEmail && u.Id != id, ct))
                {
                    throw new ArgumentException("Cet email est déjà utilisé par un autre compte");
                }

                // Email change initiates re-verification (the notification email is
                // sent by the SMTP module, not yet ported).
                user.VerificationToken = Guid.NewGuid().ToString();
                user.EmailVerified = false;
                user.Email = newEmail;
            }
        }

        await db.SaveChangesAsync(ct);
    }

    public async Task<string> SoftDeleteUserAsync(Guid id, CancellationToken ct = default)
    {
        var user = await db.Users.FirstOrDefaultAsync(u => u.Id == id, ct)
            ?? throw new ArgumentException("Utilisateur non trouvé");

        user.Status = UserStatus.Deleted;
        await db.SaveChangesAsync(ct);

        // Distributed session/SSE invalidation belongs to the session-registry +
        // realtime modules (not ported); the cookie-session model has no per-user
        // bulk invalidation here, so the counts are reported as zero.
        return "Utilisateur désactivé (soft delete) — 0 session(s) + 0 stream(s) SSE fermé(s)";
    }

    /// <summary>Grant/revoke the ADMIN role. Faithful port of <c>UserServiceImpl.setUserAdminRole</c>.</summary>
    private async Task SetAdminRoleAsync(User target, bool grantAdmin, Guid actingAdminId, CancellationToken ct)
    {
        var adminRole = await db.Roles.FirstOrDefaultAsync(r => r.Name == "ADMIN", ct)
            ?? throw new ArgumentException("Rôle ADMIN introuvable");
        var userRole = await db.Roles.FirstOrDefaultAsync(r => r.Name == "USER", ct)
            ?? throw new ArgumentException("Rôle USER introuvable");

        var hasAdmin = target.Roles.Any(r => r.Name == "ADMIN");

        if (grantAdmin)
        {
            if (!hasAdmin)
            {
                target.Roles.Add(adminRole);
            }

            if (target.Roles.All(r => r.Name != "USER"))
            {
                target.Roles.Add(userRole);
            }

            return;
        }

        if (target.Id == actingAdminId)
        {
            throw new ArgumentException("Vous ne pouvez pas retirer votre propre rôle administrateur");
        }

        if (hasAdmin)
        {
            var adminCount = await db.Users.CountAsync(u => u.Roles.Any(r => r.Name == "ADMIN"), ct);
            if (adminCount <= 1)
            {
                throw new ArgumentException("Impossible de retirer le dernier administrateur");
            }

            var toRemove = target.Roles.First(r => r.Name == "ADMIN");
            target.Roles.Remove(toRemove);
        }

        if (target.Roles.All(r => r.Name != "USER"))
        {
            target.Roles.Add(userRole);
        }
    }
}
