using Lmp.Application.Catalog;
using Lmp.Domain.Catalog;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Lmp.Infrastructure.Catalog;

/// <summary>
/// EF Core implementation of the catalogue read model. Faithful port of the
/// fetch semantics in <c>com.lmp.catalog.repository.ServiceRepository</c> /
/// <c>ServiceCatalogService</c> (category + benefits + offers eagerly loaded,
/// ordered by display order; full-text search via the V42 GIN index).
/// </summary>
public sealed class ServiceCatalogService(LmpDbContext db) : IServiceCatalogService
{
    public async Task<IReadOnlyList<Service>> GetActiveServicesAsync(CancellationToken ct = default)
        => await WithDetails(db.Services.Where(s => s.Active))
            .OrderBy(s => s.DisplayOrder)
            .ToListAsync(ct);

    public async Task<IReadOnlyList<Service>> GetFeaturedServicesAsync(CancellationToken ct = default)
        => await WithDetails(db.Services.Where(s => s.Featured && s.Active))
            .OrderBy(s => s.DisplayOrder)
            .ToListAsync(ct);

    public async Task<Service?> GetServiceBySlugAsync(string slug, CancellationToken ct = default)
        => await WithDetails(db.Services.Where(s => s.Slug == slug))
            .FirstOrDefaultAsync(ct);

    public async Task<Service?> GetServiceByIdAsync(Guid id, CancellationToken ct = default)
        => await WithDetails(db.Services.Where(s => s.Id == id))
            .FirstOrDefaultAsync(ct);

    public async Task<IReadOnlyList<Service>> SearchActiveAsync(string query, int max, CancellationToken ct = default)
    {
        // Native full-text query over the generated search_vector (V42). We fetch
        // ranked ids first, then materialise full aggregates and re-apply the rank
        // order, since Include composition over raw SQL does not preserve ORDER BY.
        var rankedIds = await db.Database
            .SqlQuery<Guid>($"""
                SELECT id AS "Value" FROM services
                 WHERE active = true
                   AND search_vector @@ websearch_to_tsquery('french', {query})
                 ORDER BY ts_rank_cd(search_vector, websearch_to_tsquery('french', {query})) DESC,
                          display_order ASC
                 LIMIT {max}
                """)
            .ToListAsync(ct);

        if (rankedIds.Count == 0)
        {
            return [];
        }

        var services = await WithDetails(db.Services.Where(s => rankedIds.Contains(s.Id)))
            .ToListAsync(ct);
        var byId = services.ToDictionary(s => s.Id);
        return rankedIds.Where(byId.ContainsKey).Select(id => byId[id]).ToList();
    }

    private static IQueryable<Service> WithDetails(IQueryable<Service> source)
        => source
            .AsNoTracking()
            .AsSplitQuery()
            .Include(s => s.Category)
            .Include(s => s.Benefits)
            .Include(s => s.Offers);
}
