namespace Lmp.Application.Auth;

/// <summary>Password strength check. Port of <c>UserServiceImpl.isPasswordStrong</c>.</summary>
public static class PasswordPolicy
{
    private const string Specials = "!@#$%^&*(),.?\":{}|<>";

    /// <summary>≥8 chars with a lowercase, uppercase, digit and special character.</summary>
    public static bool IsStrong(string? password)
    {
        if (password is null || password.Length < 8)
        {
            return false;
        }

        var hasLower = password.Any(char.IsLower);
        var hasUpper = password.Any(char.IsUpper);
        var hasDigit = password.Any(char.IsDigit);
        var hasSpecial = password.Any(c => Specials.Contains(c, StringComparison.Ordinal));
        return hasLower && hasUpper && hasDigit && hasSpecial;
    }
}
