namespace Lmp.Application.Pricing;

/// <summary>
/// Centralised monetary-precision helpers. Faithful port of
/// <c>com.lmp.shared.pricing.MoneyUtils</c> — 2-decimal currency amounts,
/// HALF_UP (away-from-zero) rounding, aligned with the ERP currency precision
/// so LMP and the ERP produce identical results.
/// </summary>
public static class MoneyUtils
{
    public const int CurrencyScale = 2;

    public static readonly decimal Hundred = 100m;
    public static readonly decimal Zero = 0m;

    /// <summary>Round to standard currency precision (2 decimals, HALF_UP).</summary>
    public static decimal Round(decimal? amount)
        => amount is null ? Zero : Math.Round(amount.Value, CurrencyScale, MidpointRounding.AwayFromZero);

    /// <summary>Multiply then round to the cent.</summary>
    public static decimal Multiply(decimal? a, decimal? b)
        => a is null || b is null ? Zero : Math.Round(a.Value * b.Value, CurrencyScale, MidpointRounding.AwayFromZero);

    /// <summary>Divide then round to the cent. Returns zero on a null/zero denominator.</summary>
    public static decimal Divide(decimal? numerator, decimal? denominator)
    {
        if (numerator is null || denominator is null || denominator.Value == 0m)
        {
            return Zero;
        }

        return Math.Round(numerator.Value / denominator.Value, CurrencyScale, MidpointRounding.AwayFromZero);
    }

    /// <summary>Percentage of an amount — exact ERP formula <c>amount * percent / 100</c>.</summary>
    public static decimal PercentOf(decimal? amount, decimal? percent)
        => amount is null || percent is null
            ? Zero
            : Math.Round(amount.Value * percent.Value / Hundred, CurrencyScale, MidpointRounding.AwayFromZero);

    /// <summary>Apply a multiplicative rate to an amount.</summary>
    public static decimal ApplyRate(decimal? amount, decimal? rate) => Multiply(amount, rate);

    /// <summary>Extract the net (HT) amount from a gross (TTC) amount: <c>ttc / (1 + vatRate)</c>.</summary>
    public static decimal ExtractHt(decimal? amountTtc, decimal? vatRate)
    {
        if (amountTtc is null)
        {
            return Zero;
        }

        var divisor = 1m + (vatRate ?? 0m);
        return Math.Round(amountTtc.Value / divisor, CurrencyScale, MidpointRounding.AwayFromZero);
    }

    /// <summary>VAT amount of a net (HT) amount: <c>ht * vatRate</c>.</summary>
    public static decimal VatAmount(decimal? amountHt, decimal? vatRate)
        => amountHt is null || vatRate is null ? Zero : Multiply(amountHt, vatRate);
}
