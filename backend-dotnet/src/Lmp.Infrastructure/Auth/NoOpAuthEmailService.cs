using Lmp.Application.Auth;
using Lmp.Domain.Auth;
using Microsoft.Extensions.Logging;

namespace Lmp.Infrastructure.Auth;

/// <summary>
/// Placeholder auth-email service that logs instead of sending. Replaced when the
/// notification module (templated, queued mail) is ported. Registration's HTTP
/// contract is unaffected — the Java backend sends these asynchronously.
/// </summary>
public sealed class NoOpAuthEmailService(ILogger<NoOpAuthEmailService> logger) : IAuthEmailService
{
    public Task SendVerificationEmailAsync(User user, CancellationToken ct = default)
    {
        logger.LogInformation("[email-stub] verification email to {Email} (token={Token})", user.Email, user.VerificationToken);
        return Task.CompletedTask;
    }

    public Task SendWelcomeEmailAsync(User user, CancellationToken ct = default)
    {
        logger.LogInformation("[email-stub] welcome email to {Email}", user.Email);
        return Task.CompletedTask;
    }
}
