namespace Lmp.Domain.Catalog;

/// <summary>
/// A catalogue service — maps to <c>services</c>. Aggregates its benefits and
/// priced offers, and resolves the "current" offer (valid promo &gt; default).
/// </summary>
public class Service
{
    public Guid Id { get; set; }
    public Guid CategoryId { get; set; }
    public ServiceCategory Category { get; set; } = null!;

    public string Title { get; set; } = null!;
    public string Slug { get; set; } = null!;
    public string Description { get; set; } = null!;
    public string? Icon { get; set; }
    public int DisplayOrder { get; set; }
    public bool Featured { get; set; }
    public bool Active { get; set; } = true;
    public DateTime? CreatedAt { get; set; }
    public DateTime? UpdatedAt { get; set; }

    /// <summary>Optional external-system link code (ERP-agnostic).</summary>
    public string? ExternalItemCode { get; set; }

    public ICollection<ServiceBenefit> Benefits { get; set; } = new List<ServiceBenefit>();
    public ICollection<ServiceOffer> Offers { get; set; } = new List<ServiceOffer>();

    /// <summary>
    /// The offer to display/charge: the first currently-valid non-default
    /// promotion, otherwise the active default offer. Mirrors
    /// <c>Service.getCurrentOffer()</c>.
    /// </summary>
    public ServiceOffer? GetCurrentOffer()
    {
        if (Offers.Count == 0)
        {
            return null;
        }

        var validPromo = Offers
            .Where(o => o.IsCurrentlyValid())
            .FirstOrDefault(o => !o.IsDefault);
        if (validPromo is not null)
        {
            return validPromo;
        }

        return Offers
            .Where(o => o.Active)
            .FirstOrDefault(o => o.IsDefault);
    }
}
