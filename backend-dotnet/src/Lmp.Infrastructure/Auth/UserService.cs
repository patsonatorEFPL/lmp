using Lmp.Application.Auth;
using Lmp.Domain.Auth;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Lmp.Infrastructure.Auth;

/// <summary>EF Core user lookups for the authentication flow.</summary>
public sealed class UserService(LmpDbContext db) : IUserService
{
    public async Task<User?> FindByLoginAsync(string login, CancellationToken ct = default)
        => await db.Users
            .AsNoTracking()
            .Include(u => u.Roles)
            .FirstOrDefaultAsync(u => u.Username == login || u.Email == login, ct);

    public async Task<User?> FindByIdAsync(Guid id, CancellationToken ct = default)
        => await db.Users
            .AsNoTracking()
            .Include(u => u.Roles)
            .FirstOrDefaultAsync(u => u.Id == id, ct);

    public async Task UpdateLastLoginDateAsync(Guid userId, CancellationToken ct = default)
        => await db.Users
            .Where(u => u.Id == userId)
            .ExecuteUpdateAsync(s => s.SetProperty(u => u.LastLoginDate, DateTime.UtcNow), ct);
}
