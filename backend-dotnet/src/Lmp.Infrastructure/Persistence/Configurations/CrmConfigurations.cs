using Lmp.Domain.Crm;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Microsoft.EntityFrameworkCore.Storage.ValueConversion;

namespace Lmp.Infrastructure.Persistence.Configurations;

internal sealed class AppointmentConfiguration : IEntityTypeConfiguration<Appointment>
{
    private static readonly ValueConverter<AppointmentStatus, string> StatusConverter = new(
        v => v.ToWire(),
        v => AppointmentStatusExtensions.FromWire(v));

    public void Configure(EntityTypeBuilder<Appointment> b)
    {
        b.ToTable("appointments");
        b.HasKey(x => x.Id);
        b.Property(x => x.Id).HasDefaultValueSql("gen_random_uuid()");
        b.Property(x => x.ClientName).HasMaxLength(100);
        b.Property(x => x.ClientEmail).HasMaxLength(100);
        b.Property(x => x.ClientPhone).HasMaxLength(20);
        b.Property(x => x.AppointmentDate).IsRequired();
        b.Property(x => x.Subject).HasMaxLength(200).IsRequired();
        b.Property(x => x.Status).HasConversion(StatusConverter).HasMaxLength(20).IsRequired();
        b.Property(x => x.CreatedAt).IsRequired();

        b.HasOne(x => x.User)
            .WithMany()
            .HasForeignKey(x => x.UserId)
            .OnDelete(DeleteBehavior.Restrict);
    }
}
