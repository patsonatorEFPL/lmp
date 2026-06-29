using Lmp.Domain.SiteConfiguration;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace Lmp.Infrastructure.Persistence.Configurations;

internal sealed class SiteConfigEntryConfiguration : IEntityTypeConfiguration<SiteConfigEntry>
{
    public void Configure(EntityTypeBuilder<SiteConfigEntry> b)
    {
        b.ToTable("site_config");
        b.HasKey(x => x.Key);
        b.Property(x => x.Key).HasColumnName("config_key").HasMaxLength(128);
        b.Property(x => x.Value).HasColumnName("config_value").HasMaxLength(4000);
        b.Property(x => x.Description).HasColumnName("config_description").HasMaxLength(500);
        b.Property(x => x.UpdatedAt).HasColumnName("updated_at").IsRequired();
    }
}
