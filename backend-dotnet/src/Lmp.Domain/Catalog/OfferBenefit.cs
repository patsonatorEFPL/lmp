namespace Lmp.Domain.Catalog;

/// <summary>A benefit line attached to an offer — maps to <c>offer_benefits</c>.</summary>
public class OfferBenefit
{
    public Guid Id { get; set; }
    public Guid OfferId { get; set; }
    public ServiceOffer Offer { get; set; } = null!;
    public string Benefit { get; set; } = null!;
    public int DisplayOrder { get; set; }
}
