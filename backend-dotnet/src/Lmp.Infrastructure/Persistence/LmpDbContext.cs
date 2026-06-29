using Lmp.Domain.Auth;
using Lmp.Domain.Billing;
using Lmp.Domain.Catalog;
using Lmp.Domain.Crm;
using Lmp.Domain.Notification;
using Lmp.Domain.SiteConfiguration;
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

    // ── Auth ─────────────────────────────────────────────────────────────────
    public DbSet<User> Users => Set<User>();
    public DbSet<Role> Roles => Set<Role>();

    // ── Billing ──────────────────────────────────────────────────────────────
    public DbSet<Cart> Carts => Set<Cart>();
    public DbSet<CartItem> CartItems => Set<CartItem>();
    public DbSet<Order> Orders => Set<Order>();
    public DbSet<Refund> Refunds => Set<Refund>();

    // ── CRM ──────────────────────────────────────────────────────────────────
    public DbSet<Appointment> Appointments => Set<Appointment>();

    // ── Notification ─────────────────────────────────────────────────────────
    public DbSet<InAppNotification> InAppNotifications => Set<InAppNotification>();

    // ── Shared / site configuration ──────────────────────────────────────────
    public DbSet<SiteConfigEntry> SiteConfig => Set<SiteConfigEntry>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);
        modelBuilder.ApplyConfigurationsFromAssembly(typeof(LmpDbContext).Assembly);
    }
}
