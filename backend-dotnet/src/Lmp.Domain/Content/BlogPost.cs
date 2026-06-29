namespace Lmp.Domain.Content;

/// <summary>
/// Blog post — maps to <c>blog_posts</c>. This port currently models the
/// sitemap-relevant subset; the full content module (CRUD, rendering) is a later
/// increment, so this entity is read-only in practice.
/// </summary>
public class BlogPost
{
    public Guid Id { get; set; }
    public string Slug { get; set; } = null!;
    public bool Published { get; set; }
    public DateTime? PublishedAt { get; set; }
    public DateTime? UpdatedAt { get; set; }
}
