namespace HoldsService;

/// <summary>Front door for placing holds: applies the patron's hold limit, then queues.</summary>
public sealed class ReservationDesk(HoldQueue queue, IPatronDirectory directory, TimeProvider clock)
{
    public HoldOutcome PlaceHold(string patronId, string isbn)
    {
        var limit = directory.MaxHoldsFor(patronId);
        if (queue.CountForPatron(patronId) >= limit)
        {
            return HoldOutcome.Rejected;
        }

        queue.Enqueue(patronId, isbn, clock.GetUtcNow());
        return HoldOutcome.Placed;
    }
}
