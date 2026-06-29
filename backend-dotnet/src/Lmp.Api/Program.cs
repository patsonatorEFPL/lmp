using System.Text.Json;
using System.Text.Json.Serialization;
using Lmp.Infrastructure;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.DataProtection;
using Microsoft.AspNetCore.Diagnostics.HealthChecks;
using Microsoft.Extensions.Diagnostics.HealthChecks;

// The Flyway schema uses `timestamp without time zone` throughout (Java
// LocalDateTime — no offset). Enable Npgsql's legacy timestamp behaviour so
// DateTime maps to `timestamp without time zone` and tz-less values write
// correctly. Must run before the data source is built.
AppContext.SetSwitch("Npgsql.EnableLegacyTimestampBehavior", true);

var builder = WebApplication.CreateBuilder(args);

// Cookie-based HTTP sessions (no JWT), matching the Spring backend's model: the
// Angular SPA calls with credentials and the browser carries the session cookie.
// API endpoints answer 401/403 rather than redirecting to a login page.
builder.Services
    .AddAuthentication(CookieAuthenticationDefaults.AuthenticationScheme)
    .AddCookie(options =>
    {
        options.Cookie.Name = "LMP_SESSION";
        options.Cookie.HttpOnly = true;
        options.Cookie.SameSite = SameSiteMode.Lax;
        options.Cookie.SecurePolicy = CookieSecurePolicy.SameAsRequest;
        options.SlidingExpiration = true;
        options.ExpireTimeSpan = TimeSpan.FromDays(30);
        options.Events.OnRedirectToLogin = ctx =>
        {
            ctx.Response.StatusCode = StatusCodes.Status401Unauthorized;
            return Task.CompletedTask;
        };
        options.Events.OnRedirectToAccessDenied = ctx =>
        {
            ctx.Response.StatusCode = StatusCodes.Status403Forbidden;
            return Task.CompletedTask;
        };
    });

builder.Services.AddAuthorization();

// Persist the DataProtection key ring so the LMP_SESSION auth cookie stays
// valid across restarts (and can be shared by multiple replicas). In the
// container the keys live on a mounted volume (DataProtection__KeyPath=/keys);
// with no path configured (local dev) the default ephemeral key ring is used.
var dataProtection = builder.Services.AddDataProtection().SetApplicationName("lmp-services");
var dataProtectionKeyPath = builder.Configuration["DataProtection:KeyPath"];
if (!string.IsNullOrWhiteSpace(dataProtectionKeyPath))
{
    dataProtection.PersistKeysToFileSystem(new DirectoryInfo(dataProtectionKeyPath));
}

builder.Services
    .AddControllers()
    .AddJsonOptions(o =>
    {
        // Match the Java/Jackson wire format: camelCase property names. Null
        // omission is handled per-DTO (see ApiResponse) so other payloads keep
        // their explicit nulls, exactly like the Spring backend.
        o.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
        o.JsonSerializerOptions.DefaultIgnoreCondition = JsonIgnoreCondition.Never;
    });

builder.Services.AddLmpInfrastructure(builder.Configuration);

var app = builder.Build();

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

// Health probes — Spring Boot Actuator-compatible paths and response shape
// ({"status":"UP"|"DOWN"}). Liveness runs no checks (process up); readiness
// verifies the DB (checks tagged "ready"). Both are anonymous.
Task WriteHealth(HttpContext ctx, HealthReport report)
{
    ctx.Response.ContentType = "application/json";
    var status = report.Status == HealthStatus.Healthy ? "UP" : "DOWN";
    return ctx.Response.WriteAsync(JsonSerializer.Serialize(new { status }));
}

app.MapHealthChecks("/actuator/health/liveness", new HealthCheckOptions
{
    Predicate = _ => false,
    ResponseWriter = WriteHealth,
});
app.MapHealthChecks("/actuator/health/readiness", new HealthCheckOptions
{
    Predicate = static check => check.Tags.Contains("ready"),
    ResponseWriter = WriteHealth,
});
app.MapHealthChecks("/actuator/health", new HealthCheckOptions { ResponseWriter = WriteHealth });

app.Run();
