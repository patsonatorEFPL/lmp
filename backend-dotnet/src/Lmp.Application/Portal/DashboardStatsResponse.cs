namespace Lmp.Application.Portal;

/// <summary>User dashboard statistics. Faithful port of <c>DashboardStatsResponse</c>.</summary>
public sealed record DashboardStatsResponse(
    long TotalOrders,
    long CompletedOrders,
    long InProgressOrders,
    long TotalReviews,
    long UpcomingAppointments,
    decimal TotalSpent,
    IReadOnlyList<DashboardStatsResponse.RecentOrderDto> RecentOrders,
    IReadOnlyList<DashboardStatsResponse.RecentReviewDto> RecentReviews,
    IReadOnlyList<DashboardStatsResponse.UpcomingAppointmentDto> UpcomingAppointmentsList)
{
    public sealed record RecentOrderDto(
        string Id,
        string? ServiceName,
        string? Status,
        decimal TotalAmount,
        string? Currency,
        string? CreatedAt);

    public sealed record RecentReviewDto(
        string Id,
        int Rating,
        string? Comment,
        bool Approved,
        string? CreatedAt);

    public sealed record UpcomingAppointmentDto(
        string Id,
        string Subject,
        string? Status,
        string? AppointmentDate,
        int DurationMinutes);
}
