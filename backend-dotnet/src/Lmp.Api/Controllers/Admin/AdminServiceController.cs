using Lmp.Application.Admin;
using Lmp.Application.Catalog;
using Lmp.Application.Common;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lmp.Api.Controllers.Admin;

/// <summary>
/// Admin catalogue management. Port of
/// <c>com.lmp.catalog.web.api.AdminServiceRestController</c>
/// (<c>/api/v1/admin/services</c>). ADMIN-only. This slice covers the read
/// endpoints; category/service/offer/benefit writes are a later increment.
/// </summary>
[ApiController]
[Route("api/v1/admin/services")]
[Authorize(Roles = "ADMIN")]
public sealed class AdminServiceController(IAdminCatalogService catalog) : ControllerBase
{
    [HttpGet("stats")]
    public async Task<ActionResult<ApiResponse<IDictionary<string, object>>>> GetStats(CancellationToken ct)
        => Ok(ApiResponse<IDictionary<string, object>>.Ok(await catalog.GetStatsAsync(ct)));

    [HttpGet("categories")]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<IDictionary<string, object?>>>>> GetCategories(CancellationToken ct)
        => Ok(ApiResponse<IReadOnlyList<IDictionary<string, object?>>>.Ok(await catalog.GetCategoriesAsync(ct)));

    [HttpGet]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<ServiceResponse>>>> GetAllServices(CancellationToken ct)
        => Ok(ApiResponse<IReadOnlyList<ServiceResponse>>.Ok(await catalog.GetAllServicesAsync(ct)));

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<ApiResponse<IDictionary<string, object?>>>> GetService(Guid id, CancellationToken ct)
    {
        var detail = await catalog.GetServiceDetailAsync(id, ct);
        return detail is null
            ? BadRequest(ApiResponse<IDictionary<string, object?>>.Error("Service non trouvé"))
            : Ok(ApiResponse<IDictionary<string, object?>>.Ok(detail));
    }
}
