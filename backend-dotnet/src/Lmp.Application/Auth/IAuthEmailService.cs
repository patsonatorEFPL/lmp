using Lmp.Domain.Auth;

namespace Lmp.Application.Auth;

/// <summary>
/// Transactional auth emails (verification, welcome). The real implementation —
/// Thymeleaf-equivalent templates queued through the mail subsystem — belongs to
/// the notification module (not yet ported); a no-op logging implementation backs
/// this seam in the meantime. Registration's HTTP contract is unaffected because
/// the Java backend sends these asynchronously and always returns a generic 200.
/// </summary>
public interface IAuthEmailService
{
    Task SendVerificationEmailAsync(User user, CancellationToken ct = default);

    Task SendWelcomeEmailAsync(User user, CancellationToken ct = default);
}
