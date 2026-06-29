using Lmp.Application.Catalog;

namespace Lmp.Application.Admin;

/// <summary>Admin catalogue reads. Port of the read methods of <c>AdminServiceRestController</c>.</summary>
public interface IAdminCatalogService
{
    /// <summary>Catalogue KPIs (categories/services/offers/active/featured counts).</summary>
    Task<IDictionary<string, object>> GetStatsAsync(CancellationToken ct = default);

    /// <summary>Categories ordered by display order, as flat maps.</summary>
    Task<IReadOnlyList<IDictionary<string, object?>>> GetCategoriesAsync(CancellationToken ct = default);

    /// <summary>All services (including inactive), ordered by display order, EUR prices.</summary>
    Task<IReadOnlyList<ServiceResponse>> GetAllServicesAsync(CancellationToken ct = default);

    /// <summary>Service detail (service + categoryId + offers[+benefits] + benefits), or null if missing.</summary>
    Task<IDictionary<string, object?>?> GetServiceDetailAsync(Guid id, CancellationToken ct = default);
}
