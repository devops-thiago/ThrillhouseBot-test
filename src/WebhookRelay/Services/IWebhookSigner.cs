namespace WebhookRelay.Services;

/// <summary>
/// Computes an HMAC-SHA256 signature over the outgoing payload so subscribers can verify
/// the delivery originated from this service. Throws <see cref="ArgumentException"/> if the
/// subscription secret is null or empty, since an unsigned webhook must never be sent.
/// </summary>
public interface IWebhookSigner
{
    string Sign(string payload, string secret);
}
