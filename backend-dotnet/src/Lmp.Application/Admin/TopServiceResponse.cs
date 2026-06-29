namespace Lmp.Application.Admin;

/// <summary>
/// One row of the admin "top services" widget. Port of
/// <c>com.lmp.shared.dto.admin.TopServiceDto</c>: revenue over the last 90 days
/// with growth (percent) relative to the comparison window.
/// </summary>
public sealed record TopServiceResponse(
    string Name,
    long Orders,
    decimal Revenue,
    int GrowthPercent);
