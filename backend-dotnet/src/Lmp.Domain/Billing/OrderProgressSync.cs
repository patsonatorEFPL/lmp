namespace Lmp.Domain.Billing;

/// <summary>
/// Aligns <see cref="Order.ProgressPercentage"/>/<see cref="Order.ProgressStatus"/>
/// with <see cref="Order.Status"/> using the same thresholds as the frontend.
/// Faithful port of <c>com.lmp.billing.domain.OrderProgressSync</c>.
/// </summary>
public static class OrderProgressSync
{
    public static void ApplyMinimumForStatus(Order order)
    {
        switch (order.Status)
        {
            case OrderStatus.Cancelled:
                order.ProgressPercentage = 0;
                order.ProgressStatus = "Annulée";
                break;
            case OrderStatus.Refunded:
                order.ProgressStatus = "Remboursée";
                break;
            case OrderStatus.PaymentPending:
            case OrderStatus.Pending:
                order.ProgressPercentage = 0;
                order.ProgressStatus = "Commande reçue";
                break;
            case OrderStatus.Confirmed: ApplyFloored(order, 10, "Paiement confirmé"); break;
            case OrderStatus.UnderReview: ApplyFloored(order, 20, "En révision"); break;
            case OrderStatus.Processing: ApplyFloored(order, 30, "En traitement"); break;
            case OrderStatus.InProgress: ApplyFloored(order, 50, "En cours"); break;
            case OrderStatus.Shipped: ApplyFloored(order, 80, "Livraison"); break;
            case OrderStatus.Delivered: ApplyFloored(order, 90, "Livrée"); break;
            case OrderStatus.Completed: ApplyFloored(order, 100, "Terminée"); break;
            default: break;
        }
    }

    private static void ApplyFloored(Order order, int floor, string label)
    {
        var current = order.ProgressPercentage ?? 0;
        if (current < floor)
        {
            order.ProgressPercentage = floor;
            order.ProgressStatus = label;
        }
        else if (string.IsNullOrWhiteSpace(order.ProgressStatus))
        {
            order.ProgressStatus = label;
        }
    }
}
