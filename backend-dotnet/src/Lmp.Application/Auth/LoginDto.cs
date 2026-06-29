using System.ComponentModel.DataAnnotations;

namespace Lmp.Application.Auth;

/// <summary>Login request body. Port of <c>com.lmp.auth.dto.LoginDto</c>.</summary>
public sealed class LoginDto
{
    [Required(ErrorMessage = "L'identifiant est obligatoire")]
    public string Email { get; set; } = null!;

    [Required(ErrorMessage = "Le mot de passe est obligatoire")]
    [MinLength(6, ErrorMessage = "Le mot de passe doit contenir au moins 6 caractères")]
    public string Password { get; set; } = null!;

    public bool RememberMe { get; set; }
}
