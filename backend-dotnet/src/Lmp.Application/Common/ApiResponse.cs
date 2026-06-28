using System.Text.Json.Serialization;

namespace Lmp.Application.Common;

/// <summary>
/// Standardised API envelope. Mirrors the Java <c>com.lmp.shared.dto.ApiResponse</c>
/// record, including its <c>@JsonInclude(NON_NULL)</c> behaviour: <see cref="Message"/>
/// and <see cref="Data"/> are omitted from the payload when null.
/// </summary>
public sealed record ApiResponse<T>(
    bool Success,
    [property: JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)] string? Message,
    [property: JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)] T? Data)
{
    public static ApiResponse<T> Ok(T data) => new(true, null, data);

    public static ApiResponse<T> Ok(string? message, T data) => new(true, message, data);

    public static ApiResponse<T> Error(string message) => new(false, message, default);
}
