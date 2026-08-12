namespace CertWatch;

public interface IEmailClient
{
    Task SendAsync(string toAddress, string subject, string body, CancellationToken ct = default);
}
