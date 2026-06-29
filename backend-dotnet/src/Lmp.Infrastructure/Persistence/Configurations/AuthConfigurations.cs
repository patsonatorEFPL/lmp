using Lmp.Domain.Auth;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Microsoft.EntityFrameworkCore.Storage.ValueConversion;

namespace Lmp.Infrastructure.Persistence.Configurations;

internal sealed class RoleConfiguration : IEntityTypeConfiguration<Role>
{
    public void Configure(EntityTypeBuilder<Role> b)
    {
        b.ToTable("roles");
        b.HasKey(x => x.Id);
        b.Property(x => x.Id).HasDefaultValueSql("gen_random_uuid()");
        b.Property(x => x.Name).IsRequired();
        b.HasIndex(x => x.Name).IsUnique();
    }
}

internal sealed class UserConfiguration : IEntityTypeConfiguration<User>
{
    private static readonly ValueConverter<UserStatus, string> StatusConverter = new(
        v => v == UserStatus.Active ? "ACTIVE" : v == UserStatus.Inactive ? "INACTIVE" : "DELETED",
        v => v == "ACTIVE" ? UserStatus.Active : v == "INACTIVE" ? UserStatus.Inactive : UserStatus.Deleted);

    public void Configure(EntityTypeBuilder<User> b)
    {
        b.ToTable("users");
        b.HasKey(x => x.Id);
        b.Property(x => x.Id).HasDefaultValueSql("gen_random_uuid()");
        b.Property(x => x.Password).IsRequired();
        b.Property(x => x.Status).HasConversion(StatusConverter);
        b.Property(x => x.AccountLocked).HasColumnName("account_locked");
        b.Property(x => x.EmailVerified).HasColumnName("email_verified");
        b.Property(x => x.VatReverseCharge).HasColumnName("vat_reverse_charge").IsRequired();
        b.Property(x => x.VatNumber).HasMaxLength(64);
        b.HasIndex(x => x.Email).IsUnique();
        b.HasIndex(x => x.Username).IsUnique();

        b.HasMany(x => x.Roles)
            .WithMany(x => x.Users)
            .UsingEntity(
                "user_roles",
                l => l.HasOne(typeof(Role)).WithMany().HasForeignKey("role_id").HasPrincipalKey(nameof(Role.Id)),
                r => r.HasOne(typeof(User)).WithMany().HasForeignKey("user_id").HasPrincipalKey(nameof(User.Id)),
                j => j.HasKey("user_id", "role_id"));
    }
}
