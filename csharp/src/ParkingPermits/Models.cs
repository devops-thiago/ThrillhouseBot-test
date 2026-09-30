namespace ParkingPermits;

public enum PermitStatus { Active, Expired, Revoked }

public record PermitApplication(string Plate, string Address, string Zone, string ApplicantEmail);

public record Permit(
    string Number,
    string Plate,
    string Address,
    string Zone,
    DateTime IssuedAt,
    DateTime ExpiresAt,
    PermitStatus Status);

public record Vehicle(string Plate, string Address);

public record VehiclePage(IReadOnlyList<Vehicle> Items, string? NextCursor);

public record ChargeResult(bool Success, string? Reference);

public record IssueOutcome(bool Issued, string Message, Permit? Permit);

public record BatchResult(IReadOnlyList<Permit> Issued, IReadOnlyList<string> Errors);

public class PaymentDeclinedException : Exception
{
    public PaymentDeclinedException(string message) : base(message) { }
}
