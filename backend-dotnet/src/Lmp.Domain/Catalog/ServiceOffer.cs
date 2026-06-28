namespace Lmp.Domain.Catalog;

/// <summary>
/// A priced offer for a service (default price or time-boxed promotion) —
/// maps to <c>service_offers</c>. Catalogue prices are stored in the base
/// currency (EUR); regional conversion happens at the presentation layer.
/// </summary>
public class ServiceOffer
{
    public Guid Id { get; set; }
    public Guid ServiceId { get; set; }
    public Service Service { get; set; } = null!;

    public string Name { get; set; } = null!;
    public decimal Price { get; set; }
    public decimal? OriginalPrice { get; set; }
    public DurationType DurationType { get; set; }
    public string? Duration { get; set; }
    public DateTime? ValidFrom { get; set; }
    public DateTime? ValidTo { get; set; }
    public bool IsDefault { get; set; }
    public bool Active { get; set; } = true;

    public ICollection<OfferBenefit> Benefits { get; set; } = new List<OfferBenefit>();

    /// <summary>
    /// Whether the offer is active and inside its [ValidFrom, ValidTo] window.
    /// Mirrors <c>ServiceOffer.isCurrentlyValid()</c>.
    /// </summary>
    public bool IsCurrentlyValid()
    {
        if (!Active)
        {
            return false;
        }

        var now = DateTime.Now;
        if (ValidFrom is { } from && now < from)
        {
            return false;
        }

        if (ValidTo is { } to && now > to)
        {
            return false;
        }

        return true;
    }
}
