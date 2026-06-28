namespace Lmp.Infrastructure.Pricing;

/// <summary>
/// EUR → currency rate, with FX margin and provenance. Port of the value returned
/// by <c>FxRateCacheService.getRate</c>.
/// </summary>
/// <param name="EffectiveRate">Raw rate × (1 + margin) — the rate actually applied.</param>
/// <param name="RawRate">Rate as published by the FX source.</param>
/// <param name="Margin">Applied margin.</param>
/// <param name="Source">"frankfurter" or "static".</param>
public readonly record struct FxRate(decimal EffectiveRate, decimal RawRate, decimal Margin, string Source);

/// <summary>
/// Daily EUR-based FX rate cache (Frankfurter/ECB in the Java backend). The live
/// Frankfurter refresh is a follow-up module; until it lands the cache returns no
/// rate, so resolution falls through to fixed legal rates → static config → EUR,
/// matching the Java behaviour when <c>pricing.fx-auto-refresh=false</c>.
/// </summary>
public interface IFxRateCache
{
    bool TryGetRate(string currency, out FxRate rate);
}

/// <summary>No-op cache: never resolves a rate (FX auto-refresh effectively off).</summary>
public sealed class NullFxRateCache : IFxRateCache
{
    public bool TryGetRate(string currency, out FxRate rate)
    {
        rate = default;
        return false;
    }
}
