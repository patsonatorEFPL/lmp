namespace Lmp.Domain.Catalog;

/// <summary>A bullet-point benefit attached to a service — maps to <c>service_benefits</c>.</summary>
public class ServiceBenefit
{
    public Guid Id { get; set; }
    public Guid ServiceId { get; set; }
    public Service Service { get; set; } = null!;
    public string Benefit { get; set; } = null!;
}
