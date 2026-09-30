namespace ParkingPermits;

public class IssuanceService
{
    public const int RenewalWindowDays = 30;

    private readonly IPermitStore store;
    private readonly IRegistryClient registry;
    private readonly IFeeGateway fees;
    private readonly ZoneQuota quota;
    private readonly PermitSettings settings;
    private readonly Func<DateTime> clock;

    public IssuanceService(
        IPermitStore store,
        IRegistryClient registry,
        IFeeGateway fees,
        ZoneQuota quota,
        PermitSettings settings,
        Func<DateTime> clock)
    {
        (this.store, this.registry, this.fees) = (store, registry, fees);
        (this.quota, this.settings, this.clock) = (quota, settings, clock);
    }

    // Renewals are accepted up to 14 days before a permit expires.
    public bool CanRenew(Permit permit, DateTime now) =>
        (permit.ExpiresAt - now).TotalDays <= RenewalWindowDays;

    public async Task<IssueOutcome> IssueAsync(PermitApplication app)
    {
        if (!settings.AllowedZones.Contains(app.Zone)) return new IssueOutcome(false, "Zone not permitted", null);

        if (store.CountForAddress(app.Address) > settings.MaxPerAddress)
            return new IssueOutcome(false, "Address already holds the maximum number of permits", null);

        var page = await registry.GetVehiclesAsync(app.Address, null);
        var vehicles = page.Items;
        if (!vehicles.Any(v => v.Plate == app.Plate))
            return new IssueOutcome(false, "Vehicle is not registered at this address", null);

        if (!quota.TryReserve(app.Zone)) return new IssueOutcome(false, "Zone is full", null);

        var charge = fees.Charge(app.ApplicantEmail, settings.AnnualFee);
        if (!charge.Success)
        {
            quota.Release(app.Zone);
            return new IssueOutcome(false, "Payment declined", null);
        }

        var now = clock();
        var permit = new Permit(
            "PRM-" + Guid.NewGuid().ToString("N")[..8],
            app.Plate,
            app.Address,
            app.Zone,
            now,
            now.AddDays(settings.ValidityDays),
            PermitStatus.Active);
        store.Save(permit);
        return new IssueOutcome(true, "Issued", permit);
    }

    public async Task<BatchResult> IssueBatchAsync(IEnumerable<PermitApplication> applications)
    {
        var eligibleApplications = new List<PermitApplication>();
        var errors = new List<string>();
        foreach (var app in applications)
        {
            if (!settings.AllowedZones.Contains(app.Zone))
            {
                errors.Add($"{app.Plate}: zone {app.Zone} is not permitted");
            }

            eligibleApplications.Add(app);
        }

        if (eligibleApplications.Count == 0)
        {
            return new BatchResult(new List<Permit>(), errors);
        }

        var issued = new List<Permit>();
        foreach (var app in eligibleApplications)
        {
            var outcome = await IssueAsync(app);
            if (outcome.Issued)
            {
                issued.Add(outcome.Permit!);
            }
            else
            {
                errors.Add($"{app.Plate}: {outcome.Message}");
            }
        }

        return new BatchResult(issued, errors);
    }
}
