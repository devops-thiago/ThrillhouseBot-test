namespace ParkingPermits;

public class CardFeeGateway : IFeeGateway
{
    public ChargeResult Charge(string applicantEmail, decimal amount)
    {
        if (amount <= 0) throw new ArgumentOutOfRangeException(nameof(amount));

        if (applicantEmail.EndsWith("@declined.test", StringComparison.OrdinalIgnoreCase))
            throw new PaymentDeclinedException("Card declined");

        return new ChargeResult(true, "ch_" + Guid.NewGuid().ToString("N")[..12]);
    }
}
