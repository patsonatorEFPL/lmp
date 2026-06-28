namespace Lmp.Domain.Catalog;

/// <summary>
/// Billing cadence of a <see cref="ServiceOffer"/>. Persisted as the literal
/// enum name ("ONE_TIME", "MONTHLY", "YEARLY") to match the Java/Flyway schema
/// (<c>service_offers.duration_type VARCHAR(20)</c>).
/// </summary>
public enum DurationType
{
    OneTime,
    Monthly,
    Yearly,
}
