using Lmp.Application.Admin;
using Lmp.Application.Catalog;
using Lmp.Domain.Catalog;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Lmp.Infrastructure.Admin;

/// <summary>EF Core admin catalogue reads. Faithful port of AdminServiceRestController read methods.</summary>
public sealed class AdminCatalogService(LmpDbContext db) : IAdminCatalogService
{
    public async Task<IDictionary<string, object>> GetStatsAsync(CancellationToken ct = default)
        => new Dictionary<string, object>(StringComparer.Ordinal)
        {
            ["totalCategories"] = await db.ServiceCategories.LongCountAsync(ct),
            ["totalServices"] = await db.Services.LongCountAsync(ct),
            ["totalOffers"] = await db.ServiceOffers.LongCountAsync(ct),
            ["activeServices"] = await db.Services.LongCountAsync(s => s.Active, ct),
            ["featuredServices"] = await db.Services.LongCountAsync(s => s.Featured, ct),
        };

    public async Task<IReadOnlyList<IDictionary<string, object?>>> GetCategoriesAsync(CancellationToken ct = default)
    {
        var categories = await db.ServiceCategories.AsNoTracking()
            .OrderBy(c => c.DisplayOrder)
            .ToListAsync(ct);

        return categories.Select(c => (IDictionary<string, object?>)new Dictionary<string, object?>(StringComparer.Ordinal)
        {
            ["id"] = c.Id,
            ["name"] = c.Name,
            ["slug"] = c.Slug,
            ["description"] = c.Description,
            ["icon"] = c.Icon,
            ["displayOrder"] = c.DisplayOrder,
        }).ToList();
    }

    public async Task<IReadOnlyList<ServiceResponse>> GetAllServicesAsync(CancellationToken ct = default)
    {
        var services = await db.Services.AsNoTracking().AsSplitQuery()
            .Include(s => s.Category)
            .Include(s => s.Benefits)
            .Include(s => s.Offers)
            .ToListAsync(ct);

        return services
            .OrderBy(s => s.DisplayOrder)
            .Select(ServiceResponse.From)
            .ToList();
    }

    public async Task<IDictionary<string, object?>?> GetServiceDetailAsync(Guid id, CancellationToken ct = default)
    {
        var service = await db.Services.AsNoTracking().AsSplitQuery()
            .Include(s => s.Category)
            .Include(s => s.Benefits)
            .Include(s => s.Offers).ThenInclude(o => o.Benefits)
            .FirstOrDefaultAsync(s => s.Id == id, ct);

        if (service is null)
        {
            return null;
        }

        var offers = service.Offers.Select(o => (object)new Dictionary<string, object?>(StringComparer.Ordinal)
        {
            ["id"] = o.Id,
            ["name"] = o.Name,
            ["price"] = o.Price,
            ["originalPrice"] = o.OriginalPrice,
            ["durationType"] = DurationTypeWire(o.DurationType),
            ["isDefault"] = o.IsDefault,
            ["active"] = o.Active,
            ["benefits"] = o.Benefits
                .OrderBy(b => b.DisplayOrder)
                .Select(b => (object)new Dictionary<string, object?> { ["id"] = b.Id, ["benefit"] = b.Benefit })
                .ToList(),
        }).ToList();

        var benefits = service.Benefits
            .Select(b => (object)new Dictionary<string, object?> { ["id"] = b.Id, ["benefit"] = b.Benefit })
            .ToList();

        return new Dictionary<string, object?>(StringComparer.Ordinal)
        {
            ["service"] = ServiceResponse.From(service),
            ["categoryId"] = service.Category?.Id,
            ["offers"] = offers,
            ["benefits"] = benefits,
        };
    }

    private static string DurationTypeWire(DurationType type) => type switch
    {
        DurationType.OneTime => "ONE_TIME",
        DurationType.Monthly => "MONTHLY",
        DurationType.Yearly => "YEARLY",
        _ => type.ToString(),
    };
}
