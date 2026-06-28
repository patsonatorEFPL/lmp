namespace Lmp.Application.Pricing;

/// <summary>
/// Country → currency → rate → final price resolution (FX margin + psychological
/// rounding). Contract for <c>com.lmp.shared.pricing.RegionalPricingService</c>.
/// </summary>
public interface IRegionalPricingService
{
    /// <summary>Stripe-supported payment currencies; anything else falls back to EUR at charge time.</summary>
    static readonly IReadOnlySet<string> StripePaymentCurrencies = new HashSet<string>(StringComparer.Ordinal)
    {
        "CAD", "USD", "EUR", "GBP", "AUD", "JPY", "CHF", "SEK", "NOK", "DKK",
    };

    /// <summary>Resolve the pricing context for an ISO country code, with an optional currency hint.</summary>
    PricingContext ContextForCountry(string? iso2Country, string? currencyHint = null);

    /// <summary>Pick the charge-time context, downgrading to EUR for Stripe-unsupported currencies.</summary>
    PricingContext ResolveForPayment(PricingContext displayContext);

    /// <summary>Convert an EUR amount to the target currency, applying psychological rounding when enabled.</summary>
    decimal? ConvertFromEur(decimal? amountEur, PricingContext context);

    /// <summary>Convert a local-currency amount back to base EUR.</summary>
    decimal? ToBaseEur(decimal? amountLocal, PricingContext context);
}
