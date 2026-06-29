using Lmp.Domain.Notification;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace Lmp.Infrastructure.Persistence.Configurations;

internal sealed class InAppNotificationConfiguration : IEntityTypeConfiguration<InAppNotification>
{
    public void Configure(EntityTypeBuilder<InAppNotification> b)
    {
        b.ToTable("in_app_notifications");
        b.HasKey(x => x.Id);
        b.Property(x => x.Id).HasDefaultValueSql("gen_random_uuid()");
        b.Property(x => x.Type).HasMaxLength(50).IsRequired();
        b.Property(x => x.Message).HasColumnType("text").IsRequired();
        b.Property(x => x.OrderId).HasMaxLength(100);
        b.Property(x => x.Amount).HasColumnType("numeric(10,2)");
        b.Property(x => x.Read).HasColumnName("is_read").IsRequired();
        b.Property(x => x.CreatedAt).IsRequired();
    }
}
