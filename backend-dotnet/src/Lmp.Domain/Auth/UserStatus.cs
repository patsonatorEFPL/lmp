namespace Lmp.Domain.Auth;

/// <summary>Account lifecycle state — persisted as the enum name (<c>users.status</c>).</summary>
public enum UserStatus
{
    Active,
    Inactive,
    Deleted,
}
