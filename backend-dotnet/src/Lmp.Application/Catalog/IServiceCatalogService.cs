using Lmp.Domain.Catalog;

namespace Lmp.Application.Catalog;

/// <summary>
/// Catalogue business logic. Contract for <c>com.lmp.catalog.service.ServiceCatalogService</c>.
/// Implementations resolve the "current" offer (valid promo &gt; default) and
/// cache read-heavy lookups.
/// </summary>
public interface IServiceCatalogService
{
    Task<IReadOnlyList<Service>> GetActiveServicesAsync(CancellationToken ct = default);

    Task<IReadOnlyList<Service>> GetFeaturedServicesAsync(CancellationToken ct = default);

    Task<IReadOnlyList<Service>> SearchActiveAsync(string query, int max, CancellationToken ct = default);

    Task<Service?> GetServiceBySlugAsync(string slug, CancellationToken ct = default);

    Task<Service?> GetServiceByIdAsync(Guid id, CancellationToken ct = default);
}
