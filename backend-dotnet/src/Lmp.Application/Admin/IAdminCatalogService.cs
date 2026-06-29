using System.Text.Json;
using Lmp.Application.Catalog;

namespace Lmp.Application.Admin;

/// <summary>Admin catalogue management. Port of <c>AdminServiceRestController</c>.</summary>
public interface IAdminCatalogService
{
    // ── Reads ────────────────────────────────────────────────────────────────
    Task<IDictionary<string, object>> GetStatsAsync(CancellationToken ct = default);

    Task<IReadOnlyList<IDictionary<string, object?>>> GetCategoriesAsync(CancellationToken ct = default);

    Task<IReadOnlyList<ServiceResponse>> GetAllServicesAsync(CancellationToken ct = default);

    Task<IDictionary<string, object?>?> GetServiceDetailAsync(Guid id, CancellationToken ct = default);

    // ── Category writes ──────────────────────────────────────────────────────
    Task<IDictionary<string, object?>> CreateCategoryAsync(IDictionary<string, JsonElement> data, CancellationToken ct = default);

    Task UpdateCategoryAsync(Guid id, IDictionary<string, JsonElement> data, CancellationToken ct = default);

    Task DeleteCategoryAsync(Guid id, CancellationToken ct = default);

    // ── Service writes ───────────────────────────────────────────────────────
    Task<IDictionary<string, object?>> CreateServiceAsync(IDictionary<string, JsonElement> data, CancellationToken ct = default);

    Task UpdateServiceAsync(Guid id, IDictionary<string, JsonElement> data, CancellationToken ct = default);

    Task DeleteServiceAsync(Guid id, CancellationToken ct = default);

    /// <summary>Assign sequential display orders (1..n) to the given service ids. Throws <see cref="ArgumentException"/> if empty or an id is missing.</summary>
    Task ReorderServicesAsync(IReadOnlyList<Guid> serviceIds, CancellationToken ct = default);

    // ── Offer writes ─────────────────────────────────────────────────────────
    Task<IDictionary<string, object?>> CreateOfferAsync(Guid serviceId, IDictionary<string, JsonElement> data, CancellationToken ct = default);

    Task UpdateOfferAsync(Guid id, IDictionary<string, JsonElement> data, CancellationToken ct = default);

    Task DeleteOfferAsync(Guid id, CancellationToken ct = default);

    // ── Benefit writes ───────────────────────────────────────────────────────
    Task<IDictionary<string, object?>> CreateBenefitAsync(Guid serviceId, IDictionary<string, JsonElement> data, CancellationToken ct = default);

    Task DeleteBenefitAsync(Guid id, CancellationToken ct = default);

    /// <summary>Replace a service's benefits with the provided list.</summary>
    Task SyncBenefitsAsync(Guid serviceId, IDictionary<string, JsonElement> data, CancellationToken ct = default);
}
