using Lmp.Domain.Catalog;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Microsoft.EntityFrameworkCore.Storage.ValueConversion;

namespace Lmp.Infrastructure.Persistence.Configurations;

internal sealed class ServiceCategoryConfiguration : IEntityTypeConfiguration<ServiceCategory>
{
    public void Configure(EntityTypeBuilder<ServiceCategory> b)
    {
        b.ToTable("service_categories");
        b.HasKey(x => x.Id);
        b.Property(x => x.Id).HasDefaultValueSql("gen_random_uuid()");
        b.Property(x => x.Name).HasMaxLength(100).IsRequired();
        b.Property(x => x.Slug).HasMaxLength(100).IsRequired();
        b.HasIndex(x => x.Slug).IsUnique();
        b.Property(x => x.Icon).HasMaxLength(50);
        b.Property(x => x.DisplayOrder).HasDefaultValue(0);

        b.HasMany(x => x.Services)
            .WithOne(x => x.Category)
            .HasForeignKey(x => x.CategoryId)
            .OnDelete(DeleteBehavior.Restrict);
    }
}

internal sealed class ServiceConfiguration : IEntityTypeConfiguration<Service>
{
    public void Configure(EntityTypeBuilder<Service> b)
    {
        b.ToTable("services");
        b.HasKey(x => x.Id);
        b.Property(x => x.Id).HasDefaultValueSql("gen_random_uuid()");
        b.Property(x => x.Title).HasMaxLength(100).IsRequired();
        b.Property(x => x.Slug).HasMaxLength(100).IsRequired();
        b.HasIndex(x => x.Slug).IsUnique();
        b.Property(x => x.Description).HasColumnType("text").IsRequired();
        b.Property(x => x.Icon).HasMaxLength(50);
        b.Property(x => x.DisplayOrder).HasDefaultValue(0);
        b.Property(x => x.Featured).HasDefaultValue(false);
        b.Property(x => x.Active).HasDefaultValue(true);
        b.Property(x => x.ExternalItemCode).HasMaxLength(140);

        b.HasMany(x => x.Benefits)
            .WithOne(x => x.Service)
            .HasForeignKey(x => x.ServiceId)
            .OnDelete(DeleteBehavior.Cascade);

        b.HasMany(x => x.Offers)
            .WithOne(x => x.Service)
            .HasForeignKey(x => x.ServiceId)
            .OnDelete(DeleteBehavior.Restrict);
    }
}

internal sealed class ServiceBenefitConfiguration : IEntityTypeConfiguration<ServiceBenefit>
{
    public void Configure(EntityTypeBuilder<ServiceBenefit> b)
    {
        b.ToTable("service_benefits");
        b.HasKey(x => x.Id);
        b.Property(x => x.Id).HasDefaultValueSql("gen_random_uuid()");
        b.Property(x => x.Benefit).HasColumnType("text").IsRequired();
    }
}

internal sealed class ServiceOfferConfiguration : IEntityTypeConfiguration<ServiceOffer>
{
    private static readonly ValueConverter<DurationType, string> DurationTypeConverter = new(
        v => v == DurationType.OneTime ? "ONE_TIME" : v == DurationType.Monthly ? "MONTHLY" : "YEARLY",
        v => v == "ONE_TIME" ? DurationType.OneTime : v == "MONTHLY" ? DurationType.Monthly : DurationType.Yearly);

    public void Configure(EntityTypeBuilder<ServiceOffer> b)
    {
        b.ToTable("service_offers");
        b.HasKey(x => x.Id);
        b.Property(x => x.Id).HasDefaultValueSql("gen_random_uuid()");
        b.Property(x => x.Name).HasMaxLength(100).IsRequired();
        b.Property(x => x.Price).HasColumnType("numeric(10,2)").IsRequired();
        b.Property(x => x.OriginalPrice).HasColumnType("numeric(10,2)");
        b.Property(x => x.DurationType)
            .HasColumnName("duration_type")
            .HasConversion(DurationTypeConverter)
            .HasMaxLength(20)
            .IsRequired();
        b.Property(x => x.Duration).HasMaxLength(50);
        b.Property(x => x.IsDefault).HasDefaultValue(false).IsRequired();
        b.Property(x => x.Active).HasDefaultValue(true).IsRequired();

        b.HasMany(x => x.Benefits)
            .WithOne(x => x.Offer)
            .HasForeignKey(x => x.OfferId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}

internal sealed class OfferBenefitConfiguration : IEntityTypeConfiguration<OfferBenefit>
{
    public void Configure(EntityTypeBuilder<OfferBenefit> b)
    {
        b.ToTable("offer_benefits");
        b.HasKey(x => x.Id);
        b.Property(x => x.Id).HasDefaultValueSql("gen_random_uuid()");
        b.Property(x => x.Benefit).HasColumnType("text").IsRequired();
        b.Property(x => x.DisplayOrder).HasDefaultValue(0);
    }
}
