using Lmp.Domain.Billing;

namespace Lmp.Application.Billing;

/// <summary>Cart use-cases. Port of <c>com.lmp.billing.service.CartService</c>.</summary>
public interface ICartService
{
    Task<Cart> GetOrCreateCartForUserAsync(Guid userId, CancellationToken ct = default);

    /// <summary>Add a service (qty 1, or increment if already present). Throws <see cref="ArgumentException"/> if the service is unknown.</summary>
    Task<Cart> AddToCartAsync(Guid userId, Guid serviceId, CancellationToken ct = default);

    Task<Cart> RemoveFromCartAsync(Guid userId, Guid cartItemId, CancellationToken ct = default);

    Task<Cart> ClearCartAsync(Guid userId, CancellationToken ct = default);
}
