namespace Lmp.Application.Pricing;

/// <summary>
/// Psychological rounding of presentation prices. Faithful port of
/// <c>com.lmp.shared.pricing.PsychologicalRounder</c>:
/// <list type="bullet">
///   <item>amounts &lt; 10 000 ending in .00 or .50 drop to .99 / .49;</item>
///   <item>amounts ≥ 10 000 round up to the next multiple of 5;</item>
///   <item>zero-decimal currencies (JPY, XOF, …) round to the unit.</item>
/// </list>
/// </summary>
public static class PsychologicalRounder
{
    private const decimal Threshold = 10000m;

    private static readonly HashSet<string> ZeroDecimal = new(StringComparer.OrdinalIgnoreCase)
    {
        "JPY", "KRW", "VND", "BIF", "CLP", "GNF",
        "MGA", "PYG", "RWF", "UGX", "XAF", "XOF", "XPF",
    };

    public static decimal? Round(decimal? amount, string currency)
    {
        if (amount is null)
        {
            return null;
        }

        if (ZeroDecimal.Contains(currency))
        {
            return Math.Round(amount.Value, 0, MidpointRounding.AwayFromZero);
        }

        var @base = Math.Round(amount.Value, 2, MidpointRounding.AwayFromZero);
        if (@base >= Threshold)
        {
            // Round up to the next multiple of 5: 12 347 → 12 350.
            var divided = Math.Ceiling(@base / 5m);
            return divided * 5m;
        }

        var cents = (int)Math.Round(@base * 100m, 0, MidpointRounding.AwayFromZero);
        var lastTwo = cents % 100;
        if (lastTwo is 0 or 50)
        {
            return @base - 0.01m;
        }

        return @base;
    }
}
