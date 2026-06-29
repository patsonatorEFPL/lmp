using System.Text.RegularExpressions;

namespace Lmp.Application.Common;

/// <summary>
/// Normalisation/validation of an intra-EU VAT identifier (ISO country prefix +
/// national part). Port of <c>com.lmp.shared.util.VatIdentifierUtils</c>. Not a
/// VIES substitute.
/// </summary>
public static partial class VatIdentifierUtils
{
    [GeneratedRegex("^[A-Z]{2}[0-9A-Z]{2,28}$")]
    private static partial Regex EuStyleVat();

    /// <summary>Strip spaces/dots/hyphens and upper-case.</summary>
    public static string Normalize(string? raw)
        => string.IsNullOrWhiteSpace(raw)
            ? string.Empty
            : MyRegex().Replace(raw, string.Empty).ToUpperInvariant();

    /// <summary>Plausible format: 2 country letters + ≥2 alphanumerics (no VIES call).</summary>
    public static bool IsPlausibleEuVatFormat(string? normalized)
        => normalized is not null && normalized.Length >= 4 && EuStyleVat().IsMatch(normalized);

    [GeneratedRegex("[\\s.\\-]")]
    private static partial Regex MyRegex();
}
