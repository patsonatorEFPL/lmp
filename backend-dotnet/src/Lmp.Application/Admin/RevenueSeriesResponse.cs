namespace Lmp.Application.Admin;

/// <summary>
/// Revenue line-chart series for the admin dashboard. Port of
/// <c>com.lmp.shared.dto.admin.RevenueSeriesDto</c>: <see cref="Current"/> and
/// <see cref="Previous"/> are per-bucket revenue sums (this period vs the same
/// buckets one year earlier), aligned positionally with <see cref="Labels"/>.
/// </summary>
public sealed record RevenueSeriesResponse(
    IReadOnlyList<decimal> Current,
    IReadOnlyList<decimal> Previous,
    IReadOnlyList<string> Labels);
