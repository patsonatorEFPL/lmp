using Lmp.Domain.Catalog;
using Microsoft.EntityFrameworkCore;

namespace Lmp.Infrastructure.Persistence;

/// <summary>
/// EF Core context mapped onto the existing PostgreSQL schema that Flyway owns.
/// This context is mapping-only — it never runs migrations; the Java backend's
/// Flyway scripts remain the single source of truth so both backends share one
/// database. snake_case column/table mapping mirrors the Flyway DDL.
/// </summary>
public class LmpDbContext(DbContextOptions<LmpDbContext> options) : DbContext(options)
{
    // ── Catalog ──────────────────────────────────────────────────────────────
    public DbSet<ServiceCategory> ServiceCategories => Set<ServiceCategory>();
    public DbSet<Service> Services => Set<Service>();
    public DbSet<ServiceBenefit> ServiceBenefits => Set<ServiceBenefit>();
    public DbSet<ServiceOffer> ServiceOffers => Set<ServiceOffer>();
    public DbSet<OfferBenefit> OfferBenefits => Set<OfferBenefit>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);
        modelBuilder.ApplyConfigurationsFromAssembly(typeof(LmpDbContext).Assembly);
    }
}
