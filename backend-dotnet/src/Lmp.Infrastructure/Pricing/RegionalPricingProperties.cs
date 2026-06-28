namespace Lmp.Infrastructure.Pricing;

/// <summary>
/// Per-country pricing grid (currency + EUR rate). Bound from the <c>Pricing</c>
/// configuration section. Port of <c>com.lmp.shared.pricing.RegionalPricingProperties</c>.
/// </summary>
public sealed class RegionalPricingProperties
{
    public const string SectionName = "Pricing";

    /// <summary>Currency of the base (catalogue) amounts.</summary>
    public string BaseCurrency { get; set; } = "EUR";

    /// <summary>Country used when geolocation fails.</summary>
    public string FallbackCountry { get; set; } = "FR";

    /// <summary>FX margin applied on the raw rate (e.g. 0.015 = +1.5%). 0 = none.</summary>
    public decimal FxMargin { get; set; }

    /// <summary>Enable automatic rate refresh via Frankfurter (ECB).</summary>
    public bool FxAutoRefresh { get; set; } = true;

    /// <summary>Apply psychological rounding to presentation prices.</summary>
    public bool PsychologicalRounding { get; set; } = true;

    /// <summary>Per-country currency/rate overrides, keyed by ISO 3166-1 alpha-2.</summary>
    public Dictionary<string, RegionRate> Region { get; set; } = new(StringComparer.OrdinalIgnoreCase);

    public sealed class RegionRate
    {
        public string? Currency { get; set; }
        public decimal? EurRate { get; set; }
    }
}
