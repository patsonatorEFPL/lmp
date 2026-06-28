using Lmp.Application.Catalog;
using Lmp.Application.Pricing;
using Lmp.Infrastructure.Catalog;
using Lmp.Infrastructure.Persistence;
using Lmp.Infrastructure.Pricing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;

namespace Lmp.Infrastructure;

/// <summary>Composition root for the infrastructure layer (persistence + integrations).</summary>
public static class DependencyInjection
{
    public static IServiceCollection AddLmpInfrastructure(
        this IServiceCollection services,
        IConfiguration configuration)
    {
        services.AddDbContext<LmpDbContext>(options =>
            options
                .UseNpgsql(configuration.GetConnectionString("LmpDb"))
                .UseSnakeCaseNamingConvention());

        services
            .AddOptions<RegionalPricingProperties>()
            .Bind(configuration.GetSection(RegionalPricingProperties.SectionName))
            .ValidateOnStart();

        // FX live refresh (Frankfurter) is a follow-up module; until then the
        // no-op cache makes pricing fall through to fixed/static/EUR rates.
        services.AddSingleton<IFxRateCache, NullFxRateCache>();
        services.AddSingleton<IRegionalPricingService, RegionalPricingService>();

        services.AddScoped<IServiceCatalogService, ServiceCatalogService>();

        return services;
    }
}
