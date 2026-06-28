using System.Text.Json;
using System.Text.Json.Serialization;
using Lmp.Infrastructure;

var builder = WebApplication.CreateBuilder(args);

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

app.MapControllers();

app.Run();
