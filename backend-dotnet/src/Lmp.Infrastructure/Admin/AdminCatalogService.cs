using System.Globalization;
using System.Text.Json;
using System.Text.RegularExpressions;
using Lmp.Application.Admin;
using Lmp.Application.Catalog;
using Lmp.Domain.Catalog;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Lmp.Infrastructure.Admin;

/// <summary>EF Core admin catalogue management. Faithful port of AdminServiceRestController.</summary>
public sealed partial class AdminCatalogService(LmpDbContext db) : IAdminCatalogService
{
    // ── Reads ────────────────────────────────────────────────────────────────
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
        var categories = await db.ServiceCategories.AsNoTracking().OrderBy(c => c.DisplayOrder).ToListAsync(ct);
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
            .Include(s => s.Category).Include(s => s.Benefits).Include(s => s.Offers)
            .ToListAsync(ct);
        return services.OrderBy(s => s.DisplayOrder).Select(ServiceResponse.From).ToList();
    }

    public async Task<IDictionary<string, object?>?> GetServiceDetailAsync(Guid id, CancellationToken ct = default)
    {
        var service = await db.Services.AsNoTracking().AsSplitQuery()
            .Include(s => s.Category).Include(s => s.Benefits)
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
            ["benefits"] = o.Benefits.OrderBy(b => b.DisplayOrder)
                .Select(b => (object)new Dictionary<string, object?> { ["id"] = b.Id, ["benefit"] = b.Benefit }).ToList(),
        }).ToList();

        var benefits = service.Benefits
            .Select(b => (object)new Dictionary<string, object?> { ["id"] = b.Id, ["benefit"] = b.Benefit }).ToList();

        return new Dictionary<string, object?>(StringComparer.Ordinal)
        {
            ["service"] = ServiceResponse.From(service),
            ["categoryId"] = service.Category?.Id,
            ["offers"] = offers,
            ["benefits"] = benefits,
        };
    }

    // ── Category writes ──────────────────────────────────────────────────────
    public async Task<IDictionary<string, object?>> CreateCategoryAsync(IDictionary<string, JsonElement> data, CancellationToken ct = default)
    {
        var name = Str(data, "name");
        var category = new ServiceCategory
        {
            Name = name!,
            Slug = Slugify(name),
            Description = StrOr(data, "description", string.Empty),
            Icon = StrOr(data, "icon", "📁"),
            DisplayOrder = Int(data, "displayOrder") ?? 0,
        };
        db.ServiceCategories.Add(category);
        await db.SaveChangesAsync(ct);
        return new Dictionary<string, object?> { ["id"] = category.Id, ["name"] = category.Name };
    }

    public async Task UpdateCategoryAsync(Guid id, IDictionary<string, JsonElement> data, CancellationToken ct = default)
    {
        var category = await db.ServiceCategories.FirstOrDefaultAsync(c => c.Id == id, ct)
            ?? throw new ArgumentException("Catégorie non trouvée");
        if (data.ContainsKey("name")) category.Name = Str(data, "name")!;
        if (data.ContainsKey("description")) category.Description = Str(data, "description");
        if (data.ContainsKey("icon")) category.Icon = Str(data, "icon");
        if (Int(data, "displayOrder") is { } order) category.DisplayOrder = order;
        await db.SaveChangesAsync(ct);
    }

    public async Task DeleteCategoryAsync(Guid id, CancellationToken ct = default)
    {
        try
        {
            await db.ServiceCategories.Where(c => c.Id == id).ExecuteDeleteAsync(ct);
        }
        catch (Exception ex) when (ex is DbUpdateException or Npgsql.PostgresException)
        {
            throw new ArgumentException("Impossible de supprimer (services liés ?)");
        }
    }

    // ── Service writes ───────────────────────────────────────────────────────
    public async Task<IDictionary<string, object?>> CreateServiceAsync(IDictionary<string, JsonElement> data, CancellationToken ct = default)
    {
        var categoryId = Guid.Parse(Str(data, "categoryId")!);
        _ = await db.ServiceCategories.AnyAsync(c => c.Id == categoryId, ct)
            ? true
            : throw new ArgumentException("Catégorie non trouvée");

        var title = Str(data, "title");
        var now = DateTime.UtcNow;
        var service = new Service
        {
            CategoryId = categoryId,
            Title = title!,
            Slug = Slugify(title),
            Description = StrOr(data, "description", string.Empty),
            Icon = StrOr(data, "icon", "📦"),
            DisplayOrder = Int(data, "displayOrder") ?? 0,
            Featured = Bool(data, "featured"),
            Active = !data.ContainsKey("active") || Bool(data, "active"),
            CreatedAt = now,
            UpdatedAt = now,
        };

        foreach (var text in StrList(data, "benefits"))
        {
            if (!string.IsNullOrWhiteSpace(text))
            {
                service.Benefits.Add(new ServiceBenefit { Benefit = text.Trim() });
            }
        }

        db.Services.Add(service);
        await db.SaveChangesAsync(ct);
        return new Dictionary<string, object?> { ["id"] = service.Id, ["title"] = service.Title };
    }

    public async Task UpdateServiceAsync(Guid id, IDictionary<string, JsonElement> data, CancellationToken ct = default)
    {
        var service = await db.Services.FirstOrDefaultAsync(s => s.Id == id, ct)
            ?? throw new ArgumentException("Service non trouvé");

        if (data.ContainsKey("title")) service.Title = Str(data, "title")!;
        if (data.ContainsKey("description")) service.Description = Str(data, "description")!;
        if (data.ContainsKey("icon")) service.Icon = Str(data, "icon");
        if (Int(data, "displayOrder") is { } order) service.DisplayOrder = order;
        if (data.ContainsKey("featured")) service.Featured = Bool(data, "featured");
        if (data.ContainsKey("active")) service.Active = Bool(data, "active");
        if (data.ContainsKey("categoryId"))
        {
            var categoryId = Guid.Parse(Str(data, "categoryId")!);
            if (!await db.ServiceCategories.AnyAsync(c => c.Id == categoryId, ct))
            {
                throw new ArgumentException("Catégorie non trouvée");
            }

            service.CategoryId = categoryId;
        }

        service.UpdatedAt = DateTime.UtcNow;
        await db.SaveChangesAsync(ct);
    }

    public async Task DeleteServiceAsync(Guid id, CancellationToken ct = default)
    {
        try
        {
            await db.Services.Where(s => s.Id == id).ExecuteDeleteAsync(ct);
        }
        catch (Exception ex) when (ex is DbUpdateException or Npgsql.PostgresException)
        {
            throw new ArgumentException("Impossible de supprimer (commandes liées ?)");
        }
    }

    public async Task ReorderServicesAsync(IReadOnlyList<Guid> serviceIds, CancellationToken ct = default)
    {
        if (serviceIds.Count == 0)
        {
            throw new ArgumentException("Liste d'IDs requise");
        }

        var now = DateTime.UtcNow;
        for (var i = 0; i < serviceIds.Count; i++)
        {
            var service = await db.Services.FirstOrDefaultAsync(s => s.Id == serviceIds[i], ct)
                ?? throw new ArgumentException("Service non trouvé: " + serviceIds[i]);
            service.DisplayOrder = i + 1;
            service.UpdatedAt = now;
        }

        await db.SaveChangesAsync(ct);
    }

    // ── Offer writes ─────────────────────────────────────────────────────────
    public async Task<IDictionary<string, object?>> CreateOfferAsync(Guid serviceId, IDictionary<string, JsonElement> data, CancellationToken ct = default)
    {
        if (!await db.Services.AnyAsync(s => s.Id == serviceId, ct))
        {
            throw new ArgumentException("Service non trouvé");
        }

        var offer = new ServiceOffer
        {
            ServiceId = serviceId,
            Name = Str(data, "name")!,
            Price = Dec(data, "price") ?? 0m,
            OriginalPrice = Dec(data, "originalPrice"),
            DurationType = ParseDuration(StrOr(data, "durationType", "ONE_TIME")),
            IsDefault = Bool(data, "isDefault"),
            Active = !data.ContainsKey("active") || Bool(data, "active"),
        };
        db.ServiceOffers.Add(offer);
        await db.SaveChangesAsync(ct);
        return new Dictionary<string, object?> { ["id"] = offer.Id };
    }

    public async Task UpdateOfferAsync(Guid id, IDictionary<string, JsonElement> data, CancellationToken ct = default)
    {
        var offer = await db.ServiceOffers.FirstOrDefaultAsync(o => o.Id == id, ct)
            ?? throw new ArgumentException("Offre non trouvée");
        if (data.ContainsKey("name")) offer.Name = Str(data, "name")!;
        if (Dec(data, "price") is { } price) offer.Price = price;
        if (Dec(data, "originalPrice") is { } original) offer.OriginalPrice = original;
        if (data.ContainsKey("durationType")) offer.DurationType = ParseDuration(Str(data, "durationType")!);
        if (data.ContainsKey("isDefault")) offer.IsDefault = Bool(data, "isDefault");
        if (data.ContainsKey("active")) offer.Active = Bool(data, "active");
        await db.SaveChangesAsync(ct);
    }

    public async Task DeleteOfferAsync(Guid id, CancellationToken ct = default)
        => await db.ServiceOffers.Where(o => o.Id == id).ExecuteDeleteAsync(ct);

    // ── Benefit writes ───────────────────────────────────────────────────────
    public async Task<IDictionary<string, object?>> CreateBenefitAsync(Guid serviceId, IDictionary<string, JsonElement> data, CancellationToken ct = default)
    {
        if (!await db.Services.AnyAsync(s => s.Id == serviceId, ct))
        {
            throw new ArgumentException("Service non trouvé");
        }

        var benefit = new ServiceBenefit { ServiceId = serviceId, Benefit = Str(data, "benefit")! };
        db.ServiceBenefits.Add(benefit);
        await db.SaveChangesAsync(ct);
        return new Dictionary<string, object?> { ["id"] = benefit.Id };
    }

    public async Task DeleteBenefitAsync(Guid id, CancellationToken ct = default)
        => await db.ServiceBenefits.Where(b => b.Id == id).ExecuteDeleteAsync(ct);

    public async Task SyncBenefitsAsync(Guid serviceId, IDictionary<string, JsonElement> data, CancellationToken ct = default)
    {
        if (!await db.Services.AnyAsync(s => s.Id == serviceId, ct))
        {
            throw new ArgumentException("Service non trouvé");
        }

        await db.ServiceBenefits.Where(b => b.ServiceId == serviceId).ExecuteDeleteAsync(ct);
        var fresh = StrList(data, "benefits")
            .Where(t => !string.IsNullOrWhiteSpace(t))
            .Select(t => new ServiceBenefit { ServiceId = serviceId, Benefit = t.Trim() });
        db.ServiceBenefits.AddRange(fresh);
        await db.SaveChangesAsync(ct);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private static string DurationTypeWire(DurationType type) => type switch
    {
        DurationType.OneTime => "ONE_TIME",
        DurationType.Monthly => "MONTHLY",
        DurationType.Yearly => "YEARLY",
        _ => type.ToString(),
    };

    private static DurationType ParseDuration(string value) => value switch
    {
        "ONE_TIME" => DurationType.OneTime,
        "MONTHLY" => DurationType.Monthly,
        "YEARLY" => DurationType.Yearly,
        _ => throw new ArgumentException("Invalid durationType: " + value),
    };

    /// <summary>Slug from a label. Faithful port of <c>slugify</c> (accent folding + non-alphanumerics to hyphens).</summary>
    private static string Slugify(string? text)
    {
        if (text is null)
        {
            return string.Empty;
        }

        var s = text.ToLowerInvariant();
        s = AccentE().Replace(s, "e");
        s = AccentA().Replace(s, "a");
        s = AccentU().Replace(s, "u");
        s = AccentO().Replace(s, "o");
        s = AccentI().Replace(s, "i");
        s = s.Replace('ç', 'c');
        s = NonAlphaNum().Replace(s, "-");
        s = TrimHyphens().Replace(s, string.Empty);
        return s;
    }

    private static string? Str(IDictionary<string, JsonElement> data, string key)
        => data.TryGetValue(key, out var v) && v.ValueKind != JsonValueKind.Null ? v.GetString() : null;

    private static string StrOr(IDictionary<string, JsonElement> data, string key, string fallback)
        => Str(data, key) ?? fallback;

    private static int? Int(IDictionary<string, JsonElement> data, string key)
        => data.TryGetValue(key, out var v) && v.ValueKind == JsonValueKind.Number ? v.GetInt32() : null;

    private static bool Bool(IDictionary<string, JsonElement> data, string key)
        => data.TryGetValue(key, out var v) && v.ValueKind == JsonValueKind.True;

    private static decimal? Dec(IDictionary<string, JsonElement> data, string key)
    {
        if (!data.TryGetValue(key, out var v))
        {
            return null;
        }

        return v.ValueKind switch
        {
            JsonValueKind.Number => v.GetDecimal(),
            JsonValueKind.String when decimal.TryParse(v.GetString(), NumberStyles.Number, CultureInfo.InvariantCulture, out var d) => d,
            _ => null,
        };
    }

    private static IEnumerable<string> StrList(IDictionary<string, JsonElement> data, string key)
    {
        if (data.TryGetValue(key, out var v) && v.ValueKind == JsonValueKind.Array)
        {
            foreach (var item in v.EnumerateArray())
            {
                if (item.ValueKind == JsonValueKind.String && item.GetString() is { } s)
                {
                    yield return s;
                }
            }
        }
    }

    [GeneratedRegex("[éèêë]")] private static partial Regex AccentE();
    [GeneratedRegex("[àâä]")] private static partial Regex AccentA();
    [GeneratedRegex("[ùûü]")] private static partial Regex AccentU();
    [GeneratedRegex("[ôö]")] private static partial Regex AccentO();
    [GeneratedRegex("[îï]")] private static partial Regex AccentI();
    [GeneratedRegex("[^a-z0-9]+")] private static partial Regex NonAlphaNum();
    [GeneratedRegex("^-|-$")] private static partial Regex TrimHyphens();
}
