namespace Lmp.Application.Common;

/// <summary>
/// JSON-compatible projection of Spring Data's <c>Page&lt;T&gt;</c>, so the
/// frontend's generated <c>Page*Response</c> clients deserialize unchanged.
/// </summary>
public sealed record PagedResponse<T>(
    int TotalPages,
    long TotalElements,
    bool First,
    bool Last,
    int Size,
    IReadOnlyList<T> Content,
    int Number,
    int NumberOfElements,
    PagedResponse<T>.SortInfo Sort,
    PagedResponse<T>.PageableInfo Pageable,
    bool Empty)
{
    public sealed record SortInfo(bool Empty, bool Sorted, bool Unsorted);

    public sealed record PageableInfo(
        long Offset,
        SortInfo Sort,
        bool Paged,
        int PageNumber,
        int PageSize,
        bool Unpaged);

    public static PagedResponse<T> Of(IReadOnlyList<T> content, long totalElements, int pageNumber, int pageSize, bool sorted)
    {
        var totalPages = pageSize == 0 ? 0 : (int)Math.Ceiling(totalElements / (double)pageSize);
        var sort = new SortInfo(Empty: !sorted, Sorted: sorted, Unsorted: !sorted);
        var pageable = new PageableInfo(
            Offset: (long)pageNumber * pageSize,
            Sort: sort,
            Paged: true,
            PageNumber: pageNumber,
            PageSize: pageSize,
            Unpaged: false);

        return new PagedResponse<T>(
            TotalPages: totalPages,
            TotalElements: totalElements,
            First: pageNumber == 0,
            Last: pageNumber >= totalPages - 1,
            Size: pageSize,
            Content: content,
            Number: pageNumber,
            NumberOfElements: content.Count,
            Sort: sort,
            Pageable: pageable,
            Empty: content.Count == 0);
    }
}
