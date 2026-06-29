namespace Lmp.Application.Common;

/// <summary>Business-day date helpers. Port of <c>com.lmp.shared.util.DateUtils</c>.</summary>
public static class DateUtils
{
    /// <summary>Business days strictly after <paramref name="start"/> up to and including <paramref name="end"/> (Mon–Fri).</summary>
    public static int CalculateBusinessDaysBetween(DateOnly start, DateOnly end)
    {
        if (end <= start)
        {
            return 0;
        }

        var businessDays = 0;
        for (var current = start.AddDays(1); current <= end; current = current.AddDays(1))
        {
            if (IsBusinessDay(current))
            {
                businessDays++;
            }
        }

        return businessDays;
    }

    /// <summary>True for Monday–Friday.</summary>
    public static bool IsBusinessDay(DateOnly date)
        => date.DayOfWeek is not (DayOfWeek.Saturday or DayOfWeek.Sunday);
}
