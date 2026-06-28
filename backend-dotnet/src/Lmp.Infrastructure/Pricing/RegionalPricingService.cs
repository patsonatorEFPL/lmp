using System.Collections.Frozen;
using Lmp.Application.Pricing;
using Microsoft.Extensions.Options;

namespace Lmp.Infrastructure.Pricing;

/// <summary>
/// Country → currency → rate → final price resolution. Faithful port of
/// <c>com.lmp.shared.pricing.RegionalPricingService</c>.
/// </summary>
public sealed class RegionalPricingService(
    IOptions<RegionalPricingProperties> options,
    IFxRateCache fxRateCache) : IRegionalPricingService
{
    /// <summary>
    /// Fixed legal EUR → currency parities, used when the FX cache does not cover
    /// the currency and no static config rate exists.
    /// </summary>
    private static readonly FrozenDictionary<string, decimal> FixedEurRates = new Dictionary<string, decimal>(StringComparer.Ordinal)
    {
        ["XOF"] = 655.957m, // West-African CFA franc (UEMOA)
        ["XAF"] = 655.957m, // Central-African CFA franc (CEMAC)
        ["KMF"] = 491.968m, // Comorian franc
        ["CVE"] = 110.265m, // Cape-Verdean escudo
    }.ToFrozenDictionary(StringComparer.Ordinal);

    private readonly RegionalPricingProperties _properties = options.Value;

    public PricingContext ContextForCountry(string? iso2Country, string? currencyHint = null)
    {
        if (string.IsNullOrWhiteSpace(iso2Country))
        {
            return EurContext("XX");
        }

        var key = iso2Country.Trim().ToUpperInvariant();

        // ── 1. Currency ───────────────────────────────────────────────────────
        _properties.Region.TryGetValue(key, out var regionRate);
        string currency;
        decimal? configStaticRate = null;

        if (regionRate is not null && !string.IsNullOrWhiteSpace(regionRate.Currency))
        {
            // Explicit config always wins (lets us correct a wrong geo currency).
            currency = regionRate.Currency.Trim().ToUpperInvariant();
            configStaticRate = regionRate.EurRate;
        }
        else if (!string.IsNullOrWhiteSpace(currencyHint))
        {
            currency = currencyHint.Trim().ToUpperInvariant();
        }
        else
        {
            return EurContext(key);
        }

        // ── 2. EUR → currency rate ────────────────────────────────────────────
        // 2a. FX cache (Frankfurter, daily).
        if (fxRateCache.TryGetRate(currency, out var fx))
        {
            return new PricingContext(key, currency, fx.EffectiveRate, fx.RawRate, fx.Margin, fx.Source);
        }

        // 2b. Fixed legal rate (XOF, XAF, KMF, CVE).
        if (FixedEurRates.TryGetValue(currency, out var fixedRate))
        {
            return new PricingContext(key, currency, fixedRate, fixedRate, 0m, "fixed");
        }

        // 2c. Static config rate (Pricing:Region:XX:EurRate).
        if (configStaticRate is { } staticRate && staticRate > 0m)
        {
            return new PricingContext(key, currency, staticRate, staticRate, 0m, "static");
        }

        // 2d. No known rate → base currency.
        return EurContext(key);
    }

    public PricingContext ResolveForPayment(PricingContext displayContext)
        => IRegionalPricingService.StripePaymentCurrencies.Contains(displayContext.Currency)
            ? displayContext
            : EurContext(displayContext.CountryCode);

    public decimal? ConvertFromEur(decimal? amountEur, PricingContext context)
    {
        if (amountEur is null)
        {
            return null;
        }

        var converted = MoneyUtils.Multiply(amountEur, context.EurToTargetRate);
        return _properties.PsychologicalRounding
            ? PsychologicalRounder.Round(converted, context.Currency)
            : converted;
    }

    public decimal? ToBaseEur(decimal? amountLocal, PricingContext context)
    {
        if (amountLocal is null || context.EurToTargetRate == 0m)
        {
            return amountLocal;
        }

        return MoneyUtils.Divide(amountLocal, context.EurToTargetRate);
    }

    private PricingContext EurContext(string countryCode)
    {
        var @base = string.IsNullOrWhiteSpace(_properties.BaseCurrency)
            ? "EUR"
            : _properties.BaseCurrency.Trim().ToUpperInvariant();
        return new PricingContext(countryCode, @base, 1m, 1m, 0m, "static");
    }
}
