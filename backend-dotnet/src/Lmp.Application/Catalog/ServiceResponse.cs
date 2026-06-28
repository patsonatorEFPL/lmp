using Lmp.Application.Pricing;
using Lmp.Domain.Catalog;

namespace Lmp.Application.Catalog;

/// <summary>
/// Catalogue service response DTO. Faithful port of
/// <c>com.lmp.catalog.dto.ServiceResponse</c>.
/// </summary>
public sealed record ServiceResponse(
    Guid Id,
    string Title,
    string Slug,
    string Description,
    string? Icon,
    string? CategoryName,
    string? CategorySlug,
    bool Featured,
    bool Active,
    int DisplayOrder,
    IReadOnlyList<string> Benefits,
    ServiceResponse.OfferResponse? CurrentOffer)
{
    /// <summary>Admin catalogue / no regional conversion (EUR prices from the database).</summary>
    public static ServiceResponse From(Service service)
    {
        var benefits = service.Benefits.Select(b => b.Benefit).ToList();
        var offer = service.GetCurrentOffer();
        return Build(service, benefits, offer is null ? null : OfferResponse.From(offer));
    }

    /// <summary>Public API: prices converted to the client's resolved currency.</summary>
    public static ServiceResponse From(Service service, PricingContext context, IRegionalPricingService pricing)
    {
        var benefits = service.Benefits.Select(b => b.Benefit).ToList();
        var offer = service.GetCurrentOffer();
        return Build(service, benefits, offer is null ? null : OfferResponse.From(offer, context, pricing));
    }

    private static ServiceResponse Build(Service service, IReadOnlyList<string> benefits, OfferResponse? offer)
        => new(
            service.Id,
            service.Title,
            service.Slug,
            service.Description,
            service.Icon,
            service.Category?.Name,
            service.Category?.Slug,
            service.Featured,
            service.Active,
            service.DisplayOrder,
            benefits,
            offer);

    public sealed record OfferResponse(
        Guid Id,
        string Name,
        decimal? Price,
        decimal? OriginalPrice,
        string? DurationType,
        bool IsDefault,
        string Currency,
        decimal? PriceEur)
    {
        public static OfferResponse From(ServiceOffer offer)
            => new(
                offer.Id,
                offer.Name,
                offer.Price,
                offer.OriginalPrice,
                DurationTypeWire(offer.DurationType),
                offer.IsDefault,
                "EUR",
                offer.Price);

        public static OfferResponse From(ServiceOffer offer, PricingContext context, IRegionalPricingService pricing)
        {
            var display = pricing.ConvertFromEur(offer.Price, context);
            var displayOriginal = offer.OriginalPrice is null
                ? null
                : pricing.ConvertFromEur(offer.OriginalPrice, context);
            return new OfferResponse(
                offer.Id,
                offer.Name,
                display,
                displayOriginal,
                DurationTypeWire(offer.DurationType),
                offer.IsDefault,
                context.Currency,
                offer.Price);
        }
    }

    /// <summary>Wire name matching the Java enum (<c>DurationType.name()</c>).</summary>
    private static string DurationTypeWire(DurationType type) => type switch
    {
        Lmp.Domain.Catalog.DurationType.OneTime => "ONE_TIME",
        Lmp.Domain.Catalog.DurationType.Monthly => "MONTHLY",
        Lmp.Domain.Catalog.DurationType.Yearly => "YEARLY",
        _ => type.ToString(),
    };
}
