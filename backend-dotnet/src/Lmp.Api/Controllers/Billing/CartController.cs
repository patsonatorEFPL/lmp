using System.Globalization;
using System.Security.Claims;
using Lmp.Application.Billing;
using Lmp.Domain.Billing;
using Microsoft.AspNetCore.Mvc;

namespace Lmp.Api.Controllers.Billing;

/// <summary>
/// Cart API. Port of <c>com.lmp.billing.web.api.CartApiController</c>
/// (<c>/api/cart</c>). User-scoped; responses keep the Java ad-hoc shape
/// (<c>cartId</c>, <c>totalAmount</c>, <c>items</c>, <c>itemCount</c>) and the
/// custom <c>{ error }</c> 401 body rather than the standard envelope.
/// </summary>
[ApiController]
[Route("api/cart")]
public sealed class CartController(ICartService cartService) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> GetCart(CancellationToken ct)
    {
        if (CurrentUserId() is not { } userId)
        {
            return Unauthorized(new { error = "Non authentifié" });
        }

        var cart = await cartService.GetOrCreateCartForUserAsync(userId, ct);
        return Ok(FormatCartResponse(cart));
    }

    [HttpPost("add/{serviceId:guid}")]
    public async Task<IActionResult> AddToCart(Guid serviceId, CancellationToken ct)
    {
        if (CurrentUserId() is not { } userId)
        {
            return Unauthorized(new { error = "Veuillez vous connecter pour ajouter au panier" });
        }

        try
        {
            var cart = await cartService.AddToCartAsync(userId, serviceId, ct);
            return Ok(FormatCartResponse(cart));
        }
        catch (ArgumentException e)
        {
            return BadRequest(new { error = e.Message });
        }
    }

    [HttpDelete("remove/{cartItemId:guid}")]
    public async Task<IActionResult> RemoveFromCart(Guid cartItemId, CancellationToken ct)
    {
        if (CurrentUserId() is not { } userId)
        {
            return Unauthorized(new { error = "Non authentifié" });
        }

        var cart = await cartService.RemoveFromCartAsync(userId, cartItemId, ct);
        return Ok(FormatCartResponse(cart));
    }

    [HttpDelete("clear")]
    public async Task<IActionResult> ClearCart(CancellationToken ct)
    {
        if (CurrentUserId() is not { } userId)
        {
            return Unauthorized(new { error = "Non authentifié" });
        }

        var cart = await cartService.ClearCartAsync(userId, ct);
        return Ok(FormatCartResponse(cart));
    }

    private Guid? CurrentUserId()
    {
        if (User.Identity?.IsAuthenticated != true)
        {
            return null;
        }

        return Guid.TryParse(User.FindFirstValue(ClaimTypes.NameIdentifier), out var id) ? id : null;
    }

    private static object FormatCartResponse(Cart cart)
    {
        var items = cart.Items.Select(item =>
        {
            var offer = item.Service.GetCurrentOffer();
            return (object)new
            {
                id = item.Id,
                serviceId = item.Service.Id,
                serviceName = item.Service.Title,
                priceValue = offer is not null ? (double)offer.Price : 0.0,
                price = offer is not null
                    ? offer.Price.ToString(CultureInfo.InvariantCulture) + " €"
                    : "Non disponible",
                quantity = item.Quantity,
            };
        }).ToList();

        return new
        {
            cartId = cart.Id,
            totalAmount = cart.GetTotalAmount(),
            items,
            itemCount = cart.Items.Count,
        };
    }
}
