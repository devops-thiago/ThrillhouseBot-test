namespace LiftPassValidator;

public enum PassKind { Day, Multi, Season }

public enum ScanOutcome
{
    Granted,
    Expired,
    NotYetValid,
    WrongZone,
    PassbackBlocked,
    LiftClosed,
    UnknownPass,
}

public sealed record LiftPass(
    string Id,
    string Holder,
    PassKind Kind,
    DateTime ValidFrom,
    DateTime ValidUntil,
    string[] Zones);

public sealed record ScanRequest(string PassId, string LiftId);

public sealed record ScanResult(ScanOutcome Outcome, string? PassId);

public sealed record LiftStatus(string LiftId, bool Open, string Zone);
