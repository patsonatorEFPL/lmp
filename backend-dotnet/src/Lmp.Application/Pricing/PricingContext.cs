namespace Lmp.Application.Pricing;

/// <summary>
/// Resolved pricing context for a request. Faithful port of
/// <c>com.lmp.shared.pricing.PricingContext</c>.
/// </summary>
/// <param name="CountryCode">ISO 3166-1 alpha-2 country code (e.g. CA, FR).</param>
/// <param name="Currency">ISO 4217 currency code (e.g. CAD, EUR).</param>
/// <param name="EurToTargetRate">Effective EUR → currency rate (raw rate × (1 + margin)).</param>
/// <param name="RawRate">Raw rate from the FX source, before margin.</param>
/// <param name="FxMargin">Applied FX margin (0 when none).</param>
/// <param name="RateSource">"frankfurter", "fixed" or "static".</param>
public sealed record PricingContext(
    string CountryCode,
    string Currency,
    decimal EurToTargetRate,
    decimal RawRate,
    decimal FxMargin,
    string RateSource)
{
    /// <summary>Backwards-compatible factory without FX metadata.</summary>
    public static PricingContext Of(string countryCode, string currency, decimal eurToTargetRate)
        => new(countryCode, currency, eurToTargetRate, eurToTargetRate, 0m, "static");
}
