using Lmp.Application.Billing;
using Lmp.Domain.Billing;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Lmp.Infrastructure.Billing;

/// <summary>EF Core cart use-cases. Faithful port of <c>com.lmp.billing.service.CartService</c>.</summary>
public sealed class CartService(LmpDbContext db) : ICartService
{
    public async Task<Cart> GetOrCreateCartForUserAsync(Guid userId, CancellationToken ct = default)
    {
        var cart = await LoadCart(userId, ct);
        if (cart is not null)
        {
            return cart;
        }

        var now = DateTime.UtcNow;
        cart = new Cart(userId) { CreatedAt = now, UpdatedAt = now };
        db.Carts.Add(cart);
        await db.SaveChangesAsync(ct);
        return cart;
    }

    public async Task<Cart> AddToCartAsync(Guid userId, Guid serviceId, CancellationToken ct = default)
    {
        var cart = await GetOrCreateCartForUserAsync(userId, ct);

        var service = await db.Services
            .Include(s => s.Offers)
            .FirstOrDefaultAsync(s => s.Id == serviceId, ct)
            ?? throw new ArgumentException("Service non trouvé.");

        var existing = cart.Items.FirstOrDefault(i => i.ServiceId == serviceId);
        if (existing is not null)
        {
            existing.Quantity += 1;
        }
        else
        {
            cart.AddItem(new CartItem(cart, service, 1) { CreatedAt = DateTime.UtcNow });
        }

        cart.UpdatedAt = DateTime.UtcNow;
        await db.SaveChangesAsync(ct);
        return await LoadCart(userId, ct) ?? cart;
    }

    public async Task<Cart> RemoveFromCartAsync(Guid userId, Guid cartItemId, CancellationToken ct = default)
    {
        var cart = await GetOrCreateCartForUserAsync(userId, ct);
        var item = cart.Items.FirstOrDefault(i => i.Id == cartItemId);
        if (item is not null)
        {
            cart.Items.Remove(item);
            cart.UpdatedAt = DateTime.UtcNow;
            await db.SaveChangesAsync(ct);
        }

        return await LoadCart(userId, ct) ?? cart;
    }

    public async Task<Cart> ClearCartAsync(Guid userId, CancellationToken ct = default)
    {
        var cart = await GetOrCreateCartForUserAsync(userId, ct);
        if (cart.Items.Count > 0)
        {
            cart.Items.Clear();
            cart.UpdatedAt = DateTime.UtcNow;
            await db.SaveChangesAsync(ct);
        }

        return await LoadCart(userId, ct) ?? cart;
    }

    /// <summary>Load a cart with items, each item's service and its offers (for current-offer pricing).</summary>
    private Task<Cart?> LoadCart(Guid userId, CancellationToken ct)
        => db.Carts
            .AsSplitQuery()
            .Include(c => c.Items)
            .ThenInclude(i => i.Service)
            .ThenInclude(s => s.Offers)
            .FirstOrDefaultAsync(c => c.UserId == userId, ct);
}
