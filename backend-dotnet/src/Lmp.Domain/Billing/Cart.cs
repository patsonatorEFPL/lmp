using Lmp.Domain.Catalog;

namespace Lmp.Domain.Billing;

/// <summary>A user's shopping cart — maps to <c>cart</c> (one per user).</summary>
public class Cart
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public List<CartItem> Items { get; set; } = [];
    public DateTime? CreatedAt { get; set; }
    public DateTime? UpdatedAt { get; set; }

    public Cart()
    {
    }

    public Cart(Guid userId) => UserId = userId;

    public void AddItem(CartItem item)
    {
        Items.Add(item);
        item.Cart = this;
    }

    /// <summary>Sum of <c>currentOffer.price × quantity</c> over items (EUR), mirroring <c>Cart.getTotalAmount()</c>.</summary>
    public double GetTotalAmount()
        => Items.Sum(item =>
        {
            var currentOffer = item.Service?.GetCurrentOffer();
            return currentOffer is not null ? (double)currentOffer.Price * item.Quantity : 0.0;
        });
}
