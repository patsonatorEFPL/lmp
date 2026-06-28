namespace Lmp.Domain.Catalog;

/// <summary>Service category — maps to <c>service_categories</c>.</summary>
public class ServiceCategory
{
    public Guid Id { get; set; }
    public string Name { get; set; } = null!;
    public string Slug { get; set; } = null!;
    public string? Description { get; set; }
    public string? Icon { get; set; }
    public int DisplayOrder { get; set; }

    public ICollection<Service> Services { get; set; } = new List<Service>();
}
