using System.Security.Claims;
using Lmp.Application.Billing;
using Lmp.Application.Common;
using Microsoft.AspNetCore.Mvc;

namespace Lmp.Api.Controllers.Billing;

/// <summary>
/// Authenticated user's orders. Port of <c>com.lmp.billing.web.api.OrderRestController</c>
/// (<c>/api/v1/orders</c>). The PDF invoice endpoint (<c>/{id}/invoice</c>) is
/// deferred until the OpenPDF invoice subsystem is ported.
/// </summary>
[ApiController]
[Route("api/v1/orders")]
public sealed class OrderController(IOrderQueryService orders) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<OrderResponse>>>> GetMyOrders(CancellationToken ct)
    {
        if (CurrentUserId() is not { } userId)
        {
            return Unauthorized(ApiResponse<IReadOnlyList<OrderResponse>>.Error("Not authenticated"));
        }

        var result = (await orders.GetOrdersForUserAsync(userId, ct))
            .Select(OrderResponse.From)
            .ToList();
        return Ok(ApiResponse<IReadOnlyList<OrderResponse>>.Ok(result));
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<ApiResponse<OrderResponse>>> GetOrder(Guid id, CancellationToken ct)
    {
        if (CurrentUserId() is not { } userId)
        {
            return Unauthorized(ApiResponse<OrderResponse>.Error("Not authenticated"));
        }

        var order = await orders.GetOrderForUserAsync(id, userId, ct);
        return order is null
            ? NotFound()
            : Ok(ApiResponse<OrderResponse>.Ok(OrderResponse.From(order)));
    }

    [HttpGet("{id:guid}/refunds")]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<object>>>> GetOrderRefunds(Guid id, CancellationToken ct)
    {
        if (CurrentUserId() is not { } userId)
        {
            return Unauthorized(ApiResponse<IReadOnlyList<object>>.Error("Not authenticated"));
        }

        var order = await orders.GetOrderForUserAsync(id, userId, ct);
        if (order is null)
        {
            return NotFound();
        }

        var refunds = (await orders.GetRefundsForOrderAsync(id, ct))
            .Select(r => (object)new
            {
                id = r.Id,
                amount = r.Amount,
                currency = r.Currency,
                status = r.Status,
                reason = r.Reason,
                createdAt = r.CreatedAt,
                processedAt = r.ProcessedAt,
            })
            .ToList();
        return Ok(ApiResponse<IReadOnlyList<object>>.Ok(refunds));
    }

    private Guid? CurrentUserId()
    {
        if (User.Identity?.IsAuthenticated != true)
        {
            return null;
        }

        return Guid.TryParse(User.FindFirstValue(ClaimTypes.NameIdentifier), out var id) ? id : null;
    }
}
