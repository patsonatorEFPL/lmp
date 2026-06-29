using System.Text.Json;
using Lmp.Application.Admin;
using Lmp.Application.Catalog;
using Lmp.Application.Common;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lmp.Api.Controllers.Admin;

/// <summary>
/// Admin catalogue management. Port of
/// <c>com.lmp.catalog.web.api.AdminServiceRestController</c>
/// (<c>/api/v1/admin/services</c>). ADMIN-only.
/// </summary>
[ApiController]
[Route("api/v1/admin/services")]
[Authorize(Roles = "ADMIN")]
public sealed class AdminServiceController(IAdminCatalogService catalog) : ControllerBase
{
    // ── Reads ────────────────────────────────────────────────────────────────
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

    // ── Category writes ──────────────────────────────────────────────────────
    [HttpPost("categories")]
    public Task<IActionResult> CreateCategory([FromBody] Dictionary<string, JsonElement> data, CancellationToken ct)
        => RunData(() => catalog.CreateCategoryAsync(data, ct), "Catégorie créée");

    [HttpPut("categories/{id:guid}")]
    public Task<IActionResult> UpdateCategory(Guid id, [FromBody] Dictionary<string, JsonElement> data, CancellationToken ct)
        => Run(() => catalog.UpdateCategoryAsync(id, data, ct), "Catégorie mise à jour");

    [HttpDelete("categories/{id:guid}")]
    public Task<IActionResult> DeleteCategory(Guid id, CancellationToken ct)
        => Run(() => catalog.DeleteCategoryAsync(id, ct), "Catégorie supprimée");

    // ── Service writes ───────────────────────────────────────────────────────
    [HttpPost]
    public Task<IActionResult> CreateService([FromBody] Dictionary<string, JsonElement> data, CancellationToken ct)
        => RunData(() => catalog.CreateServiceAsync(data, ct), "Service créé");

    [HttpPut("{id:guid}")]
    public Task<IActionResult> UpdateService(Guid id, [FromBody] Dictionary<string, JsonElement> data, CancellationToken ct)
        => Run(() => catalog.UpdateServiceAsync(id, data, ct), "Service mis à jour");

    [HttpDelete("{id:guid}")]
    public Task<IActionResult> DeleteService(Guid id, CancellationToken ct)
        => Run(() => catalog.DeleteServiceAsync(id, ct), "Service supprimé");

    [HttpPut("reorder")]
    public Task<IActionResult> Reorder([FromBody] Dictionary<string, JsonElement> data, CancellationToken ct)
    {
        var ids = new List<Guid>();
        if (data.TryGetValue("serviceIds", out var arr) && arr.ValueKind == JsonValueKind.Array)
        {
            foreach (var e in arr.EnumerateArray())
            {
                if (Guid.TryParse(e.GetString(), out var g))
                {
                    ids.Add(g);
                }
            }
        }

        return Run(() => catalog.ReorderServicesAsync(ids, ct), "Ordre mis à jour");
    }

    // ── Offer writes ─────────────────────────────────────────────────────────
    [HttpPost("{serviceId:guid}/offers")]
    public Task<IActionResult> CreateOffer(Guid serviceId, [FromBody] Dictionary<string, JsonElement> data, CancellationToken ct)
        => RunData(() => catalog.CreateOfferAsync(serviceId, data, ct), "Offre créée");

    [HttpPut("offers/{id:guid}")]
    public Task<IActionResult> UpdateOffer(Guid id, [FromBody] Dictionary<string, JsonElement> data, CancellationToken ct)
        => Run(() => catalog.UpdateOfferAsync(id, data, ct), "Offre mise à jour");

    [HttpDelete("offers/{id:guid}")]
    public Task<IActionResult> DeleteOffer(Guid id, CancellationToken ct)
        => Run(() => catalog.DeleteOfferAsync(id, ct), "Offre supprimée");

    // ── Benefit writes ───────────────────────────────────────────────────────
    [HttpPost("{serviceId:guid}/benefits")]
    public Task<IActionResult> CreateBenefit(Guid serviceId, [FromBody] Dictionary<string, JsonElement> data, CancellationToken ct)
        => RunData(() => catalog.CreateBenefitAsync(serviceId, data, ct), "Avantage ajouté");

    [HttpDelete("benefits/{id:guid}")]
    public Task<IActionResult> DeleteBenefit(Guid id, CancellationToken ct)
        => Run(() => catalog.DeleteBenefitAsync(id, ct), "Avantage supprimé");

    [HttpPost("{serviceId:guid}/benefits/sync")]
    public Task<IActionResult> SyncBenefits(Guid serviceId, [FromBody] Dictionary<string, JsonElement> data, CancellationToken ct)
        => Run(() => catalog.SyncBenefitsAsync(serviceId, data, ct), "Avantages mis à jour");

    // ── Shared handlers (faithful try/catch -> 400 with the thrown message) ───
    private async Task<IActionResult> Run(Func<Task> action, string okMessage)
    {
        try
        {
            await action();
            return Ok(new ApiResponse<object>(true, okMessage, null));
        }
        catch (ArgumentException e)
        {
            return BadRequest(ApiResponse<object>.Error(e.Message));
        }
    }

    private async Task<IActionResult> RunData(Func<Task<IDictionary<string, object?>>> action, string okMessage)
    {
        try
        {
            var data = await action();
            return Ok(ApiResponse<IDictionary<string, object?>>.Ok(okMessage, data));
        }
        catch (ArgumentException e)
        {
            return BadRequest(ApiResponse<IDictionary<string, object?>>.Error(e.Message));
        }
    }
}
