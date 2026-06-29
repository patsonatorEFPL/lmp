using System.ComponentModel.DataAnnotations;

namespace Lmp.Application.Auth;

/// <summary>Registration request body. Port of <c>com.lmp.auth.dto.RegisterDto</c>.</summary>
public sealed class RegisterDto
{
    [MaxLength(50, ErrorMessage = "Le prénom ne peut pas dépasser 50 caractères")]
    public string? FirstName { get; set; }

    [MaxLength(50, ErrorMessage = "Le nom ne peut pas dépasser 50 caractères")]
    public string? LastName { get; set; }

    [Required(ErrorMessage = "L'email est obligatoire")]
    [EmailAddress(ErrorMessage = "Format d'email invalide")]
    [MaxLength(100, ErrorMessage = "L'email ne peut pas dépasser 100 caractères")]
    public string Email { get; set; } = null!;

    [Required(ErrorMessage = "Le mot de passe est obligatoire")]
    [StringLength(100, MinimumLength = 6, ErrorMessage = "Le mot de passe doit contenir entre 6 et 100 caractères")]
    public string Password { get; set; } = null!;

    [Required(ErrorMessage = "La confirmation du mot de passe est obligatoire")]
    public string ConfirmPassword { get; set; } = null!;

    [MaxLength(20, ErrorMessage = "Le téléphone ne peut pas dépasser 20 caractères")]
    public string? Phone { get; set; }

    [MaxLength(255, ErrorMessage = "L'adresse ne peut pas dépasser 255 caractères")]
    public string? Address { get; set; }

    [MaxLength(50, ErrorMessage = "La ville ne peut pas dépasser 50 caractères")]
    public string? City { get; set; }

    [MaxLength(20, ErrorMessage = "Le code postal ne peut pas dépasser 20 caractères")]
    public string? PostalCode { get; set; }

    [MaxLength(50, ErrorMessage = "Le pays ne peut pas dépasser 50 caractères")]
    public string? Country { get; set; }

    [MaxLength(100, ErrorMessage = "Le nom de l'entreprise ne peut pas dépasser 100 caractères")]
    public string? CompanyName { get; set; }

    public bool AcceptTerms { get; set; }

    public bool IsPasswordMatching() => Password is not null && Password == ConfirmPassword;
}
