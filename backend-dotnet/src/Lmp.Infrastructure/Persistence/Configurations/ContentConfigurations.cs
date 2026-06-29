using Lmp.Domain.Content;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace Lmp.Infrastructure.Persistence.Configurations;

internal sealed class BlogPostConfiguration : IEntityTypeConfiguration<BlogPost>
{
    public void Configure(EntityTypeBuilder<BlogPost> b)
    {
        b.ToTable("blog_posts");
        b.HasKey(x => x.Id);
        b.Property(x => x.Id).HasDefaultValueSql("gen_random_uuid()");
        b.Property(x => x.Slug).IsRequired();
        // Only the sitemap-relevant columns are mapped; the rest of the table is
        // left to the full content module. Treat as read-only here.
        b.Metadata.SetIsTableExcludedFromMigrations(true);
    }
}
