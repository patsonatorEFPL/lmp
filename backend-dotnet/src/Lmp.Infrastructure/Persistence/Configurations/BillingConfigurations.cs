using Lmp.Domain.Billing;
using Lmp.Domain.Catalog;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Microsoft.EntityFrameworkCore.Storage.ValueConversion;

namespace Lmp.Infrastructure.Persistence.Configurations;

internal sealed class OrderConfiguration : IEntityTypeConfiguration<Order>
{
    private static readonly ValueConverter<OrderStatus, string> StatusConverter = new(
        v => v.ToWire(),
        v => OrderStatusExtensions.FromWire(v));

    public void Configure(EntityTypeBuilder<Order> b)
    {
        b.ToTable("orders");
        b.HasKey(x => x.Id);
        b.Property(x => x.Id).HasDefaultValueSql("gen_random_uuid()");
        b.Property(x => x.ServiceName).IsRequired();
        b.Property(x => x.TotalAmount).HasColumnType("numeric(10,2)").IsRequired();
        b.Property(x => x.Currency).HasMaxLength(3);
        b.Property(x => x.Status).HasConversion(StatusConverter).HasMaxLength(30);
        b.Property(x => x.AppliedVatRate).HasColumnType("numeric(5,4)");
        b.Property(x => x.AmountBaseEur).HasColumnType("numeric(10,2)");
        b.Property(x => x.FxRate).HasColumnType("numeric(18,6)");
        b.Property(x => x.VpnScore).HasColumnType("numeric(5,3)");
        b.Property(x => x.VatReverseCharge).HasColumnName("vat_reverse_charge").IsRequired();
        b.Property(x => x.CustomerVatNumber).HasMaxLength(64);
        b.Property(x => x.CheckoutToken).HasMaxLength(64);
        b.Property(x => x.IpCountry).HasMaxLength(2);
        b.Property(x => x.IpAddress).HasMaxLength(45);
        b.Property(x => x.GeoCountry).HasMaxLength(2);
        b.Property(x => x.CardCountry).HasMaxLength(2);

        b.HasOne(x => x.User)
            .WithMany()
            .HasForeignKey(x => x.UserId)
            .OnDelete(DeleteBehavior.Restrict);

        b.HasMany(x => x.Refunds)
            .WithOne(x => x.Order)
            .HasForeignKey(x => x.OrderId)
            .OnDelete(DeleteBehavior.Restrict);
    }
}

internal sealed class RefundConfiguration : IEntityTypeConfiguration<Refund>
{
    public void Configure(EntityTypeBuilder<Refund> b)
    {
        b.ToTable("refunds");
        b.HasKey(x => x.Id);
        b.Property(x => x.Id).HasDefaultValueSql("gen_random_uuid()");
        b.Property(x => x.Amount).HasColumnType("numeric(10,2)").IsRequired();
        b.Property(x => x.Currency).HasMaxLength(3);
        b.Property(x => x.Status).HasMaxLength(50);
    }
}

internal sealed class CartConfiguration : IEntityTypeConfiguration<Cart>
{
    public void Configure(EntityTypeBuilder<Cart> b)
    {
        b.ToTable("cart");
        b.HasKey(x => x.Id);
        b.Property(x => x.Id).HasDefaultValueSql("gen_random_uuid()");
        b.Property(x => x.UserId).HasColumnName("user_id").IsRequired();
        b.HasIndex(x => x.UserId).IsUnique();

        b.HasMany(x => x.Items)
            .WithOne(x => x.Cart)
            .HasForeignKey(x => x.CartId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}

internal sealed class CartItemConfiguration : IEntityTypeConfiguration<CartItem>
{
    public void Configure(EntityTypeBuilder<CartItem> b)
    {
        b.ToTable("cart_item");
        b.HasKey(x => x.Id);
        b.Property(x => x.Id).HasDefaultValueSql("gen_random_uuid()");
        b.Property(x => x.Quantity).HasDefaultValue(1).IsRequired();

        b.HasOne(x => x.Service)
            .WithMany()
            .HasForeignKey(x => x.ServiceId)
            .OnDelete(DeleteBehavior.Restrict);
    }
}
