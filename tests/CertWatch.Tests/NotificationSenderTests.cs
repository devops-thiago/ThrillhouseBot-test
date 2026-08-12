using CertWatch;
using CertWatch.Models;

namespace CertWatch.Tests;

public class NotificationSenderTests
{
    private sealed class FakeOwnerLookupRepository : IOwnerLookupRepository
    {
        public Task<string?> GetOwnerEmailAsync(string ownerId, CancellationToken ct = default) =>
            Task.FromResult<string?>("owner@example.com");
    }

    private sealed class RecordingEmailClient : IEmailClient
    {
        public List<string> SentTo { get; } = new();

        public Task SendAsync(string toAddress, string subject, string body, CancellationToken ct = default)
        {
            SentTo.Add(toAddress);
            return Task.CompletedTask;
        }
    }

    private static CertificateRecord MakeCertificate(string ownerId) => new()
    {
        Serial = "AB:CD:EF:01",
        CommonName = "payments.internal.example.com",
        OwnerId = ownerId,
        ExpiresAt = DateTimeOffset.UtcNow.AddDays(5),
        Sha256Thumbprint = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"
    };

    [Fact]
    public async Task SendExpiryNotificationsAsync_KnownOwner_SendsToOwnerEmail()
    {
        var emailClient = new RecordingEmailClient();
        var sender = new NotificationSender(new FakeOwnerLookupRepository(), emailClient);
        var certificates = new List<CertificateRecord> { MakeCertificate("owner-42") };

        await sender.SendExpiryNotificationsAsync(certificates);

        Assert.Equal(["owner@example.com"], emailClient.SentTo);
    }

    [Fact]
    public async Task SendExpiryNotificationsAsync_OwnerNotInDirectory_SendsGracefully()
    {
        // owner-does-not-exist has no row in the owners table, exercising the
        // "no matching owner" branch of the lookup.
        var emailClient = new RecordingEmailClient();
        var sender = new NotificationSender(new FakeOwnerLookupRepository(), emailClient);
        var certificates = new List<CertificateRecord> { MakeCertificate("owner-does-not-exist") };

        await sender.SendExpiryNotificationsAsync(certificates);

        Assert.Single(emailClient.SentTo);
    }
}
