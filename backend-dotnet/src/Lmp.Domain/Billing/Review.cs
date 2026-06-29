namespace Lmp.Domain.Billing;

/// <summary>A customer review of an order — maps to <c>reviews</c>.</summary>
public class Review
{
    public Guid Id { get; set; }
    public Guid? UserId { get; set; }
    public Guid OrderId { get; set; }
    public int Rating { get; set; }
    public string? Comment { get; set; }
    public bool AdminApproved { get; set; }
    public bool Featured { get; set; }
    public DateTime? CreatedAt { get; set; }
    public DateTime? ExpiresAt { get; set; }
}
