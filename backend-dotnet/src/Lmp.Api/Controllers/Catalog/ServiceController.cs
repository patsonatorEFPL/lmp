using Lmp.Application.Catalog;
using Lmp.Application.Common;
using Lmp.Application.Pricing;
using Microsoft.AspNetCore.Mvc;

namespace Lmp.Api.Controllers.Catalog;

/// <summary>
/// Public service-catalogue API. Port of <c>com.lmp.catalog.web.api.ServiceRestController</c>
/// (<c>/api/v1/services</c>). Prices are converted to the caller's resolved
/// currency. Endpoints are public — no authentication.
/// </summary>
[ApiController]
[Route("api/v1/services")]
public sealed class ServiceController(
    IServiceCatalogService catalog,
    IRegionalPricingService pricing) : ControllerBase
{
    private const int DefaultSearchLimit = 20;
    private const string PublicCache = "public, max-age=300";

    [HttpGet]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<ServiceResponse>>>> GetAll(CancellationToken ct)
    {
        var context = pricing.ContextForCountry(ResolveCountry());
        var services = await catalog.GetActiveServicesAsync(ct);
        Response.Headers.CacheControl = PublicCache;
        return Ok(ApiResponse<IReadOnlyList<ServiceResponse>>.Ok(Project(services, context)));
    }

    [HttpGet("featured")]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<ServiceResponse>>>> GetFeatured(CancellationToken ct)
    {
        var context = pricing.ContextForCountry(ResolveCountry());
        var services = await catalog.GetFeaturedServicesAsync(ct);
        Response.Headers.CacheControl = PublicCache;
        return Ok(ApiResponse<IReadOnlyList<ServiceResponse>>.Ok(Project(services, context)));
    }

    [HttpGet("search")]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<ServiceResponse>>>> Search(
        [FromQuery(Name = "q")] string? query,
        [FromQuery] int limit = DefaultSearchLimit,
        CancellationToken ct = default)
    {
        Response.Headers.CacheControl = PublicCache;
        if (string.IsNullOrWhiteSpace(query))
        {
            return Ok(ApiResponse<IReadOnlyList<ServiceResponse>>.Ok([]));
        }

        var safeLimit = Math.Clamp(limit, 1, 50);
        var context = pricing.ContextForCountry(ResolveCountry());
        var services = await catalog.SearchActiveAsync(query.Trim(), safeLimit, ct);
        return Ok(ApiResponse<IReadOnlyList<ServiceResponse>>.Ok(Project(services, context)));
    }

    [HttpGet("{slug}")]
    public async Task<ActionResult<ApiResponse<ServiceResponse>>> GetBySlug(string slug, CancellationToken ct)
    {
        var service = await catalog.GetServiceBySlugAsync(slug, ct);
        if (service is null)
        {
            return NotFound();
        }

        var context = pricing.ContextForCountry(ResolveCountry());
        Response.Headers.CacheControl = PublicCache;
        return Ok(ApiResponse<ServiceResponse>.Ok(ServiceResponse.From(service, context, pricing)));
    }

    private List<ServiceResponse> Project(IEnumerable<Domain.Catalog.Service> services, PricingContext context)
        => services.Select(s => ServiceResponse.From(s, context, pricing)).ToList();

    /// <summary>
    /// Country signal from Cloudflare's <c>CF-IPCountry</c> header. The Java backend
    /// additionally runs a GeoLite2/HTTP lookup for the unfiltered list endpoint;
    /// that geo subsystem is a follow-up module, so for now all catalogue endpoints
    /// share this O(1) header signal (as 3 of the 4 already do).
    /// </summary>
    private string ResolveCountry()
    {
        var cf = Request.Headers["CF-IPCountry"].ToString();
        return string.IsNullOrWhiteSpace(cf) ? "XX" : cf.Trim().ToUpperInvariant();
    }
}
