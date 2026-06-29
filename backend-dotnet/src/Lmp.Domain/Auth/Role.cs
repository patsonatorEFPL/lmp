namespace Lmp.Domain.Auth;

/// <summary>Authorization role (e.g. <c>ADMIN</c>, <c>USER</c>) — maps to <c>roles</c>.</summary>
public class Role
{
    public Guid Id { get; set; }
    public string Name { get; set; } = null!;

    public ICollection<User> Users { get; set; } = new List<User>();

    public Role()
    {
    }

    public Role(string name) => Name = name;
}
