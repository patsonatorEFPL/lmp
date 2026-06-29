using Lmp.Domain.Catalog;

namespace Lmp.Domain.Billing;

/// <summary>A line in a <see cref="Cart"/> — maps to <c>cart_item</c>.</summary>
public class CartItem
{
    public Guid Id { get; set; }
    public Guid CartId { get; set; }
    public Cart Cart { get; set; } = null!;
    public Guid ServiceId { get; set; }
    public Service Service { get; set; } = null!;
    public int Quantity { get; set; } = 1;
    public DateTime? CreatedAt { get; set; }

    public CartItem()
    {
    }

    public CartItem(Cart cart, Service service, int quantity)
    {
        Cart = cart;
        Service = service;
        ServiceId = service.Id;
        Quantity = quantity;
    }
}
