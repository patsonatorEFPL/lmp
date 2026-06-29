using System.Text.Json;

namespace Lmp.Application.Admin;

/// <summary>Admin user management. Port of the user write methods of <c>AdminRestController</c>.</summary>
public interface IAdminUserService
{
    /// <summary>
    /// Update a user's admin role / status / lock / email. <paramref name="actorId"/> is the
    /// acting admin (for self-protection rules). Throws <see cref="ArgumentException"/> with a
    /// user-facing message on any rule violation.
    /// </summary>
    Task UpdateUserAsync(Guid id, IDictionary<string, JsonElement> data, Guid actorId, CancellationToken ct = default);

    /// <summary>Soft-delete: set status DELETED. Returns the status message. Throws if not found.</summary>
    Task<string> SoftDeleteUserAsync(Guid id, CancellationToken ct = default);
}
