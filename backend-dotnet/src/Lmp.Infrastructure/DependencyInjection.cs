using Lmp.Application.Auth;
using Lmp.Application.Billing;
using Lmp.Application.Catalog;
using Lmp.Application.Crm;
using Lmp.Application.Notification;
using Lmp.Application.Pricing;
using Lmp.Application.SiteConfiguration;
using Lmp.Infrastructure.Auth;
using Lmp.Infrastructure.Billing;
using Lmp.Infrastructure.Catalog;
using Lmp.Infrastructure.Crm;
using Lmp.Infrastructure.Notification;
using Lmp.Infrastructure.Persistence;
using Lmp.Infrastructure.Pricing;
using Lmp.Infrastructure.SiteConfiguration;
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

        // Site configuration (hot-editable, hierarchical resolution).
        services
            .AddOptions<SiteConfigOptions>()
            .Bind(configuration.GetSection(SiteConfigOptions.SectionName));
        services.AddSingleton<ISiteConfigManager, SiteConfigManager>();
        services.AddSingleton<IAuthHostResolver, AuthHostResolver>();

        // Auth.
        services.AddSingleton<IPasswordEncoder, DelegatingPasswordEncoder>();
        services.AddSingleton<IDisposableEmailBlocklist, DisposableEmailBlocklist>();
        services.AddSingleton<IAuthEmailService, NoOpAuthEmailService>();
        services.AddScoped<IUserService, UserService>();
        services.AddScoped<IAuthService, AuthService>();

        // Billing.
        services.AddScoped<ICartService, CartService>();
        services.AddScoped<IOrderQueryService, OrderQueryService>();

        // CRM.
        services.AddScoped<IAppointmentService, AppointmentService>();

        // Notification.
        services.AddScoped<IInAppNotificationService, InAppNotificationService>();

        return services;
    }
}
