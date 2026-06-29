using Lmp.Domain.Auth;

namespace Lmp.Application.Auth;

/// <summary>
/// User lookups for authentication. Subset of <c>com.lmp.auth.service.UserService</c>
/// needed by the session flow; grows as auth features are ported.
/// </summary>
public interface IUserService
{
    /// <summary>Find by login: username OR email (mirrors <c>UserRepository.findByLogin</c>). Roles included.</summary>
    Task<User?> FindByLoginAsync(string login, CancellationToken ct = default);

    /// <summary>Find by id, roles included.</summary>
    Task<User?> FindByIdAsync(Guid id, CancellationToken ct = default);

    /// <summary>Stamp the last-login timestamp.</summary>
    Task UpdateLastLoginDateAsync(Guid userId, CancellationToken ct = default);
}
