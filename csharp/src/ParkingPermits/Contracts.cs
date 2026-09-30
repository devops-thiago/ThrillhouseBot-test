namespace ParkingPermits;

public interface IFeeGateway
{
    /// <summary>
    /// Charges the applicant the permit fee. A declined card is reported by throwing
    /// <see cref="PaymentDeclinedException"/>; a returned result always has Success == true.
    /// </summary>
    ChargeResult Charge(string applicantEmail, decimal amount);
}

public interface IRegistryClient
{
    /// <summary>Returns one page of vehicles registered at an address (100 per page).</summary>
    Task<VehiclePage> GetVehiclesAsync(string address, string? cursor);
}

public interface IPermitStore
{
    int CountForAddress(string address);
    void Save(Permit permit);
    void Expire(string number);
    IReadOnlyList<Permit> All();
}
