using System.Globalization;

namespace Lmp.Application.Crm;

/// <summary>
/// Appointment booking request from the client modal. Port of
/// <c>com.lmp.crm.dto.AppointmentRequest</c>. Intentionally attribute-free —
/// the Java controller does not bean-validate it; validation happens in the service.
/// </summary>
public sealed class AppointmentRequest
{
    public string? Name { get; set; }
    public string? Email { get; set; }
    public string? Phone { get; set; }
    public string? Service { get; set; }
    public string? Date { get; set; }
    public string? Time { get; set; }
    public string? Message { get; set; }

    /// <summary>Parse <c>date + " " + time</c> as <c>yyyy-MM-dd HH:mm</c>.</summary>
    public DateTime GetAppointmentDateTime()
    {
        var text = $"{Date} {Time}";
        if (DateTime.TryParseExact(text, "yyyy-MM-dd HH:mm", CultureInfo.InvariantCulture, DateTimeStyles.None, out var dt))
        {
            return dt;
        }

        throw new ArgumentException($"Format de date/heure invalide: {Date} {Time}");
    }

    /// <summary>Human label for a service slug. Mirrors <c>resolveServiceLabel</c>.</summary>
    public string ResolveServiceLabel() => Service switch
    {
        null => "Consultation",
        "consultation" => "Consultation générale",
        "web" => "Développement Web",
        "seo" => "Référencement SEO",
        "formation" => "Formation",
        "audit" => "Audit technique",
        "marketing" => "Marketing Digital",
        "security" => "Sécurité Web",
        "other" => "Autre",
        _ => Service,
    };
}
